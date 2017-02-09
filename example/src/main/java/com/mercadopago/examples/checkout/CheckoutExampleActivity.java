package com.mercadopago.examples.checkout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mercadopago.callbacks.Callback;
import com.mercadopago.callbacks.PaymentCallback;
import com.mercadopago.callbacks.ReviewableCallback;
import com.mercadopago.constants.Sites;
import com.mercadopago.core.MercadoPago;
import com.mercadopago.core.MercadoPagoCheckout;
import com.mercadopago.core.MerchantServer;
import com.mercadopago.examples.R;
import com.mercadopago.examples.reviewables.CellphoneReview;
import com.mercadopago.examples.utils.ColorPickerDialog;
import com.mercadopago.examples.utils.ExamplesUtils;
import com.mercadopago.exceptions.MercadoPagoError;
import com.mercadopago.model.ApiException;
import com.mercadopago.model.Item;
import com.mercadopago.model.PaymentData;
import com.mercadopago.preferences.CheckoutPreference;
import com.mercadopago.preferences.DecorationPreference;
import com.mercadopago.model.Payment;
import com.mercadopago.preferences.FlowPreference;
import com.mercadopago.preferences.ServicePreference;
import com.mercadopago.util.JsonUtil;
import com.mercadopago.util.LayoutUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class CheckoutExampleActivity extends AppCompatActivity {

    private Activity mActivity;
    private ImageView mColorSample;
    private CheckBox mDarkFontEnabled;
    private ProgressBar mProgressBar;
    private View mRegularLayout;

    private CheckoutPreference mCheckoutPreference;
    private String mPublicKey;
    private Integer mDefaultColor;
    private Integer mSelectedColor;

    //Result
    private PaymentData mPaymentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_example);
        mActivity = this;
        mColorSample = (ImageView) findViewById(R.id.colorSample);
        mDarkFontEnabled = (CheckBox) findViewById(R.id.darkFontEnabled);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mRegularLayout = findViewById(R.id.regularLayout);
        mPublicKey = ExamplesUtils.DUMMY_MERCHANT_PUBLIC_KEY;
        mDefaultColor = ContextCompat.getColor(this, R.color.colorPrimary);
    }

    public void changeColor(View view) {
        new ColorPickerDialog(this, mDefaultColor, new ColorPickerDialog.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                mDarkFontEnabled.setEnabled(true);
                mColorSample.setBackgroundColor(color);
                mSelectedColor = color;
            }
        }).show();
    }

    public void onContinueClicked(View view) {
        showProgressLayout();
        Map<String, Object> map = new HashMap<>();
        map.put("item_id", "1");
        map.put("amount", new BigDecimal(300));
        MerchantServer.createPreference(this, "http://private-4d9654-mercadopagoexamples.apiary-mock.com/",
                "merchantUri/create_preference", map, new Callback<CheckoutPreference>() {
                    @Override
                    public void success(CheckoutPreference checkoutPreference) {
                        mCheckoutPreference = checkoutPreference;
                        startMercadoPagoCheckout();
                    }

                    @Override
                    public void failure(ApiException error) {
                        showRegularLayout();
                        Toast.makeText(mActivity, getString(R.string.preference_creation_failed), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startMercadoPagoCheckout() {

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("company_id", "movistar");
        additionalInfo.put("phone_number", "111111");

        ServicePreference servicePreference = new ServicePreference.Builder()
                .setCreatePaymentURL("http://private-4d9654-mercadopagoexamples.apiary-mock.com", "create_payment", additionalInfo)
                .build();

        CellphoneReview cellphoneReview = new CellphoneReview(this, "15111111");
        cellphoneReview.setReviewableCallback(new ReviewableCallback() {
            @Override
            public void onChangeRequired(PaymentData paymentData) {
                Toast.makeText(getBaseContext(), "Change button clicked!", Toast.LENGTH_SHORT).show();
                mPaymentData = paymentData;
                doSomething();
            }
        });


        DecorationPreference decorationPreference = new DecorationPreference.Builder()
                .setCustomFont("fonts/Merriweather-Light.ttf")
                .build();


        CheckoutPreference localCheckoutPreference = new CheckoutPreference.Builder()
                .setSite(Sites.ARGENTINA)
                .addItem(new Item("Recarga Movistar", BigDecimal.ONE))
                .build();

        new MercadoPagoCheckout.Builder()
                .setContext(this)
                .setPublicKey(mPublicKey)
                .setDecorationPreference(decorationPreference)
                .setCheckoutPreference(localCheckoutPreference)
                .setServicePreference(servicePreference)
                .addReviewable(cellphoneReview)
                .start(new PaymentCallback() {
                    @Override
                    public void onSuccess(Payment payment) {
                        //Done!
                        Log.d("log", "success");
                        Log.d("log", payment.getStatus());
                    }

                    @Override
                    public void onCancel() {
                        //User canceled
                        Log.d("log", "cancel");
                    }

                    @Override
                    public void onFailure(MercadoPagoError exception) {
                        //Failure in checkout
                        Log.d("log", "failure");
                    }
                });
    }

    private DecorationPreference.Builder getCurrentDecorationPreferenceBuilder() {
        DecorationPreference.Builder builder = new DecorationPreference.Builder();
        if (mSelectedColor != null) {
            builder.setBaseColor(mSelectedColor);
            if (mDarkFontEnabled.isChecked()) {
                builder.enableDarkFont();
            }
        }
        return builder;
    }

    private void doSomething() {
        //After doing something, we start MercadoPagoCheckout with PaymentData
        Toast.makeText(this, "Restarting!", Toast.LENGTH_SHORT).show();
        startMercadoPagoCheckoutWithPaymentData();
    }

    private void startMercadoPagoCheckoutWithPaymentData() {

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("company_id", "movistar");
        additionalInfo.put("phone_number", "111111");

        DecorationPreference decorationPreference = new DecorationPreference.Builder()
                .setCustomFont("fonts/Merriweather-Light.ttf")
                .build();

        CheckoutPreference checkoutPreference = new CheckoutPreference.Builder()
                .setSite(Sites.ARGENTINA)
                .setPayerAccessToken("TEST-7176766875549918-111008-fa5660d2d0aa37532716eb2bf2f9089b__LB_LC__-192992930")
                .addItem(new Item("Recarga Movistar", BigDecimal.TEN))
                .enableAccountMoney()
                .build();

        ServicePreference servicePreference = new ServicePreference.Builder()
                .setCreatePaymentURL("http://private-4d9654-mercadopagoexamples.apiary-mock.com", "create_payment", additionalInfo)
                .build();

        CellphoneReview cellphoneReview = new CellphoneReview(this, "15999999");
        cellphoneReview.setReviewableCallback(new ReviewableCallback() {
            @Override
            public void onChangeRequired(PaymentData paymentData) {
                Toast.makeText(getBaseContext(), "Change button clicked!", Toast.LENGTH_SHORT).show();
                mPaymentData = paymentData;
                doSomething();
            }
        });

        new MercadoPagoCheckout.Builder()
                .setContext(this)
                .setPublicKey(mPublicKey)
                .setCheckoutPreference(checkoutPreference)
                .setServicePreference(servicePreference)
                .setPaymentData(mPaymentData)
                .setDecorationPreference(decorationPreference)
                .addReviewable(cellphoneReview)
                .start(new PaymentCallback() {
                    @Override
                    public void onSuccess(Payment payment) {
                        //Done!
                        Log.d("log", "success");
                        Log.d("log", payment.getStatus());
                    }

                    @Override
                    public void onCancel() {
                        //User canceled
                        Log.d("log", "cancel");
                    }

                    @Override
                    public void onFailure(MercadoPagoError exception) {
                        //Failure in checkout
                        Log.d("log", "failure");
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LayoutUtil.showRegularLayout(this);

        if (requestCode == MercadoPago.CHECKOUT_REQUEST_CODE) {
            showRegularLayout();
            if (resultCode == RESULT_OK && data != null) {

                // Set message
                Payment payment = JsonUtil.getInstance().fromJson(data.getStringExtra("payment"), Payment.class);
                Toast.makeText(mActivity, getString(R.string.payment_received_congrats) + payment.getId(), Toast.LENGTH_LONG).show();

            } else {
                if ((data != null) && (data.getStringExtra("mpException") != null)) {
                    MercadoPagoError mercadoPagoError = JsonUtil.getInstance().fromJson(data.getStringExtra("mercadoPagoError"), MercadoPagoError.class);
                    Toast.makeText(mActivity, mercadoPagoError.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        showRegularLayout();
    }

    private void showRegularLayout() {
        mProgressBar.setVisibility(View.GONE);
        mRegularLayout.setVisibility(View.VISIBLE);
    }

    private void showProgressLayout() {
        mProgressBar.setVisibility(View.VISIBLE);
        mRegularLayout.setVisibility(View.GONE);
    }

    public void resetSelection(View view) {
        mSelectedColor = null;
        mColorSample.setBackgroundColor(mDefaultColor);
        mDarkFontEnabled.setChecked(false);
        mDarkFontEnabled.setEnabled(false);
    }
}
