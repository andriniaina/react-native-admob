
package com.sbugert.rnadmob;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by andri on 16/01/2016.
 */
public class RNAdMobInterstitialAdModule extends ReactContextBaseJavaModule {
  public static final String TAG = "RNAdMobInterstitial";
  private final Activity activity;
  InterstitialAd mInterstitialAd;
  String testDeviceId;
  private String gender;
  private Date birthday;

  public RNAdMobInterstitialAdModule(ReactApplicationContext reactContext, Activity activity) {
    super(reactContext);
    this.activity = activity;
  }

  @Override
  public String getName() {
    return "RNAdMobInterstitial";
  }

  @ReactMethod
  public void init(final String adUnitID, final String testDeviceId, final String gender, final ReadableMap birthday) {
    mInterstitialAd = new InterstitialAd(this.activity);
    this.testDeviceId = testDeviceId;
    this.gender = gender;
    if (birthday != null)
      this.birthday = new GregorianCalendar(birthday.getInt("year"), birthday.getInt("month"), birthday.getInt("date")).getTime();

    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Setting adUnitId:" + adUnitID);
        mInterstitialAd.setAdUnitId(adUnitID);

        mInterstitialAd.setAdListener(new AdListener() {
          @Override
          public void onAdClosed() {
            sendEvent(getReactApplicationContext(), "onAdInterstitialClosed");
          }
        });
      }
    });
  }

  @ReactMethod
  public void requestNewInterstitial() {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        AdRequest.Builder builder = new AdRequest.Builder();
        if (testDeviceId != null) {
          builder = builder.addTestDevice(testDeviceId);
          Log.d(TAG, "Setting testDeviceId:" + testDeviceId);
        }

        if ("female".equals(gender)) {
          builder = builder.setGender(AdRequest.GENDER_FEMALE);
          Log.d(TAG, "set Gender=female");
        } else if ("male".equals(gender)) {
          builder = builder.setGender(AdRequest.GENDER_MALE);
          Log.d(TAG, "set Gender=male");
        }

        if (birthday != null) {
          builder = builder.setBirthday(birthday);
          Log.d(TAG, "set birthday=" + birthday.toString());
        }

        AdRequest adRequest = builder.build();
        mInterstitialAd.loadAd(adRequest);
        Log.d(TAG, "loadAd started");
      }
    });
  }

  @ReactMethod
  public void show() {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        if (mInterstitialAd.isLoaded())
          mInterstitialAd.show();
        else
          Log.w(TAG, "Interstitial is not loaded yet!");
      }
    });
  }

  private void sendEvent(ReactContext reactContext, String eventName) {
    reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, Arguments.createMap());
  }
}

