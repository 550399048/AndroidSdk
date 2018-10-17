# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep class com.wppai.adsdk.nativ.NativeAd { *;}
-keep public interface com.wppai.adsdk.nativ.NativeAd$NativeAdListener { *;}
-keep public interface com.wppai.adsdk.nativ.NativeAdDataRef{ *;}
-keep class com.wppai.adsdk.nativ.NativeAdData { *;}
-keep class com.wppai.adsdk.splash.SplashAd { *;}
-keep public interface com.wppai.adsdk.splash.SplashAd$SplashAdListener { *;}
-keep class com.wppai.adsdk.interstitial.IntersititialAd { *;}
-keep public interface com.wppai.adsdk.interstitial.IntersititialAd$IntersititialAdListener { *;}
-keep class com.wppai.adsdk.banner.BannerAdView { *;}
-keep public interface com.wppai.adsdk.banner.BannerAdView$BannerAdListener { *;}

-dontwarn android.support.**
-dontwarn com.squareup.okhttp.**
-dontwarn okhttp3.**
-dontwarn com.liulishuo.okdownload.**

#TT
-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.androidquery.callback.** {*;}
-keep class com.bytedance.sdk.openadsdk.service.TTDownloadProvider

#GDT
-keep class com.qq.e.** {
    public protected *;
}
-keep class android.support.v4.**{
    public *;
}
-keep class android.support.v7.**{
    public *;
}

#baidu
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.baidu.mobads.*.** { *; }