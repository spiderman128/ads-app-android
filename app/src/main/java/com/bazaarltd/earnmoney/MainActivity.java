package com.bazaarltd.earnmoney;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bazaarltd.earnmoney.utils.Helper;
import com.bazaarltd.earnmoney.utils.TinyDB;
import com.google.android.material.navigation.NavigationView;
import com.onesignal.OneSignal;
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;

import org.json.JSONObject;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private SpotsDialog spotsDialog;
    private TinyDB tinydb;
    private ConnectivityManager connectivityManager;
    private NetworkInfo activeNetwork;
    private WebView webView;
    private Boolean connectionErrorDialogShown = false;
    private String telegramChannel, telegramGroup;
    App_Controller app_controller;


    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app_controller = new App_Controller(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

       changeColor();

        StartAppSDK.init(this, app_controller.getStartAppAdsCode(), true);
        StartAppAd.disableSplash();
        StartAppAd.enableConsent(this, false);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        navigationView.setItemIconTintList(null);

        webView = findViewById(R.id.webView);
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().startSync();


        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("dd");
        String currentDateAndTime = sdf.format(new Date());


        if (app_controller.getDate() < Integer.parseInt(currentDateAndTime)) {
            app_controller.setDate(Integer.parseInt(currentDateAndTime));
            app_controller.spin1DailyTaskLimitCounter(0);
            app_controller.spin1DailyTaskCounter(0);
        }

        spotsDialog = new SpotsDialog(MainActivity.this, R.style.SpotsDialog);
        spotsDialog.setCancelable(false);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = connectivityManager.getActiveNetworkInfo();

        tinydb = new TinyDB(this);

        if (Constants.SHOW_DRAWER) {
            initNavigationDrawer();
        }
        fetchAppData();

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.setWebViewClient(new MyBrowser());
        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        if (!connectionErrorDialogShown) {
            webView.loadUrl(Constants.HOME_URL
                    + "?user=" + Helper.getUserAccount(this)
                    + "&did=" + Helper.getDeviceId(this)
                    + "&" + Constants.EXTRA_PARAMS);
        }

    }


    private void changeColor() {
        toolbar.setBackgroundColor(Color.parseColor(app_controller.getColorCode1()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(app_controller.getColorCode1()));
        }
    }

    public void initNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                drawerLayout.closeDrawers();
                int id = item.getItemId();
                switch (id) {
                    case R.id.nav_profile:
                        openWebActivity("Profile", "profile");
                        break;
                    case R.id.nav_wallet:
                        openWebActivity("Wallet", "wallet");
                        break;
                    case R.id.nav_history:
                        openWebActivity("Payments", "payments");
                        break;
                    case R.id.nav_channel:
                        loadUrl(telegramChannel);
                        break;
                    case R.id.nav_group:
                        loadUrl(telegramGroup);
                        break;
                    case R.id.nav_dev:
                        loadUrl(Constants.DEV_URL);
                        break;
                }
                return true;
            }
        });

        drawerLayout = findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            @Override
            public void onDrawerClosed(View v) {
                super.onDrawerClosed(v);
            }

            @Override
            public void onDrawerOpened(View v) {
                super.onDrawerOpened(v);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    public boolean vpn() {
        String iface = "";
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isUp())
                    iface = networkInterface.getName();
                if ( iface.contains("tun") || iface.contains("ppp") || iface.contains("pptp")) {
                    return true;
                }
            }
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            System.out.println(url);
            connectionErrorDialogShown = false;
            checkNetworkAndVPN();
            if (url.contains(Uri.parse(Constants.APP_ROOT).getHost())) {
                view.loadUrl(url);
                return false;
            }
            loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView webview, String url, Bitmap favicon) {
            super.onPageStarted(webview, url, favicon);
            checkNetworkAndVPN();
            spotsDialog.show();
            if (!connectionErrorDialogShown) {
                webView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            spotsDialog.dismiss();
        }

        @Override
        public void onLoadResource(WebView view, String url) {
//            System.out.println(url);
            super.onLoadResource(view, url);
            checkNetworkAndVPN();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
//            System.out.println(request);
//            webView.loadUrl("about:blank");
//            webView.setVisibility(View.INVISIBLE);
//            showConnectionErrorDialog();
//            try {
//                view.stopLoading();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            try {
//                view.clearView();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            super.onReceivedError(view, request, error);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String resourceUrl = request.getUrl().toString();
//            System.out.println(resourceUrl);
            if (resourceUrl.contains("fonts/")) {
                try {
                    String fontFileName = resourceUrl.split("fonts/")[1];
                    return new WebResourceResponse(
                            URLConnection.guessContentTypeFromName(request.getUrl().getPath()),
                            "utf-8",
                            MainActivity.this.getAssets().open("webfonts/" + fontFileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return super.shouldInterceptRequest(view, request);
        }

    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @SuppressLint("HardwareIds")
        @JavascriptInterface
        public String getDeviceId() {
            return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        @JavascriptInterface
        public void setUserAccount(String user) {
            Helper.setUserAccount(MainActivity.this, user);
        }

        @JavascriptInterface
        public void openWebActivity(String activityName, String viewLocation) {
            startActivity(new Intent(MainActivity.this, WebActivity.class)
                    .putExtra("activityName", activityName)
                    .putExtra("viewLocation", viewLocation)
            );
        }

        @JavascriptInterface
        public void taskActivity() {
            startActivity(new Intent(MainActivity.this, TaskActivity.class));
        }

        @JavascriptInterface
        public void exitApp() {
            exitAlert();
        }

        @JavascriptInterface
        public boolean is_vpn_connected() {
            return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN).isConnectedOrConnecting();
        }

        @JavascriptInterface
        public void loadUrl(String url) {
            loadUrl(url);
        }

        @JavascriptInterface
        public void sweetAlert(String dialogMessage, String dialogType, Boolean finishActivity) {
            sweetAlertDialog(dialogMessage, dialogType, false);
        }

        @JavascriptInterface
        public void successAlert(String message, Boolean finish) {
            sweetAlertDialog(message, "", false);
        }

        @JavascriptInterface
        public void errorAlert(String message, Boolean finish) {
            sweetAlertDialog(message, "WARNING_TYPE", false);
        }

        @JavascriptInterface
        public void dieAlert(String dialogMessage) {
            sweetAlertDialog(dialogMessage, "WARNING_TYPE", true);
        }

        @JavascriptInterface
        public void blockAlert(int type) {
            blockAlertDialog(type);
        }
    }

    private void Toast(String ToastString) {
        Toast.makeText(getApplicationContext(), ToastString, Toast.LENGTH_SHORT).show();
    }

    private void sweetAlertDialog(String dialogMessage, String dialogType, final Boolean finishActivity) {
        SweetAlertDialog sweetAlert = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        sweetAlert.setTitleText(dialogMessage);
        sweetAlert.setCancelable(false);
        if (dialogType == "WARNING_TYPE") {
            sweetAlert.changeAlertType(SweetAlertDialog.WARNING_TYPE);
        }
        sweetAlert.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlert) {
                if (finishActivity) {
                    finish();
                } else {
                    sweetAlert.dismiss();
                }
            }
        });
        sweetAlert.show();
    }

    public void loadUrl(String mUrl) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(mUrl));
        startActivity(i);
    }

    public void exitAlert() {
        SweetAlertDialog sweetAlert = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        sweetAlert.setTitleText("Do you want to exit?");
        sweetAlert.setCancelable(true);
        sweetAlert.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlert) {
                finish();
            }
        });
        sweetAlert.show();
    }

    public void openWebActivity(String activityName, String viewLocation) {
        startActivity(new Intent(MainActivity.this, WebActivity.class)
                .putExtra("activityName", activityName)
                .putExtra("viewLocation", viewLocation)
        );
    }

    public void checkNetworkAndVPN() {

        if (activeNetwork != null && activeNetwork.isConnected()) {

        } else {
            showConnectionErrorDialog();
            return;
        }
    }

    public Boolean is_VPN_connected() {
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN).isConnectedOrConnecting();
    }

    public void showConnectionErrorDialog() {
        if (!connectionErrorDialogShown) {
            webView.loadUrl("about:blank");
            sweetAlertDialog("Connection Problem!", "WARNING_TYPE", true);
            connectionErrorDialogShown = true;
        }
    }

    public void fetchAppData() {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, Constants.APP_DATA_API_URL
                + "?user=" + Helper.getUserAccount(this)
                + "&did=" + Helper.getDeviceId(this)
                + "&" + Constants.EXTRA_PARAMS,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject data = new JSONObject(response);


                            if (!data.getBoolean("success")) {
                                sweetAlertDialog(data.getString("message"), "WARNING_TYPE", true);
                                return;
                            }

                            if (Constants.APP_VERSION < data.getInt("app_version")) {
                                updateApp(data.getString("app_url"));
                                return;
                            }

                            if (!data.isNull("vpn_required")) {
                                if (!data.getBoolean("vpn_required") && is_VPN_connected()) {
                                    sweetAlertDialog("VPN not allowed", "WARNING_TYPE", true);
                                    return;
                                }

                                if (data.getBoolean("vpn_required") && !is_VPN_connected()) {
                                    sweetAlertDialog("Connect VPN", "WARNING_TYPE", true);
                                    return;
                                }
                            }

                            if (data != null) {

                                telegramChannel = data.getString("tg_channel");
                                telegramGroup = data.getString("support_group");
                                Helper.setHomeAdUnit(MainActivity.this, data.getString("ban_ad"));

                                String startAppAdsCode = data.getString("startapp_ads_code");
                                String spin_waitingTime = data.getString("spin_waiting_time");

                                String spin_limit = data.getString("spin_limit");

                                String primary_color = data.getString("primary_color");
                                String gradient_color_end = data.getString("gradient_color_end");

                                String daily_task_limit = data.getString("daily_task_limit");
                                String spin_control = data.getString("spin_control");
                                App_Controller app_controller = new App_Controller(MainActivity.this);
                                app_controller.dataStore(spin_limit, daily_task_limit, spin_control, startAppAdsCode, spin_waitingTime);
                                app_controller.setColorCode(primary_color, gradient_color_end);

                                if (primary_color != null){
                                    changeColor();
                                }
                            }


                        } catch (Exception e) {
                            Toast(e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showConnectionErrorDialog();
                return;
            }
        });
        queue.add(stringRequest);
    }

    public void updateApp(final String url) {
        SweetAlertDialog sweetAlert = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        sweetAlert.setTitleText("Update Required!");
        sweetAlert.setConfirmText("Update");
        sweetAlert.setCancelable(false);
        sweetAlert.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlert) {
                loadUrl(url);
                finish();
            }
        });
        sweetAlert.show();
    }

    public void blockAlertDialog(final int type) {
        SweetAlertDialog sweetAlert = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        sweetAlert.setTitleText("Account Blocked");
        sweetAlert.setConfirmText("Ok");
        sweetAlert.setCancelable(false);
        if (type == 2) {
            sweetAlert.setConfirmText("Contact Us");
        }
        sweetAlert.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlert) {
                if (type == 2) {
                    loadUrl(telegramGroup);
                }
                finish();
            }
        });
        sweetAlert.show();
    }

    @Override
    public void onBackPressed() {
        exitAlert();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieSyncManager.getInstance().sync();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}