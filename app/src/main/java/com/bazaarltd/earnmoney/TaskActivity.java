package com.bazaarltd.earnmoney;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import java.util.HashMap;
import java.util.Map;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bazaarltd.earnmoney.utils.Facts;
import com.bazaarltd.earnmoney.utils.Helper;
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskActivity extends AppCompatActivity {

    ConnectivityManager connectivityManager;
    NetworkInfo activeNetwork;
    TextView taskButton;
    TextView factText, impressionCountText, impressionTargetText;
    ProgressBar loader;
    LinearLayout taskContainer;
    CountDownTimer countDownTimer;
    boolean  mTimerRunning;
    int adImpressionCount = 0;
    int adReloadTrial = 0;

    int VIEW_TARGET = 20, IMPRESSION_TIME = 1000 * 5,
            CLICK_TIME = 1000 * 10, BUTTON_CT_TIME = 1000 * 3,
            CLICK_TTIMER = 1, IM_TTIMER = 1,IM_TTIMER_SUCCESS = 1,
            CLICK_TTIMER_SUCCESS = 1, AUTO_BACK = 1;


    String USER_ACCOUNT;
    StartAppAd startAppAd;
    Boolean AD_CLICKED = false;
    Boolean AD_LOADED = false;
    Boolean AD_VIEW_SUCCESS = false;
    Boolean AD_CLICK_SUCCESS = false;
    Boolean SHOW_AD_AUTOMATICALLY = false;
    RelativeLayout relativeLayout;

    App_Controller app_controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        app_controller = new App_Controller(this);
        startAppAd = new StartAppAd(this);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = connectivityManager.getActiveNetworkInfo();

        taskButton = findViewById(R.id.taskButton);
        loader = findViewById(R.id.loader);
        taskContainer = findViewById(R.id.taskContainer);
        relativeLayout = findViewById(R.id.mainLayout);


        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
                Color.parseColor(app_controller.getColorCode1()),
                Color.parseColor(app_controller.getColorCode2())
        });

        gd.setCornerRadius(0f);
        relativeLayout.setBackgroundDrawable(gd);

        taskButton.setBackgroundColor(Color.parseColor(app_controller.getColorCode1()));

        //relativeLayout.setBackgroundColor(Color.parseColor("#03fc98"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(app_controller.getColorCode1()));
        }

        factText = findViewById(R.id.factText);
        impressionCountText = findViewById(R.id.impressionCountText);
        impressionTargetText = findViewById(R.id.impressionTargetText);

        USER_ACCOUNT = Helper.getUserAccount(this);
        adImpressionCount = Helper.getSuccessImpressionCount(this);

        factText.setTypeface(Typeface.createFromAsset(this.getAssets(), "kalpurush.ttf"));
        impressionCountText.setTypeface(Typeface.createFromAsset(this.getAssets(), "kalpurush.ttf"));
        impressionTargetText.setTypeface(Typeface.createFromAsset(this.getAssets(), "kalpurush.ttf"));

        taskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View p1)
            {
                SHOW_AD_AUTOMATICALLY = true;
                adReloadTrial = 0;
                showStartAppAd();
            }
        });

        fetchTaskData(USER_ACCOUNT);
    }

    private void sweetAlertDialog(String dialogMessage, final String dialogType, final Boolean finishActivity)
    {
        SweetAlertDialog sweetAlert = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        sweetAlert.setTitleText(dialogMessage);
        sweetAlert.setCancelable(false);
        if (dialogType == "WARNING_TYPE")
        {
            sweetAlert.changeAlertType(SweetAlertDialog.WARNING_TYPE);
        }
        sweetAlert.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlert)
            {
                if (finishActivity)
                {
                    finish();
                }
                else
                {
                    sweetAlert.dismiss();
                }
            }
        });
        sweetAlert.show();
    }

    public void loadUrl(String mUrl)
    {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(mUrl));
        startActivity(i);
    }

    private void loadStartAppAd() {
        startAppAd.loadAd(StartAppAd.AdMode.AUTOMATIC, new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                if( SHOW_AD_AUTOMATICALLY ) {
                    showStartAppAd();
                }
            }
            @Override
            public void onFailedToReceiveAd(Ad ad) {
                if( adReloadTrial < 4) {
                    loadStartAppAd();
                    adReloadTrial++;
                    return;
                }
                Toast("Ad failed to load");
            }
        });
    }

    private void showStartAppAd() {
        startAppAd.showAd(new AdDisplayListener() {
            @Override
            public void adHidden(Ad ad) {

                loadStartAppAd();

                if ( !isClaimEnabled() )
                {
                    countDownTimer.cancel();
                    if (AD_VIEW_SUCCESS)
                    {
                        onViewSuccess();
                    }
                    else
                    {
                        sweetAlertDialog("You must wait " + IMPRESSION_TIME / 1000 + " seconds", "WARNING_TYPE", false);
                    }
                }
                else
                {
                    if (AD_CLICKED) {
                        showLoader();
                        countDownTimer = new CountDownTimer(CLICK_TIME, 1000) {
                            public void onTick(long millisUntilFinished)
                            {
                                Toast("Wait " + millisUntilFinished / 1000);
                            }
                            public void onFinish()
                            {
                                AD_CLICK_SUCCESS = true;
                                mTimerRunning = false;
                                hideLoader();
                                if(CLICK_TTIMER == 1) {
                                    if(AUTO_BACK == 1) {
                                        Toast("Click Success! Wait, back automatically");
                                    }
                                    else {
                                        Toast("Click Success!");
                                    }
                                }
                                countDownTimer.cancel();
                                Helper.resetSuccessImpressionCount(TaskActivity.this);
                                authTask(USER_ACCOUNT);
                            }
                        }.start();
                        mTimerRunning = true;
                    }
                    else
                    {
                        sweetAlertDialog("You must click the ad", "WARNING_TYPE", false);
                    }
                }

            }
            @Override
            public void adDisplayed(Ad ad) {

                AD_CLICKED = false;
                AD_VIEW_SUCCESS = false;
                SHOW_AD_AUTOMATICALLY = false;

                if ( !isClaimEnabled() )
                {
                    countDownTimer = new CountDownTimer(IMPRESSION_TIME, 1000) {
                        public void onTick(long millisUntilFinished)
                        {
                            if(IM_TTIMER == 1) {
                                Toast("Wait " + millisUntilFinished / 1000);
                            }
                        }
                        public void onFinish()
                        {
                            AD_VIEW_SUCCESS = true;
                            if(IM_TTIMER == 1) {
                                Toast("Close this Ad");
                            }
                        }
                    }.start();
                }
                else
                {
                    Toast("Click this Ad");
                }

            }
            @Override
            public void adClicked(Ad ad) {
                AD_CLICKED = true;
                if( !isClaimEnabled() ) {
                    addInvalidClick(USER_ACCOUNT);
                    countDownTimer.cancel();
                }
            }

            @Override
            public void adNotDisplayed(Ad ad) {
            }
        });
    }

    private void Toast(String ToastString)
    {
        Toast.makeText(getApplicationContext(), ToastString, Toast.LENGTH_SHORT).show();
    }

    private void authTask(String user)
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        final String url = Constants.TASK_AUTH_API_URL;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response)
                    {

                        startActivity(new Intent(TaskActivity.this, MainActivity.class));
                        finishAffinity();


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                sweetAlertDialog("Network Error", "WARNING_TYPE", true);
            }

        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("user", USER_ACCOUNT);
                params.put("did", Helper.getDeviceId(TaskActivity.this));
                params.put("task-app", Constants.TASK_APP_INDEX);
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", Base64.encodeToString(USER_ACCOUNT.getBytes(), Base64.DEFAULT));
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    private void addInvalidClick(String user)
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        final String url = Constants.INVALID_CLICK_API_URL
                + "?user=" + Helper.getUserAccount(this)
                + "&did=" + Helper.getDeviceId(this)
                + "&" + Constants.EXTRA_PARAMS;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response)
                    {
                        Intent i = new Intent(TaskActivity.this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                sweetAlertDialog("Network Error", "WARNING_TYPE", true);
            }
        });
        queue.add(stringRequest);
    }

    private void fetchTaskData(String user)
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        final String url = Constants.TASK_DATA_API_URL
                + "?user=" + Helper.getUserAccount(this)
                + "&did=" + Helper.getDeviceId(this)
                + "&" + Constants.EXTRA_PARAMS;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response)
                    {
                        try
                        {
                            JSONObject data = new JSONObject(response);

                            if ( !data.getBoolean("success") ) {
                                sweetAlertDialog( data.getString("message"), "WARNING_TYPE", true );
                                return;
                            }

                            if( !data.isNull("vpn_required") ) {
                                if (!data.getBoolean("vpn_required") && is_VPN_connected()) {
                                    sweetAlertDialog("VPN not allowed", "WARNING_TYPE", true);
                                    return;
                                }

                                if (data.getBoolean("vpn_required") && !is_VPN_connected()) {
                                    sweetAlertDialog("Connect VPN", "WARNING_TYPE", true);
                                    return;
                                }
                            }

                            VIEW_TARGET = data.getInt("view_target");
                            CLICK_TTIMER = data.getInt("click_ttimer");
                            IM_TTIMER = data.getInt("im_ttimer");
                            AUTO_BACK = data.getInt("auto_back");

                            IMPRESSION_TIME = data.getInt("impression_time") * 1000;
                            CLICK_TIME = data.getInt("click_time") * 1000;
                            BUTTON_CT_TIME = data.getInt("button_timer") * 1000;

                            setText();
                            hideLoader();

                            loadStartAppAd();

                        }
                        catch (JSONException e)
                        {
                            Toast(e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                sweetAlertDialog("Network error!", "WARNING_TYPE", true);
            }
        });
        queue.add(stringRequest);
    }

    public Boolean is_VPN_connected()
    {
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN).isConnectedOrConnecting();
    }

    private Boolean isClaimEnabled() {
        return adImpressionCount >= VIEW_TARGET;
    }

    private void setText() {
        impressionCountText.setText( Helper.getBangla(String.valueOf(adImpressionCount)) );
        impressionTargetText.setText( "/" + Helper.getBangla(String.valueOf(VIEW_TARGET)) );
        factText.setText(Facts.getFactByIndex(adImpressionCount));
        if ( isClaimEnabled() ) {
            taskButton.setText("Claim");
            return;
        }
        taskButton.setText("Next");
    }

    private void onViewSuccess() {
        adImpressionCount = adImpressionCount + 1;
        Helper.setSuccessImpressionCount(this, adImpressionCount);
        setText();
        startButttonTimer(BUTTON_CT_TIME);
    }

    private void startButttonTimer(int second) {
        if (second < 3) return;
        CountDownTimer cTimer = new CountDownTimer(second, 1000) {
            public void onTick(long millisUntilFinished)
            {
                taskButton.setEnabled(false);
                taskButton.setText("Wait " + millisUntilFinished / 1000);
            }
            public void onFinish()
            {
                taskButton.setEnabled(true);
                if ( isClaimEnabled() ) {
                    taskButton.setText("Claim");
                    return;
                }
                taskButton.setText("Next");
            }
        }.start();
    }

    private void showLoader() {
        loader.setVisibility(View.VISIBLE);
        taskContainer.setVisibility(View.GONE);
    }

    private void hideLoader() {
        loader.setVisibility(View.GONE);
        taskContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mTimerRunning){
            countDownTimer.cancel();
            Toast.makeText(this, "Your doing mistake.", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mTimerRunning){
            Toast.makeText(this, "Please Wait until Timer Finished", Toast.LENGTH_SHORT).show();
        }else {
            finish();
        }
    }
}
