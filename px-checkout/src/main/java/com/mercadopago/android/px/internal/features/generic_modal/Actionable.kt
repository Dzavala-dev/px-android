package com.mercadopago.android.px.internal.features.generic_modal

import android.os.Parcelable
import com.mercadopago.android.px.internal.extensions.isNotNull
import com.mercadopago.android.px.internal.extensions.isNotNullNorEmpty
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Actionable(
    val label: String,
    val deepLink: String?,
    val action: ActionType?) : Parcelable {

    init {
        check(deepLink.isNotNullNorEmpty() || action.isNotNull()) {
            "An ${javaClass.simpleName} should have a deepLink or an action to follow"
        }
    }
}
