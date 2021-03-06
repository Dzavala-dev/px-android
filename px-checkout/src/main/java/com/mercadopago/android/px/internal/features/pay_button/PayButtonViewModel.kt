package com.mercadopago.android.px.internal.features.pay_button

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import com.mercadopago.android.px.addons.model.SecurityValidationData
import com.mercadopago.android.px.internal.base.BaseViewModel
import com.mercadopago.android.px.internal.callbacks.PaymentServiceEventHandler
import com.mercadopago.android.px.internal.core.ConnectionHelper
import com.mercadopago.android.px.internal.core.ProductIdProvider
import com.mercadopago.android.px.internal.extensions.isNotNullNorEmpty
import com.mercadopago.android.px.internal.features.checkout.PostPaymentDriver
import com.mercadopago.android.px.internal.features.checkout.PostPaymentUrlsMapper
import com.mercadopago.android.px.internal.features.explode.ExplodeDecoratorMapper
import com.mercadopago.android.px.internal.features.pay_button.PayButton.OnReadyForPaymentCallback
import com.mercadopago.android.px.internal.features.pay_button.UIProgress.ButtonLoadingCanceled
import com.mercadopago.android.px.internal.features.pay_button.UIProgress.ButtonLoadingFinished
import com.mercadopago.android.px.internal.features.pay_button.UIProgress.ButtonLoadingStarted
import com.mercadopago.android.px.internal.features.pay_button.UIProgress.FingerprintRequired
import com.mercadopago.android.px.internal.features.pay_button.UIResult.VisualProcessorResult
import com.mercadopago.android.px.internal.features.payment_congrats.model.PaymentCongratsModelMapper
import com.mercadopago.android.px.internal.features.security_code.model.SecurityCodeParams
import com.mercadopago.android.px.internal.livedata.MediatorSingleLiveData
import com.mercadopago.android.px.internal.model.SecurityType
import com.mercadopago.android.px.internal.repository.CustomTextsRepository
import com.mercadopago.android.px.internal.repository.PaymentRepository
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository
import com.mercadopago.android.px.internal.util.SecurityValidationDataFactory
import com.mercadopago.android.px.internal.viewmodel.BusinessPaymentModel
import com.mercadopago.android.px.internal.viewmodel.PaymentModel
import com.mercadopago.android.px.internal.viewmodel.PostPaymentAction
import com.mercadopago.android.px.internal.viewmodel.mappers.PayButtonViewModelMapper
import com.mercadopago.android.px.model.Card
import com.mercadopago.android.px.model.Currency
import com.mercadopago.android.px.model.Payment
import com.mercadopago.android.px.model.PaymentRecovery
import com.mercadopago.android.px.model.PaymentResult
import com.mercadopago.android.px.model.exceptions.MercadoPagoError
import com.mercadopago.android.px.model.internal.PaymentConfiguration
import com.mercadopago.android.px.tracking.internal.events.BiometricsFrictionTracker
import com.mercadopago.android.px.tracking.internal.events.FrictionEventTracker
import com.mercadopago.android.px.tracking.internal.events.NoConnectionFrictionTracker
import com.mercadopago.android.px.tracking.internal.model.Reason
import com.mercadopago.android.px.tracking.internal.views.OneTapViewTracker
import com.mercadopago.android.px.internal.viewmodel.PayButtonViewModel as ButtonConfig

private const val BUNDLE_PAYMENT_CONFIGURATION = "BUNDLE_PAYMENT_CONFIGURATION"
private const val BUNDLE_PAYMENT_MODEL = "BUNDLE_PAYMENT_MODEL"
private const val BUNDLE_OBSERVING_SERVICE = "BUNDLE_OBSERVING_SERVICE"
private const val RETRY_COUNTER = "retry_counter"

