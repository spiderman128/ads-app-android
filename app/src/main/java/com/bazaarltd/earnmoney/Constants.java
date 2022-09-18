package com.bazaarltd.earnmoney;

public class Constants
{
    public static final String APP_ID = "";

    public static final int APP_VERSION = 1;
    public static final Boolean SHOW_DRAWER = true;
    public static final String DEV_URL = "https://click-pay-0.flycricket.io/privacy.html";
    /*  public static final String DEV_URL = "https://t.me/Resource_Lab_App_Debloper";*/

    public static final String EXTRA_PARAMS = "";
    public static final String TASK_APP_INDEX = "0";
    public static final String HOME_PAGE = "index.php";
    public static final String HOST_DOMAIN = "mybdfun24.000webhostapp.com";

    public static final String BASE_URL = "https://" + HOST_DOMAIN + "/" + APP_ID;
    public static final String APP_ROOT = BASE_URL + "/app";
    public static final String HOME_URL = APP_ROOT + "/" + HOME_PAGE;

    public static final String APP_DATA_API_URL = APP_ROOT + "/api/app-data.php";
    public static final String TASK_DATA_API_URL = APP_ROOT + "/api/task-data.php";
    public static final String TASK_AUTH_API_URL = APP_ROOT + "/api/task-authh.php";
    public static final String INVALID_CLICK_API_URL = APP_ROOT + "/api/invalid.php";
}


