package com.wppai.adsdk.comm;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class SystemUtil {
    public final static String TAG = "SystemUtil";

    /**
     * 获取当前手机系统版本号
     *
     * @return 系统版本号
     */
    public static String getSystemVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * 获取手机型号
     *
     * @return 手机型号
     */
    public static String getSystemModel() {
        return android.os.Build.MODEL;
    }

    /**
     * 获取手机厂商
     *
     * @return 手机厂商
     */
    public static String getDeviceBrand() {
        return android.os.Build.BRAND;
    }

    /**
     * 获取当前手机设备厂商
     *
     * @return 手机设备厂商
     */
    public static String getSystemManufacturer() {
        return android.os.Build.MANUFACTURER;
    }

    /**
     * 获取手机IMEI(需要“android.permission.READ_PHONE_STATE”权限)
     *
     * @return 手机IMEI
     */
    public static String getIMEI(Context ctx) {
        int permissionCheck = ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.READ_PHONE_STATE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Activity.TELEPHONY_SERVICE);
            if (tm != null) {
                return tm.getDeviceId();
            }
        } else {
            Log.w(TAG, "no permission.READ_PHONE_STATE");
            return "unknown";
        }
        return "unknown";
    }

    /**
     * 获取当前手机android id
     *
     * @return 手机android id
     */
    public static String getAndroidId(Context ctx) {
        return Settings.System.getString(ctx.getContentResolver(), Settings.System.ANDROID_ID);
    }

    /**
     * 获取当前应用包名
     *
     * @return 应用包名
     */
    public static String getPackageName(Context ctx) {
        return ctx.getPackageName();
    }

    //版本名
    public static String getAppVersion(Context context) {
        return getPackageInfo(context).versionName;
    }

    //版本号
    public static int getVersionCode(Context context) {
        return getPackageInfo(context).versionCode;
    }

    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo pi = null;

        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);

            return pi;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pi;
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        return width;
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int heigth = dm.heightPixels;
        return heigth;
    }

    public static double getScreenDensity(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        double density = dm.density;
        NumberFormat nf=new DecimalFormat( "0.0 ");
        density = Double.parseDouble(nf.format(density));
        return density;
    }


    private static final int NETWORK_TYPE_UNKNOWN = 0;
    private static final int NETWORK_TYPE_WIFI = 1;
    private static final int NETWORK_TYPE_2G = 2;
    private static final int NETWORK_TYPE_3G = 3;
    private static final int NETWORK_TYPE_4G = 4;

    private static final int NETWORK_CARRIER_UNKNOWN = 0;
    private static final int NETWORK_CARRIER_CMCC = 1;
    private static final int NETWORK_CARRIER_CUCC = 2;
    private static final int NETWORK_CARRIER_CTCC = 3;

    /**
     * 获取网络状态
     */
    public static int getNetworkType(Context context) {
        int strNetworkType = 0;

        NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (networkInfo != null) {
            if (networkInfo.isConnected()) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    strNetworkType = NETWORK_TYPE_WIFI;
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    int networkClass = 0;
                    Class telephonyManagerClass = telephonyManager.getClass();
                    Method getNetworkClassMethod = null;
                    try {
                        Method getDataNetworkTypeMethod = telephonyManagerClass
                                .getMethod("getDataNetworkType", new Class[]{int.class});
                        Class subscriptionManagerClazz = Class.forName("android.telephony.SubscriptionManager");
                        Method getDefaultDataSubIdMethod = subscriptionManagerClazz
                                .getMethod("getDefaultDataSubId", new Class[]{int.class});
                        int defaultDataSubId = (int) getDefaultDataSubIdMethod.invoke(null, new Object[]{});
                        int dataNetworkType = (int) getDataNetworkTypeMethod.invoke(telephonyManager, defaultDataSubId);
                        getNetworkClassMethod = telephonyManagerClass.getMethod("getNetworkClass", new Class[]{int.class});
                        networkClass = (int) getNetworkClassMethod.invoke(telephonyManager, dataNetworkType);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    switch (networkClass) {
                        case 1:
                            strNetworkType = NETWORK_TYPE_2G;
                            break;
                        case 2:
                            strNetworkType = NETWORK_TYPE_3G;
                            break;
                        case 3:
                            strNetworkType = NETWORK_TYPE_4G;
                            break;
                        case 0:
                            strNetworkType = NETWORK_TYPE_UNKNOWN;
                            break;
                        default:
                            strNetworkType = NETWORK_TYPE_UNKNOWN;
                            break;
                    }
                }
            } else {
                strNetworkType = NETWORK_TYPE_UNKNOWN;
            }
        } else {
            strNetworkType = NETWORK_TYPE_UNKNOWN;
        }
        return strNetworkType;
    }

    /**
     * 获取运营商状态
     */
    public static int getNetworkCarrier(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String networkOperator = telephonyManager.getNetworkOperator();
        if (networkOperator == null) {
            return NETWORK_CARRIER_UNKNOWN;
        }
        switch (telephonyManager.getNetworkOperator()) {
            case "46000":
            case "46002":
            case "46007":
                return NETWORK_CARRIER_CMCC;
            case "46001":
            case "46006":
                return NETWORK_CARRIER_CUCC;
            case "46003":
            case "46005":
            case "46011":
                return NETWORK_CARRIER_CTCC;
            default:
                return NETWORK_CARRIER_UNKNOWN;
        }
    }

    /**
     * 手机是否安装app
     */
    public static boolean isInstalled(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return false;
        }

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            if (pi != null) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Ignore
        }

        return false;
    }
}
