package com.mercadopago.android.px.internal.features.payment_result.components;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.internal.util.ViewUtils;
import com.mercadopago.android.px.internal.view.ActionDispatcher;
import com.mercadopago.android.px.internal.view.CompactComponent;
import com.mercadopago.android.px.internal.view.MPTextView;
import com.mercadopago.android.px.model.InstructionReference;

public class InstructionReferenceComponent
    extends CompactComponent<InstructionReferenceComponent.Props, ActionDispatcher> {

    /* default */ InstructionReferenceComponent(@NonNull final Props props, @NonNull final ActionDispatcher dispatcher) {
        super(props, dispatcher);
    }

    public static class Props {
        public final InstructionReference reference;

        public Props(final Builder builder) {
            reference = builder.reference;
        }

        public Builder toBuilder() {
            return new Props.Builder().setReference(reference);
        }

        public static class Builder {
            InstructionReference reference;

            Builder setReference(@NonNull final InstructionReference reference) {
                this.reference = reference;
                return this;
            }

            public Props build() {
                return new Props(this);
            }
        }
    }

    @Override
    public View render(@NonNull final ViewGroup parent) {
        final Context context = parent.getContext();
        @SuppressLint("InflateParams")
        final View referenceView =
            LayoutInflater.from(context).inflate(R.layout.px_payment_result_instruction_reference, null);
        final MPTextView labelTextView = referenceView.findViewById(R.id.mpsdkReferenceLabel);
        final MPTextView valueTextView = referenceView.findViewById(R.id.mpsdkReferenceValue);
        final MPTextView commentTextView = referenceView.findViewById(R.id.mpsdkReferenceComment);

        ViewUtils.loadOrGone(props.reference.getLabel(), labelTextView);

        final String formattedReference = props.reference.getFormattedReference();
        ViewUtils.loadOrGone(formattedReference, valueTextView);

        ViewUtils.loadOrGone(props.reference.getComment(), commentTextView);

        return referenceView;
    }
}
