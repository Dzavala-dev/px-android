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
import com.mercadopago.android.px.model.InstructionAction;
import com.mercadopago.android.px.model.Interaction;

public class InstructionInteractionComponent
    extends CompactComponent<InstructionInteractionComponent.Props, ActionDispatcher> {

    /* default */ InstructionInteractionComponent(@NonNull final Props props, @NonNull final ActionDispatcher dispatcher) {
        super(props, dispatcher);
    }

    public static class Props {
        /* default */ final Interaction interaction;

        public Props(final Props.Builder builder) {
            interaction = builder.interaction;
        }

        public Props.Builder toBuilder() {
            return new Props.Builder().setInteraction(interaction);
        }

        public static class Builder {
            Interaction interaction;

            /* default */ Props.Builder setInteraction(
                @NonNull final Interaction interaction) {
                this.interaction = interaction;
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
        final View view =
            LayoutInflater.from(context).inflate(R.layout.px_payment_result_instruction_interaction, null);
        final ViewGroup interactionContainer = view.findViewById(R.id.mpsdkInstructionInteractionContainer);
        final MPTextView title = view.findViewById(R.id.mpsdkInteractionTitle);
        final MPTextView content = view.findViewById(R.id.mpsdkInteractionContent);

        final Interaction interaction = props.interaction;
        ViewUtils.loadOrGone(interaction.getTitle(), title);
        ViewUtils.loadOrGone(interaction.getContent(), content);

        final InstructionAction action = interaction.getAction();
        if (action != null) {
            final InstructionsAction.Prop props =
                new InstructionsAction.Prop.Builder().setInstructionAction(action).build();

            new InstructionsAction(props, getActions()).render(interactionContainer);
        }
        return view;
    }
}
