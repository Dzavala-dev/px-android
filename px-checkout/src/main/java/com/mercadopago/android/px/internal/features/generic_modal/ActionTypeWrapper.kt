package com.mercadopago.android.px.internal.features.generic_modal

import com.mercadopago.android.px.model.ExpressMetadata

class ActionTypeWrapper(var items: List<ExpressMetadata>) {

    val indexToReturn: Int
    val actionType: ActionType

    init {
        var indexOfNewCard = -1
        var indexOfOfflineMethod = -1
        var index = 0
        var hasActiveMethods = false
        items.forEach {
            when {
                it.isOfflineMethods -> indexOfOfflineMethod = index
                it.isNewCard -> indexOfNewCard = index
                it.status.isActive && !it.isOfflineMethods && !it.isNewCard -> hasActiveMethods = true
            }
            index++
        }

        when {
            hasActiveMethods -> {
                actionType = ActionType.PAY_WITH_OTHER_METHOD
                indexToReturn = 0
            }
            indexOfOfflineMethod > 0 -> {
                actionType = ActionType.PAY_WITH_OFFLINE_METHOD
                indexToReturn = indexOfOfflineMethod
            }
            indexOfNewCard > 0 -> {
                actionType = ActionType.ADD_NEW_CARD
                indexToReturn = indexOfNewCard
            }
            else -> {
                actionType = ActionType.PAY_WITH_OTHER_METHOD
                indexToReturn = 0
            }
        }
    }
}