internal class PayButtonViewModel(
    private val paymentService: PaymentRepository,
    private val productIdProvider: ProductIdProvider,
    private val connectionHelper: ConnectionHelper,
    private val paymentSettingRepository: PaymentSettingRepository,
    customTextsRepository: CustomTextsRepository,
    payButtonViewModelMapper: PayButtonViewModelMapper,
    private val paymentCongratsMapper: PaymentCongratsModelMapper,
    private val postPaymentUrlsMapper: PostPaymentUrlsMapper) : BaseViewModel(), PayButton.ViewModel {

    val buttonTextLiveData = MutableLiveData<ButtonConfig>()
    private var buttonConfig: ButtonConfig = payButtonViewModelMapper.map(customTextsRepository.customTexts)
    private var retryCounter = 0

    init {
        buttonTextLiveData.value = buttonConfig
    }

    private var handler: PayButton.Handler? = null
    private var paymentConfiguration: PaymentConfiguration? = null
    private var paymentModel: PaymentModel? = null

    val cvvRequiredLiveData = MediatorSingleLiveData<SecurityCodeParams>()
    val stateUILiveData = MediatorSingleLiveData<PayButtonUiState>()
    private var observingService = false

    private fun <X : Any, I> transform(liveData: LiveData<X>, block: (content: X) -> I): LiveData<I?> {
        return map(liveData) {
            observingService = false
            block(it)
        }
    }

    override fun attach(handler: PayButton.Handler) {
        this.handler = handler
    }

    override fun detach() {
        handler = null
    }

    override fun preparePayment() {
        paymentConfiguration = null
        if (connectionHelper.checkConnection()) {
            handler?.prePayment(object : OnReadyForPaymentCallback {
                override fun call(paymentConfiguration: PaymentConfiguration) {
                    if (paymentConfiguration.customOptionId.isNotNullNorEmpty()) {
                        paymentSettingRepository.clearToken()
                    }
                    startSecuredPayment(paymentConfiguration)
                }
            })
        } else {
            manageNoConnection()
        }
    }

    private fun startSecuredPayment(paymentConfiguration: PaymentConfiguration) {
        this.paymentConfiguration = paymentConfiguration
        val data: SecurityValidationData = SecurityValidationDataFactory
            .create(productIdProvider, paymentSettingRepository.checkoutPreference!!.totalAmount, paymentConfiguration)
        stateUILiveData.value = FingerprintRequired(data)
    }

    override fun handleBiometricsResult(isSuccess: Boolean, securityRequested: Boolean) {
        if (isSuccess) {
            paymentSettingRepository.configure(if (securityRequested) SecurityType.SECOND_FACTOR else SecurityType.NONE)
            startPayment()
        } else {
            BiometricsFrictionTracker.track()
        }
    }

    override fun startPayment() {
        if (paymentService.isExplodingAnimationCompatible) {
            stateUILiveData.value = ButtonLoadingStarted(paymentService.paymentTimeout, buttonConfig)
        }
        handler?.enqueueOnExploding(object : PayButton.OnEnqueueResolvedCallback {
            override fun success() {
                paymentConfiguration?.let { configuration ->
                    paymentService.startExpressPayment(configuration)
                    paymentService.observableEvents?.let { observeService(it) }
                    handler?.onPaymentExecuted(configuration)
                }
            }

            override fun failure() {
                stateUILiveData.value = ButtonLoadingCanceled
            }
        })
    }

    private fun observeService(serviceLiveData: PaymentServiceEventHandler) {
        observingService = true
        // Error event
        val paymentErrorLiveData: LiveData<ButtonLoadingCanceled?> =
            transform(serviceLiveData.paymentErrorLiveData) { error ->
                val shouldHandleError = error.isPaymentProcessing
                if (shouldHandleError) onPaymentProcessingError() else noRecoverableError(error)
                handler?.onPaymentError(error)
                ButtonLoadingCanceled
            }
        stateUILiveData.addSource(paymentErrorLiveData) { stateUILiveData.value = it }

        // Visual payment event
        val visualPaymentLiveData: LiveData<VisualProcessorResult?> =
            transform(serviceLiveData.visualPaymentLiveData) { VisualProcessorResult }
        stateUILiveData.addSource(visualPaymentLiveData) { stateUILiveData.value = it }

        // Payment finished event
        val paymentFinishedLiveData: LiveData<ButtonLoadingFinished?> =
            transform(serviceLiveData.paymentFinishedLiveData) { paymentModel ->
                this.paymentModel = paymentModel
                ButtonLoadingFinished(ExplodeDecoratorMapper().map(paymentModel))
            }
        stateUILiveData.addSource(paymentFinishedLiveData) { stateUILiveData.value = it }

        // Cvv required event
        val cvvRequiredLiveData: LiveData<Pair<Card, Reason>?> = transform(serviceLiveData.requireCvvLiveData) { it }
        this.cvvRequiredLiveData.addSource(cvvRequiredLiveData) { value ->
            value?.let { pair ->
                handler?.onCvvRequested()?.let {
                    this.cvvRequiredLiveData.value = SecurityCodeParams(paymentConfiguration!!,
                        it.fragmentContainer, it.renderMode, card = pair.first, reason = pair.second)
                }
            }
            stateUILiveData.value = ButtonLoadingCanceled
        }

        // Invalid esc event
        val recoverRequiredLiveData: LiveData<PaymentRecovery?> =
            transform(serviceLiveData.recoverInvalidEscLiveData) { it.takeIf { it.shouldAskForCvv() } }
        this.cvvRequiredLiveData.addSource(recoverRequiredLiveData) { paymentRecovery ->
            paymentRecovery?.let { recoverPayment(it) }
            stateUILiveData.value = ButtonLoadingCanceled
        }
    }

    private fun onPaymentProcessingError() {
        val currency: Currency = paymentSettingRepository.currency
        val paymentResult: PaymentResult = PaymentResult.Builder()
            .setPaymentData(paymentService.paymentDataList)
            .setPaymentStatus(Payment.StatusCodes.STATUS_IN_PROCESS)
            .setPaymentStatusDetail(Payment.StatusDetail.STATUS_DETAIL_PENDING_CONTINGENCY)
            .build()
        onPostPayment(PaymentModel(paymentResult, currency))
    }

    override fun onPostPayment(paymentModel: PaymentModel) {
        this.paymentModel = paymentModel
        stateUILiveData.value = ButtonLoadingFinished(ExplodeDecoratorMapper().map(paymentModel))
    }

    override fun onPostPaymentAction(postPaymentAction: PostPaymentAction) {
        postPaymentAction.execute(object : PostPaymentAction.ActionController {
            override fun recoverPayment(postPaymentAction: PostPaymentAction) {
                stateUILiveData.value = ButtonLoadingCanceled
                recoverPayment()
            }

            override fun onChangePaymentMethod() {
                stateUILiveData.value = ButtonLoadingCanceled
            }
        })
        handler?.onPostPaymentAction(postPaymentAction)
    }

    override fun handleCongratsResult(resultCode: Int, data: Intent?) {
        handler?.onPostCongrats(resultCode, data)
    }

    override fun handleSecurityCodeResult(resultCode: Int, data: Intent?) {
        handler?.onPostCongrats(resultCode, data)
    }

    override fun onRecoverPaymentEscInvalid(recovery: PaymentRecovery) = recoverPayment(recovery)

    override fun recoverPayment() = recoverPayment(paymentService.createPaymentRecovery())

    private fun recoverPayment(recovery: PaymentRecovery) {
        handler?.onCvvRequested()?.let {
            cvvRequiredLiveData.value = SecurityCodeParams(paymentConfiguration!!, it.fragmentContainer, it.renderMode,
                paymentRecovery = recovery)
        }
    }

    private fun manageNoConnection() {
        trackNoConnectionFriction()
        stateUILiveData.value = UIError.ConnectionError(++retryCounter)
    }

    private fun trackNoRecoverableFriction(error: MercadoPagoError) {
        FrictionEventTracker.with(OneTapViewTracker.PATH_REVIEW_ONE_TAP_VIEW,
            FrictionEventTracker.Id.GENERIC, FrictionEventTracker.Style.CUSTOM_COMPONENT, error).track()
    }

    private fun trackNoConnectionFriction() {
        NoConnectionFrictionTracker.track()
    }

    private fun noRecoverableError(error: MercadoPagoError) {
        trackNoRecoverableFriction(error)
        stateUILiveData.value = UIError.BusinessError
    }

    override fun hasFinishPaymentAnimation() {
        paymentModel?.let { paymentModel ->
            handler?.onPaymentFinished(paymentModel, object : PayButton.OnPaymentFinishedCallback {
                override fun call() {
                    resolvePostPaymentUrls(paymentModel)?.let {
                        PostPaymentDriver.Builder(paymentModel, it).action(
                            object : PostPaymentDriver.Action {
                                override fun showCongrats(model: PaymentModel) {
                                    stateUILiveData.value = UIResult.PaymentResult(model)
                                }

                                override fun showCongrats(model: BusinessPaymentModel) {
                                    stateUILiveData.value = UIResult.CongratsPaymentModel(paymentCongratsMapper.map(model))
                                }

                                override fun skipCongrats(model: PaymentModel) {
                                    stateUILiveData.value = UIResult.NoCongratsResult(model)
                                }
                            }
                        ).build().execute()
                    }
                }
            })
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        bundle.putParcelable(BUNDLE_PAYMENT_CONFIGURATION, paymentConfiguration)
        bundle.putParcelable(BUNDLE_PAYMENT_MODEL, paymentModel)
        bundle.putBoolean(BUNDLE_OBSERVING_SERVICE, observingService)
        bundle.putInt(RETRY_COUNTER, retryCounter)
    }

    override fun recoverFromBundle(bundle: Bundle) {
        paymentConfiguration = bundle.getParcelable(BUNDLE_PAYMENT_CONFIGURATION)
        paymentModel = bundle.getParcelable(BUNDLE_PAYMENT_MODEL)
        observingService = bundle.getBoolean(BUNDLE_OBSERVING_SERVICE)
        retryCounter = bundle.getInt(RETRY_COUNTER, 0)
        if (observingService) {
            paymentService.observableEvents?.let {
                observeService(it)
            } ?: onPaymentProcessingError()
        }
    }

    private fun resolvePostPaymentUrls(paymentModel: PaymentModel) : PostPaymentUrlsMapper.Response? {
        return paymentSettingRepository.checkoutPreference?.let { preference ->
            val congratsResponse = paymentModel.congratsResponse
            postPaymentUrlsMapper.map(PostPaymentUrlsMapper.Model(
                congratsResponse.redirectUrl,
                congratsResponse.backUrl,
                paymentModel.payment,
                preference,
                paymentSettingRepository.site.id
            ))
        }
    }
}
