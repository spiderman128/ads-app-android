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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bazaarltd.earnmoney.utils.Helper;
import com.bazaarltd.earnmoney.utils.TinyDB;
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog;

import java.io.IOException;
import java.net.URLConnection;

import dmax.dialog.SpotsDialog;

public class WebActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SpotsDialog spotsDialog;
    private TinyDB tinydb;

    private ConnectivityManager connectivityManager;
    private NetworkInfo activeNetwork;
    private WebView webView;
    private boolean connectionErrorDialogShown = false;
    App_Controller app_controller;

    @SuppressLint("AddJavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        app_controller = new App_Controller(this);

        toolbar.setBackgroundColor(Color.parseColor(app_controller.getColorCode1()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(app_controller.getColorCode1()));
        }


        String activityName = getIntent().getStringExtra("activityName");
        String viewLocation = getIntent().getStringExtra("viewLocation");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(activityName);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        spotsDialog = new SpotsDialog(WebActivity.this, R.style.SpotsDialog);
        spotsDialog.setCancelable(false);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = connectivityManager.getActiveNetworkInfo();

        tinydb = new TinyDB(this);
        webView = findViewById(R.id.webView);

        webView.addJavascriptInterface(new WebActivity.WebAppInterface(this), "Android");
        webView.setWebViewClient(new WebActivity.MyBrowser());
        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else {
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
            webView.loadUrl(Constants.APP_ROOT + "/" + viewLocation + ".php"
                    + "?user=" + Helper.getUserAccount(this)
                    + "&did=" + Helper.getDeviceId(this)
                    + "&" + Constants.EXTRA_PARAMS);
        }
    }

    private class MyBrowser extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            connectionErrorDialogShown = false;
            checkNetworkAndVPN();
            if (url.contains(Uri.parse(Constants.APP_ROOT).getHost()))
            {
                view.loadUrl(url);
                return false;
            }
            loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView webview, String url, Bitmap favicon)
        {
            super.onPageStarted(webview, url, favicon);
            checkNetworkAndVPN();
            spotsDialog.show();
            if (!connectionErrorDialogShown) {
                webView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);
            spotsDialog.dismiss();
        }

        @Override
        public void onLoadResource(WebView view, String url)
        {
            super.onLoadResource(view, url);
            checkNetworkAndVPN();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)
        {
            webView.loadUrl("about:blank");
            webView.setVisibility(View.INVISIBLE);
            showConnectionErrorDialog();
            try {
                view.stopLoading();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                view.clearView();
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.onReceivedError(view, request, error);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String resourceUrl = request.getUrl().toString();
            if (resourceUrl.contains("fonts/")) {
                try {
                    String fontFileName = resourceUrl.split("fonts/")[1];
                    return new WebResourceResponse(
                            URLConnection.guessContentTypeFromName(request.getUrl().getPath()),
                            "utf-8",
                            WebActivity.this.getAssets().open("webfonts/" + fontFileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return super.shouldInterceptRequest(view,request);
        }

    }

    public class WebAppInterface
    {
        Context mContext;
        WebAppInterface(Context c)
        {
            mContext = c;
        }

        @JavascriptInterface
        public String getDeviceId()
        {
            return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        @JavascriptInterface
        public void setUserAccount(String user) {
            Helper.setUserAccount(WebActivity.this, user);
        }

        @JavascriptInterface
        public void openWebActivity(String activityName, String viewLocation)
        {
            startActivity(new Intent(WebActivity.this, WebActivity.class)
                    .putExtra("activityName", activityName)
                    .putExtra("viewLocation", viewLocation)
            );
        }

        @JavascriptInterface
        public void taskActivity()
        {
            startActivity(new Intent(WebActivity.this, TaskActivity.class));
        }

        @JavascriptInterface
        public void exitApp()
        {
            System.exit(1);
        }

        @JavascriptInterface
        public boolean is_vpn_connected() {
            return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN).isConnectedOrConnecting();
        }

        @JavascriptInterface
        public void loadUrl(String url)
        {
            loadUrl(url);
        }

        @JavascriptInterface
        public void sweetAlert(String dialogMessage, String dialogType, Boolean finishActivity)
        {
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
        public void dieAlert(String dialogMessage)
        {
            sweetAlertDialog(dialogMessage, "WARNING_TYPE", true);
        }

        @JavascriptInterface
        public void blockAlert(int type)
        {
            blockAlertDialog(type);
        }
    }

    private void Toast(String ToastString)
    {
        Toast.makeText(getApplicationContext(), ToastString, Toast.LENGTH_SHORT).show();
    }

    private void sweetAlertDialog(String dialogMessage, String dialogType, final Boolean finishActivity)
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

    public void checkNetworkAndVPN()
    {

        if (activeNetwork != null && activeNetwork.isConnected())
        {

        }
        else
        {
            showConnectionErrorDialog();
            return;
        }
    }

    public Boolean is_VPN_connected()
    {
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN).isConnectedOrConnecting();
    }

    public void showConnectionErrorDialog() {
        if (!connectionErrorDialogShown)
        {
            webView.loadUrl("about:blank");
            sweetAlertDialog("Connection Problem!", "WARNING_TYPE", true);
            connectionErrorDialogShown = true;
        }
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
            public void onClick(SweetAlertDialog sweetAlert)
            {
                System.exit(1);
            }
        });
        sweetAlert.show();
    }
    @Override
    public void onBackPressed()
    {
        finish();
    }
}
