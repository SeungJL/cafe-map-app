package com.about.studyaboutclubapp;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class IntentModule extends ReactContextBaseJavaModule {

  public IntentModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @NonNull
  @Override
  public String getName() {
    return "IntentModule";
  }

  @ReactMethod
  public void openIntent(String url, Promise promise) {
    try {
      if (url == null || url.length() == 0) {
        promise.resolve(false);
        return;
      }

      if (getCurrentActivity() == null) {
        promise.reject("NO_ACTIVITY", "No current activity");
        return;
      }

      // ✅ intent://... 를 진짜 Android Intent로 파싱/실행
      Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

      if (intent.resolveActivity(getCurrentActivity().getPackageManager()) != null) {
        getCurrentActivity().startActivity(intent);
        promise.resolve(true);
        return;
      }

      // ✅ browser_fallback_url이 있으면 웹으로
      String fallbackUrl = intent.getStringExtra("browser_fallback_url");
      if (fallbackUrl != null && fallbackUrl.length() > 0) {
        Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
        getCurrentActivity().startActivity(fallbackIntent);
        promise.resolve(true);
        return;
      }

      // ✅ package가 있으면 스토어로
      String pkg = intent.getPackage();
      if (pkg != null && pkg.length() > 0) {
        Uri market = Uri.parse("market://details?id=" + pkg);
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, market);
        getCurrentActivity().startActivity(marketIntent);
        promise.resolve(true);
        return;
      }

      promise.resolve(false);
    } catch (Exception e) {
      promise.reject("INTENT_ERROR", e);
    }
  }
}
