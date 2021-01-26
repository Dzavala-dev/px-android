package com.mercadopago.android.px.internal.features.generic_modal

import com.google.gson.annotations.SerializedName

sealed class GenericDialogAction {
    class DeepLinkAction(val deepLink: String) : GenericDialogAction()
    class CustomAction(val type: ActionType) : GenericDialogAction()
}

enum class ActionType {
    @SerializedName("pay_with_other_method") PAY_WITH_OTHER_METHOD,
    @SerializedName("pay_with_offline_method") PAY_WITH_OFFLINE_METHOD,
    @SerializedName("add_new_card") ADD_NEW_CARD,
    @SerializedName("dismiss") DISMISS
}
