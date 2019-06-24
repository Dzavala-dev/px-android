package com.mercadopago.android.px.internal.features.review_payment_methods;

import com.mercadopago.android.px.mocks.PaymentMethods;
import com.mercadopago.android.px.model.PaymentMethod;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ReviewPaymentMethodsPresenterTest {

    @Mock private ReviewPaymentMethods.View view;

    private ReviewPaymentMethodsPresenter presenter;
    private List<PaymentMethod> stubPaymentMethodList;

    @Before
    public void setUp() {
        stubPaymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        presenter = new ReviewPaymentMethodsPresenter(stubPaymentMethodList);
        presenter.attachView(view);
    }

    @Test
    public void whenInitializeThenShowSupportedPaymentMethodsList() {
        presenter.initialize();
        verify(view).initializeSupportedPaymentMethods(stubPaymentMethodList);
    }
}
