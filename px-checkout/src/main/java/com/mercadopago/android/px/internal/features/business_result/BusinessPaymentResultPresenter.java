package com.mercadopago.android.px.internal.features.business_result;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mercadopago.android.px.addons.FlowBehaviour;
import com.mercadopago.android.px.internal.base.BasePresenter;
import com.mercadopago.android.px.internal.features.payment_congrats.model.FlowBehaviourResultMapper;
import com.mercadopago.android.px.internal.features.payment_congrats.model.PaymentCongratsModel;
import com.mercadopago.android.px.internal.features.payment_congrats.model.PaymentCongratsResponse;
import com.mercadopago.android.px.internal.features.payment_result.CongratsAutoReturn;
import com.mercadopago.android.px.internal.features.payment_result.presentation.PaymentResultFooter;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.internal.view.PaymentResultBody;
import com.mercadopago.android.px.model.ExitAction;
import com.mercadopago.android.px.model.internal.PrimaryExitAction;
import com.mercadopago.android.px.model.internal.SecondaryExitAction;
import com.mercadopago.android.px.tracking.internal.events.AbortEvent;
import com.mercadopago.android.px.tracking.internal.events.CongratsSuccessDeepLink;
import com.mercadopago.android.px.tracking.internal.events.CrossSellingEvent;
import com.mercadopago.android.px.tracking.internal.events.DeepLinkType;
import com.mercadopago.android.px.tracking.internal.events.DiscountItemEvent;
import com.mercadopago.android.px.tracking.internal.events.DownloadAppEvent;
import com.mercadopago.android.px.tracking.internal.events.PrimaryActionEvent;
import com.mercadopago.android.px.tracking.internal.events.ScoreEvent;
import com.mercadopago.android.px.tracking.internal.events.SecondaryActionEvent;
import com.mercadopago.android.px.tracking.internal.events.SeeAllDiscountsEvent;
import com.mercadopago.android.px.tracking.internal.events.ViewReceiptEvent;
import com.mercadopago.android.px.tracking.internal.views.ResultViewTrack;

/* default */ class BusinessPaymentResultPresenter extends BasePresenter<BusinessPaymentResult.View>
    implements BusinessPaymentResult.Presenter, PaymentResultBody.Listener {

    @NonNull /* default */ final PaymentCongratsModel model;
    /* default */ final ResultViewTrack viewTracker;
    private final FlowBehaviour flowBehaviour;
    @Nullable /* default */ CongratsAutoReturn autoReturnTimer;

    /* default */ BusinessPaymentResultPresenter(@NonNull final PaymentCongratsModel model,
        @NonNull final FlowBehaviour flowBehaviour, final boolean isMP) {
        this.model = model;
        this.flowBehaviour = flowBehaviour;
        viewTracker = new ResultViewTrack(model, isMP);
    }

    @Override
    public void attachView(final BusinessPaymentResult.View view) {
        super.attachView(view);
        configureView();
    }

    @Override
    public void onFreshStart() {
        viewTracker.track();
        flowBehaviour.trackConversion(new FlowBehaviourResultMapper().map(model.getCongratsType()));
    }

    @Override
    public void onStart() {
        if (autoReturnTimer != null) {
            autoReturnTimer.start();
        }
    }

    @Override
    public void onStop() {
        if (autoReturnTimer != null) {
            autoReturnTimer.cancel();
        }
    }

    @Override
    public void onAbort() {
        new AbortEvent(viewTracker).track();
        final PaymentCongratsResponse congratsResponse = model.getPaymentCongratsResponse();
        getView().processCustomExit(congratsResponse.getBackUrl(), congratsResponse.getRedirectUrl());
    }

    private void configureView() {
        final BusinessPaymentResultViewModel viewModel = new BusinessPaymentResultMapper().map(model);
        getView().configureViews(viewModel, this, new PaymentResultFooter.Listener() {
            @Override
            public void onClick(@NonNull final ExitAction action) {
                if (action instanceof PrimaryExitAction) {
                    new PrimaryActionEvent(viewTracker).track();
                } else if (action instanceof SecondaryExitAction) {
                    new SecondaryActionEvent(viewTracker).track();
                }
                final PaymentCongratsResponse congratsResponse = model.getPaymentCongratsResponse();
                getView().processCustomExit(action, congratsResponse.getBackUrl(), congratsResponse.getRedirectUrl());
            }

            @Override
            public void onClick(@NonNull final String target) {
                getView().launchDeepLink(target);
            }
        });
        getView().setStatusBarColor(viewModel.getHeaderModel().getBackgroundColor());
        final CongratsAutoReturn.Model autoReturnModel = viewModel.getAutoReturnModel();
        if (autoReturnModel != null) {
            initAutoReturn(autoReturnModel);
        }
    }

    private void initAutoReturn(@NonNull final CongratsAutoReturn.Model model) {
        autoReturnTimer = new CongratsAutoReturn(model, new CongratsAutoReturn.Listener() {
            @Override
            public void onFinish() {
                autoReturnTimer = null;
                onAbort();
            }

            @Override
            public void updateView(@NonNull final String label) {
                getView().updateAutoReturnLabel(label);
            }
        });
    }

    @Override
    public void OnClickDownloadAppButton(@NonNull final String deepLink) {
        new DownloadAppEvent(viewTracker).track();
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void OnClickCrossSellingButton(@NonNull final String deepLink) {
        new CrossSellingEvent(viewTracker).track();
        getView().processCrossSellingBusinessAction(deepLink);
    }

    @Override
    public void onClickLoyaltyButton(@NonNull final String deepLink) {
        new ScoreEvent(viewTracker).track();
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void onClickShowAllDiscounts(@NonNull final String deepLink) {
        new SeeAllDiscountsEvent(viewTracker).track();
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void onClickViewReceipt(@NonNull final String deepLink) {
        new ViewReceiptEvent(viewTracker).track();
        getView().launchDeepLink(deepLink);
    }

    @Override
    public void onClickTouchPoint(@Nullable final String deepLink) {
        new DiscountItemEvent(viewTracker, 0, TextUtil.EMPTY).track();
        if (deepLink != null) {
            getView().launchDeepLink(deepLink);
        }
    }

    @Override
    public void onClickDiscountItem(final int index, @Nullable final String deepLink, @Nullable final String trackId) {
        new DiscountItemEvent(viewTracker, index, trackId).track();
        if (deepLink != null) {
            getView().launchDeepLink(deepLink);
        }
    }

    @Override
    public void onClickMoneySplit() {
        final PaymentCongratsResponse.ExpenseSplit moneySplit = model.getPaymentCongratsResponse().getExpenseSplit();
        final String deepLink;
        if (moneySplit != null && (deepLink = moneySplit.getAction().getTarget()) != null) {
            new CongratsSuccessDeepLink(DeepLinkType.MONEY_SPLIT_TYPE, deepLink).track();
            getView().launchDeepLink(deepLink);
        }
    }
}
