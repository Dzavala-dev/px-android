package com.mercadopago.android.px.model.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import com.mercadopago.android.px.configuration.DiscountParamsConfiguration;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.model.commission.PaymentTypeChargeRule;
import com.mercadopago.android.px.preferences.CheckoutPreference;
import java.util.ArrayList;
import java.util.Collection;

/**
 * DTO that represents init informaton from checkout.
 */
@SuppressWarnings("unused")
public final class InitRequest {

    @NonNull private final String publicKey;

    @NonNull private final Collection<String> cardsWithEsc;

    @NonNull private final Collection<PaymentTypeChargeRule> charges;

    @SerializedName("discount_configuration")
    @NonNull private final DiscountParamsConfiguration discountParamsConfiguration;

    /**
     * Specific feature related params.
     */
    @NonNull private final CheckoutFeatures features;

    /**
     * When when there is a "close" preference this value is not null.
     */
    @Nullable private final String preferenceId;

    /**
     * When it's an "open" preference, this value is not null. Open preference means a fictional preference (does not
     * exists in backend)
     */
    @Nullable private final CheckoutPreference preference;

    @Nullable private final String flowId;

    /* default */ InitRequest(final Builder builder) {
        publicKey = builder.publicKey;
        cardsWithEsc = builder.cardsWithEsc;
        charges = builder.charges;
        discountParamsConfiguration = builder.discountParamsConfiguration;
        features = builder.features;
        preference = builder.preference;
        preferenceId = builder.preferenceId;
        flowId = builder.flowId;
    }

    public static class Builder {
        /* default */ @NonNull final String publicKey;
        /* default */ @NonNull Collection<String> cardsWithEsc = new ArrayList<>();
        /* default */ @NonNull Collection<PaymentTypeChargeRule> charges = new ArrayList<>();
        /* default */ @NonNull DiscountParamsConfiguration discountParamsConfiguration =
            new DiscountParamsConfiguration.Builder().build();
        /* default */ @NonNull CheckoutFeatures features = new CheckoutFeatures.Builder().build();

        /* default */ @Nullable CheckoutPreference preference;
        /* default */ @Nullable String preferenceId;
        /* default */ @Nullable String flowId = TextUtil.EMPTY;

        public Builder(@NonNull final String publicKey) {
            this.publicKey = publicKey;
        }

        public Builder setCardWithEsc(@NonNull final Collection<String> cardsWithEsc) {
            this.cardsWithEsc.addAll(cardsWithEsc);
            return this;
        }

        public Builder setCharges(@NonNull final Collection<PaymentTypeChargeRule> charges) {
            this.charges.addAll(charges);
            return this;
        }

        public Builder setDiscountParamsConfiguration(final DiscountParamsConfiguration discountParamsConfiguration) {
            this.discountParamsConfiguration = discountParamsConfiguration;
            return this;
        }

        public Builder setCheckoutFeatures(@NonNull final CheckoutFeatures features) {
            this.features = features;
            return this;
        }

        public Builder setCheckoutPreference(@Nullable final CheckoutPreference preference) {
            this.preference = preference;
            return this;
        }

        public Builder setCheckoutPreferenceId(@Nullable final String preferenceId) {
            this.preferenceId = preferenceId;
            return this;
        }

        public Builder setFlowId(@Nullable final String flowId) {
            this.flowId = flowId;
            return this;
        }

        public InitRequest build() {
            return new InitRequest(this);
        }
    }
}