package com.mercadopago.android.px.internal.features.express;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.mercadolibre.android.cardform.internal.LifecycleListener;
import com.mercadopago.android.px.addons.ESCManagerBehaviour;
import com.mercadopago.android.px.addons.model.internal.Configuration;
import com.mercadopago.android.px.addons.model.internal.Experiment;
import com.mercadopago.android.px.configuration.DynamicDialogConfiguration;
import com.mercadopago.android.px.core.DynamicDialogCreator;
import com.mercadopago.android.px.core.internal.TriggerableQueue;
import com.mercadopago.android.px.internal.base.BasePresenter;
import com.mercadopago.android.px.internal.experiments.KnownExperiment;
import com.mercadopago.android.px.internal.experiments.KnownVariant;
import com.mercadopago.android.px.internal.experiments.ScrolledVariant;
import com.mercadopago.android.px.internal.experiments.Variant;
import com.mercadopago.android.px.internal.experiments.VariantHandler;
import com.mercadopago.android.px.internal.features.express.installments.InstallmentRowHolder;
import com.mercadopago.android.px.internal.features.express.offline_methods.OfflineMethods;
import com.mercadopago.android.px.internal.features.express.slider.HubAdapter;
import com.mercadopago.android.px.internal.features.express.slider.SplitPaymentHeaderAdapter;
import com.mercadopago.android.px.internal.features.generic_modal.ActionType;
import com.mercadopago.android.px.internal.features.generic_modal.ActionTypeWrapper;
import com.mercadopago.android.px.internal.features.generic_modal.FromModalToGenericDialogItem;
import com.mercadopago.android.px.internal.features.pay_button.PayButton;
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository;
import com.mercadopago.android.px.internal.repository.AmountRepository;
import com.mercadopago.android.px.internal.repository.ChargeRepository;
import com.mercadopago.android.px.internal.repository.CustomTextsRepository;
import com.mercadopago.android.px.internal.repository.DisabledPaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.DiscountRepository;
import com.mercadopago.android.px.internal.repository.ExperimentsRepository;
import com.mercadopago.android.px.internal.repository.InitRepository;
import com.mercadopago.android.px.internal.repository.PayerComplianceRepository;
import com.mercadopago.android.px.internal.repository.PayerCostSelectionRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.tracking.TrackingRepository;
import com.mercadopago.android.px.internal.util.ApiUtil;
import com.mercadopago.android.px.internal.util.CardFormWrapper;
import com.mercadopago.android.px.internal.util.PayerComplianceWrapper;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.internal.view.AmountDescriptorView;
import com.mercadopago.android.px.internal.view.ElementDescriptorView;
import com.mercadopago.android.px.internal.view.PaymentMethodDescriptorView;
import com.mercadopago.android.px.internal.view.SummaryView;
import com.mercadopago.android.px.internal.view.experiments.ExperimentHelper;
import com.mercadopago.android.px.internal.viewmodel.ConfirmButtonViewModel;
import com.mercadopago.android.px.internal.viewmodel.PostPaymentAction;
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState;
import com.mercadopago.android.px.internal.viewmodel.drawables.PaymentMethodDrawableItemMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.AmountDescriptorMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.ConfirmButtonViewModelMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.ElementDescriptorMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.InstallmentViewModelMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.PaymentMethodDescriptorMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.SplitHeaderMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.SummaryInfoMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.SummaryViewModelMapper;
import com.mercadopago.android.px.model.AmountConfiguration;
import com.mercadopago.android.px.model.DiscountConfigurationModel;
import com.mercadopago.android.px.model.ExpressMetadata;
import com.mercadopago.android.px.model.PayerCost;
import com.mercadopago.android.px.model.PaymentData;
import com.mercadopago.android.px.model.exceptions.ApiException;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;
import com.mercadopago.android.px.model.internal.FromExpressMetadataToPaymentConfiguration;
import com.mercadopago.android.px.model.internal.InitResponse;
import com.mercadopago.android.px.model.internal.Modal;
import com.mercadopago.android.px.model.internal.PaymentConfiguration;
import com.mercadopago.android.px.model.internal.SummaryInfo;
import com.mercadopago.android.px.model.one_tap.CheckoutBehaviour;
import com.mercadopago.android.px.preferences.CheckoutPreference;
import com.mercadopago.android.px.services.Callback;
import com.mercadopago.android.px.tracking.internal.events.ConfirmEvent;
import com.mercadopago.android.px.tracking.internal.events.InstallmentsEventTrack;
import com.mercadopago.android.px.tracking.internal.events.SuspendedFrictionTracker;
import com.mercadopago.android.px.tracking.internal.events.SwipeOneTapEventTracker;
import com.mercadopago.android.px.tracking.internal.events.TargetBehaviourEvent;
import com.mercadopago.android.px.tracking.internal.mapper.FromSelectedExpressMetadataToAvailableMethods;
import com.mercadopago.android.px.tracking.internal.model.ConfirmData;
import com.mercadopago.android.px.tracking.internal.model.TargetBehaviourTrackData;
import com.mercadopago.android.px.tracking.internal.views.OneTapViewTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* default */ class ExpressPaymentPresenter extends BasePresenter<ExpressPayment.View>
    implements ExpressPayment.Actions, AmountDescriptorView.OnClickListener {

    private static final String BUNDLE_STATE_SPLIT_PREF = "state_split_pref";
    private static final String BUNDLE_STATE_CURRENT_PM_INDEX = "state_current_pm_index";

    @NonNull private final AmountRepository amountRepository;
    @NonNull private final DiscountRepository discountRepository;
    @NonNull private final PaymentSettingRepository paymentSettingRepository;
    @NonNull private final AmountConfigurationRepository amountConfigurationRepository;
    @NonNull private final DisabledPaymentMethodRepository disabledPaymentMethodRepository;
    @NonNull private final ChargeRepository chargeRepository;
    @NonNull private final ESCManagerBehaviour escManagerBehaviour;
    @NonNull private final ExperimentsRepository experimentsRepository;
    @NonNull private final PayerComplianceRepository payerComplianceRepository;
    @NonNull private final TrackingRepository trackingRepository;
    @NonNull private final PaymentMethodDescriptorMapper paymentMethodDescriptorMapper;
    @NonNull private final CustomTextsRepository customTextsRepository;
    @NonNull private final AmountDescriptorMapper amountDescriptorMapper;
    @NonNull /* default */ final InitRepository initRepository;
    private final PayerCostSelectionRepository payerCostSelectionRepository;
    private final PaymentMethodDrawableItemMapper paymentMethodDrawableItemMapper;
    private SplitSelectionState splitSelectionState;
    private Set<String> cardsWithSplit;
    /* default */ List<ExpressMetadata> expressMetadataList; //FIXME remove.
    /* default */ Map<String, Modal> modals; //FIXME remove.
    /* default */ PayerComplianceWrapper payerCompliance; //FIXME remove.
    /* default */ int paymentMethodIndex;
    /* default */ ActionTypeWrapper actionTypeWrapper;
    /* default */ TriggerableQueue triggerableQueue;

    /* default */ ExpressPaymentPresenter(@NonNull final PaymentSettingRepository paymentSettingRepository,
        @NonNull final DisabledPaymentMethodRepository disabledPaymentMethodRepository,
        @NonNull final PayerCostSelectionRepository payerCostSelectionRepository,
        @NonNull final DiscountRepository discountRepository,
        @NonNull final AmountRepository amountRepository,
        @NonNull final InitRepository initRepository,
        @NonNull final AmountConfigurationRepository amountConfigurationRepository,
        @NonNull final ChargeRepository chargeRepository,
        @NonNull final ESCManagerBehaviour escManagerBehaviour,
        @NonNull final PaymentMethodDrawableItemMapper paymentMethodDrawableItemMapper,
        @NonNull final ExperimentsRepository experimentsRepository,
        @NonNull final PayerComplianceRepository payerComplianceRepository,
        @NonNull final TrackingRepository trackingRepository,
        @NonNull final PaymentMethodDescriptorMapper paymentMethodDescriptorMapper,
        @NonNull final CustomTextsRepository customTextsRepository,
        @NonNull final AmountDescriptorMapper amountDescriptorMapper) {

        this.paymentSettingRepository = paymentSettingRepository;
        this.disabledPaymentMethodRepository = disabledPaymentMethodRepository;
        this.payerCostSelectionRepository = payerCostSelectionRepository;
        this.discountRepository = discountRepository;
        this.amountRepository = amountRepository;
        this.initRepository = initRepository;
        this.amountConfigurationRepository = amountConfigurationRepository;
        this.chargeRepository = chargeRepository;
        this.escManagerBehaviour = escManagerBehaviour;
        this.paymentMethodDrawableItemMapper = paymentMethodDrawableItemMapper;
        this.experimentsRepository = experimentsRepository;
        this.payerComplianceRepository = payerComplianceRepository;
        this.trackingRepository = trackingRepository;
        this.paymentMethodDescriptorMapper = paymentMethodDescriptorMapper;
        this.customTextsRepository = customTextsRepository;
        this.amountDescriptorMapper = amountDescriptorMapper;

        splitSelectionState = new SplitSelectionState();
        triggerableQueue = new TriggerableQueue();
    }

    /* default */ void onFailToRetrieveInitResponse(@NonNull final ApiException apiException) {
        getView().showError(MercadoPagoError.createNotRecoverable(apiException, ApiUtil.RequestOrigin.POST_INIT));
    }

    @Override
    public void loadViewModel() {
        final SummaryInfo summaryInfo = new SummaryInfoMapper().map(paymentSettingRepository.getCheckoutPreference());

        final ElementDescriptorView.Model elementDescriptorModel =
            new ElementDescriptorMapper().map(summaryInfo);

        final List<SummaryView.Model> summaryModels =
            new SummaryViewModelMapper(paymentSettingRepository.getCurrency(), discountRepository, amountRepository,
                elementDescriptorModel, this, summaryInfo, chargeRepository, amountConfigurationRepository,
                customTextsRepository, amountDescriptorMapper).map(new ArrayList<>(expressMetadataList));

        final List<PaymentMethodDescriptorView.Model> paymentModels =
            paymentMethodDescriptorMapper.map(expressMetadataList);

        final List<SplitPaymentHeaderAdapter.Model> splitHeaderModels =
            new SplitHeaderMapper(paymentSettingRepository.getCurrency(), amountConfigurationRepository)
                .map(expressMetadataList);

        final List<ConfirmButtonViewModel> confirmButtonViewModels =
            new ConfirmButtonViewModelMapper(disabledPaymentMethodRepository).map(expressMetadataList);

        final HubAdapter.Model model =
            new HubAdapter.Model(paymentModels, summaryModels, splitHeaderModels, confirmButtonViewModels);
        getView().configurePayButton(this::overridePayButtonStateChange);
        getView().configurePaymentMethodHeader(getVariants());
        getView().showToolbarElementDescriptor(elementDescriptorModel);
        getView().configureRenderMode(getVariants());
        getView().configureAdapters(paymentSettingRepository.getSite(), paymentSettingRepository.getCurrency());
        getView().updateAdapters(model);
        updateElements();
        getView().updatePaymentMethods(paymentMethodDrawableItemMapper.map(expressMetadataList));
        if (OfflineMethods.shouldLaunch(expressMetadataList)) {
            getView().showOfflineMethodsCollapsed();
        }
    }

    @Override
    public void attachView(final ExpressPayment.View view) {
        super.attachView(view);
        initPresenter();
    }

    private void initPresenter() {
        initRepository.init().enqueue(new Callback<InitResponse>() {
            @Override
            public void success(final InitResponse initResponse) {
                if (isViewAttached()) {
                    expressMetadataList = initResponse.getExpress();
                    actionTypeWrapper = new ActionTypeWrapper(expressMetadataList);
                    modals = initResponse.getModals();
                    payerCompliance = new PayerComplianceWrapper(initResponse.getPayerCompliance());
                    cardsWithSplit = initResponse.getIdsWithSplitAllowed();
                    triggerableQueue.execute();
                    loadViewModel();
                }
            }

            @Override
            public void failure(final ApiException apiException) {
                onFailToRetrieveInitResponse(apiException);
            }
        });
    }

    @Override
    public void recoverFromBundle(@NonNull final Bundle bundle) {
        splitSelectionState = bundle.getParcelable(BUNDLE_STATE_SPLIT_PREF);
        paymentMethodIndex = bundle.getInt(BUNDLE_STATE_CURRENT_PM_INDEX);
    }

    @NonNull
    @Override
    public Bundle storeInBundle(@NonNull final Bundle bundle) {
        bundle.putParcelable(BUNDLE_STATE_SPLIT_PREF, splitSelectionState);
        bundle.putInt(BUNDLE_STATE_CURRENT_PM_INDEX, paymentMethodIndex);
        return bundle;
    }

    @Override
    public void onFreshStart() {
        triggerableQueue.enqueue(() -> {
            trackOneTapView();
            return null;
        });
    }

    private void trackOneTapView() {
        final OneTapViewTracker oneTapViewTracker =
            new OneTapViewTracker(expressMetadataList, paymentSettingRepository.getCheckoutPreference(),
                discountRepository.getCurrentConfiguration(), escManagerBehaviour.getESCCardIds(), cardsWithSplit,
                disabledPaymentMethodRepository.getDisabledPaymentMethods().size(),
                experimentsRepository.getExperiments(Configuration.TrackingMode.NO_CONDITIONAL));
        setCurrentViewTracker(oneTapViewTracker);
    }

    private ExpressMetadata getCurrentExpressMetadata() {
        return expressMetadataList.get(paymentMethodIndex);
    }

    @Override
    public void cancel() {
        tracker.trackBack();
        getView().cancel();
    }

    @Override
    public void onBack() {
        tracker.trackAbort();
    }

    private void updateElementPosition(final int selectedPayerCost) {
        payerCostSelectionRepository.save(getCurrentExpressMetadata().getCustomOptionId(), selectedPayerCost);
        updateElements();
    }

    @Override
    public void onInstallmentsRowPressed() {
        updateInstallments();
        getView().animateInstallmentsList();
        final ExpressMetadata expressMetadata = getCurrentExpressMetadata();
        final AmountConfiguration amountConfiguration =
            amountConfigurationRepository.getConfigurationFor(expressMetadata.getCustomOptionId());
        new InstallmentsEventTrack(expressMetadata, amountConfiguration).track();
    }

    @Override
    public void updateInstallments() {
        final ExpressMetadata expressMetadata = getCurrentExpressMetadata();
        final AmountConfiguration amountConfiguration =
            amountConfigurationRepository.getConfigurationFor(expressMetadata.getCustomOptionId());
        final List<PayerCost> payerCostList = getCurrentPayerCosts();
        final int selectedIndex = amountConfiguration.getCurrentPayerCostIndex(splitSelectionState.userWantsToSplit(),
            payerCostSelectionRepository.get(expressMetadata.getCustomOptionId()));
        final List<InstallmentRowHolder.Model> models =
            new InstallmentViewModelMapper(paymentSettingRepository.getCurrency(), expressMetadata.getBenefits(),
                getVariants()).map(payerCostList);
        getView().updateInstallmentsList(selectedIndex, models);
    }

    private List<PayerCost> getCurrentPayerCosts() {
        final ExpressMetadata expressMetadata = getCurrentExpressMetadata();
        final AmountConfiguration amountConfiguration =
            amountConfigurationRepository.getConfigurationFor(expressMetadata.getCustomOptionId());
        return amountConfiguration.getAppliedPayerCost(splitSelectionState.userWantsToSplit());
    }

    /**
     * When user cancel the payer cost selection this method will be called with the current payment method position
     */
    @Override
    public void onInstallmentSelectionCanceled() {
        updateElements();
        getView().collapseInstallmentsSelection();
    }

    /**
     * When user selects a new payment method this method will be called with the new current paymentMethodIndex.
     *
     * @param paymentMethodIndex current payment method paymentMethodIndex.
     */
    @Override
    public void onSliderOptionSelected(final int paymentMethodIndex) {
        this.paymentMethodIndex = paymentMethodIndex;
        new SwipeOneTapEventTracker().track();
        updateElementPosition(payerCostSelectionRepository.get(getCurrentExpressMetadata().getCustomOptionId()));
    }

    private void updateElements() {
        getView().updateViewForPosition(paymentMethodIndex,
            payerCostSelectionRepository.get(getCurrentExpressMetadata().getCustomOptionId()), splitSelectionState);
    }

    /**
     * When user selects a new payer cost for certain payment method this method will be called.
     *
     * @param payerCostSelected user selected payerCost.
     */
    @Override
    public void onPayerCostSelected(final PayerCost payerCostSelected) {
        final String customOptionId = getCurrentExpressMetadata().getCustomOptionId();
        final int selected = amountConfigurationRepository.getConfigurationFor(customOptionId)
            .getAppliedPayerCost(splitSelectionState.userWantsToSplit())
            .indexOf(payerCostSelected);
        updateElementPosition(selected);
        getVariant(KnownVariant.SCROLLED).process(new VariantHandler() {
            @Override
            public void visit(@NonNull final ScrolledVariant variant) {
                if (variant.isDefault()) {
                    getView().collapseInstallmentsSelection();
                }
            }
        });
    }

    public void onDisabledDescriptorViewClick() {
        getView().showDisabledPaymentMethodDetailDialog(
            disabledPaymentMethodRepository.getDisabledPaymentMethod(getCurrentExpressMetadata().getCustomOptionId()),
            getCurrentExpressMetadata().getStatus());
    }

    @Override
    public void onDiscountAmountDescriptorClicked(@NonNull final DiscountConfigurationModel discountModel) {
        getView().showDiscountDetailDialog(paymentSettingRepository.getCurrency(), discountModel);
    }

    @Override
    public void onChargesAmountDescriptorClicked(@NonNull final DynamicDialogCreator dynamicDialogCreator) {
        final DynamicDialogCreator.CheckoutData checkoutData = new DynamicDialogCreator.CheckoutData(
            paymentSettingRepository.getCheckoutPreference(), Collections.singletonList(new PaymentData()));
        getView().showDynamicDialog(dynamicDialogCreator, checkoutData);
    }

    @Override
    public void onSplitChanged(final boolean isChecked) {
        if (splitSelectionState.userWantsToSplit() != isChecked) {
            resetPayerCostSelection();
        }
        splitSelectionState.setUserWantsToSplit(isChecked);
        // cancel also update the position.
        // it is used because the installment selection can be expanded by the user.
        onInstallmentSelectionCanceled();
    }

    @Override
    public void onHeaderClicked() {
        final CheckoutPreference checkoutPreference = paymentSettingRepository.getCheckoutPreference();
        final DynamicDialogConfiguration dynamicDialogConfiguration =
            paymentSettingRepository.getAdvancedConfiguration().getDynamicDialogConfiguration();

        final DynamicDialogCreator.CheckoutData checkoutData =
            new DynamicDialogCreator.CheckoutData(checkoutPreference, Collections.singletonList(new PaymentData()));

        if (dynamicDialogConfiguration.hasCreatorFor(DynamicDialogConfiguration.DialogLocation.TAP_ONE_TAP_HEADER)) {
            getView().showDynamicDialog(
                dynamicDialogConfiguration.getCreatorFor(DynamicDialogConfiguration.DialogLocation.TAP_ONE_TAP_HEADER),
                checkoutData);
        }
    }

    /* default */ void resetPayerCostSelection() {
        payerCostSelectionRepository.reset();
    }

    @Override
    public void onPostPaymentAction(@NonNull final PostPaymentAction postPaymentAction) {
        postPaymentAction.execute(new PostPaymentAction.ActionController() {
            @Override
            public void recoverPayment(@NonNull final PostPaymentAction postPaymentAction) {
                //nothing to do here
            }

            @Override
            public void onChangePaymentMethod() {
                postDisableModelUpdate();
            }
        });
    }

    /* default */ void postDisableModelUpdate() {
        initRepository.refresh().enqueue(new Callback<InitResponse>() {
            @Override
            public void success(final InitResponse initResponse) {
                if (isViewAttached()) {
                    resetState(initResponse);
                }
            }

            @Override
            public void failure(final ApiException apiException) {
                onFailToRetrieveInitResponse(apiException);
            }
        });
    }

    @Override
    public void onOtherPaymentMethodClicked() {
        getView().showOfflineMethodsExpanded();
    }

    @Override
    @SuppressLint("WrongConstant")
    public void handlePrePaymentAction(@NonNull final PayButton.OnReadyForPaymentCallback callback) {
        if (!handleBehaviour(CheckoutBehaviour.Type.TAP_PAY)) {
            requireCurrentConfiguration(callback);
        }
    }

    private boolean handleBehaviour(@CheckoutBehaviour.Type @NonNull final String behaviourType) {
        final ExpressMetadata expressMetadata = getCurrentExpressMetadata();

        final CheckoutBehaviour behaviour = expressMetadata.getBehaviour(behaviourType);
        final Modal modal = behaviour != null && behaviour.getModal() != null ? modals.get(behaviour.getModal()) : null;
        final String target = behaviour != null ? behaviour.getTarget() : null;
        final boolean isMethodSuspended = expressMetadata.getStatus().isSuspended();

        if (modal != null) {
            getView().showGenericDialog(
                new FromModalToGenericDialogItem(actionTypeWrapper.getActionType(), behaviour.getModal()).map(modal));
            return true;
        } else if (TextUtil.isNotEmpty(target)) {
            new TargetBehaviourEvent(viewTracker, new TargetBehaviourTrackData(behaviourType, target)).track();
            getView().startDeepLink(target);
            return true;
        } else if (isMethodSuspended) {
            // is a friction if the method is suspended and does not have any behaviour to handle
            SuspendedFrictionTracker.INSTANCE.track();
            return true;
        } else {
            return false;
        }
    }

    private void requireCurrentConfiguration(@NonNull PayButton.OnReadyForPaymentCallback callback) {
        final ExpressMetadata expressMetadata = getCurrentExpressMetadata();

        final PaymentConfiguration configuration = new FromExpressMetadataToPaymentConfiguration(
            amountConfigurationRepository, splitSelectionState, payerCostSelectionRepository).map(expressMetadata);

        callback.call(configuration);
    }

    @Override
    public void handleGenericDialogAction(@NonNull final ActionType type) {
        switch (type) {
        case PAY_WITH_OTHER_METHOD:
        case PAY_WITH_OFFLINE_METHOD:
            getView().setPagerIndex(actionTypeWrapper.getIndexToReturn());
            break;
        case ADD_NEW_CARD:
            getView().setPagerIndex(actionTypeWrapper.getIndexToReturn());
            getView().startAddNewCardFlow(
                new CardFormWrapper(paymentSettingRepository, trackingRepository));
            break;
        default: // do nothing
        }
    }

    @Override
    public void onPaymentExecuted(@NonNull final PaymentConfiguration configuration) {
        final List<Experiment> experiments = new ArrayList<>();
        final ConfirmData confirmData = new ConfirmData(ConfirmData.ReviewType.ONE_TAP, paymentMethodIndex,
            new FromSelectedExpressMetadataToAvailableMethods(escManagerBehaviour.getESCCardIds(),
                configuration.getPayerCost(), configuration.getSplitPayment())
                .map(getCurrentExpressMetadata()));
        final Experiment experiment = experimentsRepository.getExperiment(KnownExperiment.INSTALLMENTS_HIGHLIGHT);
        if (getCurrentPayerCosts().size() > 1 && experiment != null) {
            experiments.add(experiment);
        }
        new ConfirmEvent(confirmData, experiments).track();
    }

    private boolean overridePayButtonStateChange(@NonNull final PayButton.State uiState) {
        final ExpressMetadata currentExpressMetadata = getCurrentExpressMetadata();
        return uiState == PayButton.State.ENABLE && (currentExpressMetadata.isNewCard() ||
            currentExpressMetadata.isOfflineMethods() || disabledPaymentMethodRepository.hasPaymentMethodId(
            currentExpressMetadata.getCustomOptionId()));
    }

    /* default */ void resetState(@NonNull final InitResponse initResponse) {
        expressMetadataList = initResponse.getExpress();
        actionTypeWrapper = new ActionTypeWrapper(expressMetadataList);
        modals = initResponse.getModals();
        payerCompliance = new PayerComplianceWrapper(initResponse.getPayerCompliance());
        resetPayerCostSelection();
        paymentMethodIndex = 0;
        getView().clearAdapters();
        loadViewModel();
    }

    @Override
    public void handleDeepLink() {
        disabledPaymentMethodRepository.reset();
        if (isViewAttached()) {
            getView().showLoading();
        }
        initRepository.cleanRefresh().enqueue(new Callback<InitResponse>() {
            @Override
            public void success(final InitResponse initResponse) {
                if (isViewAttached()) {
                    if (payerCompliance.turnedIFPECompliant(initResponse.getPayerCompliance())) {
                        payerComplianceRepository.turnIFPECompliant();
                    }
                    resetState(initResponse);
                    getView().hideLoading();
                }
            }

            @Override
            public void failure(final ApiException apiException) {
                if (isViewAttached()) {
                    getView().hideLoading();
                }
            }
        });
    }

    @Override
    public void onCardAdded(@NonNull final String cardId, @NonNull final LifecycleListener.Callback callback) {
        initRepository.refreshWithNewCard(cardId).enqueue(new Callback<InitResponse>() {
            @Override
            public void success(final InitResponse initResponse) {
                callback.onSuccess();
            }

            @Override
            public void failure(final ApiException apiException) {
                callback.onError();
            }
        });
    }

    @Override
    public void onCardFormResult() {
        postDisableModelUpdate();
    }

    private List<Variant> getVariants() {
        return ExperimentHelper.INSTANCE.getVariantsFrom(
            experimentsRepository.getExperiments(), KnownVariant.PULSE, KnownVariant.BADGE, KnownVariant.SCROLLED);
    }

    private Variant getVariant(@NonNull final KnownVariant knownVariant) {
        return ExperimentHelper.INSTANCE.getVariantFrom(experimentsRepository.getExperiments(), knownVariant);
    }
}