package com.sbugert.rnadmob;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
 
import java.util.Date;
import java.util.GregorianCalendar;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class RNAdMobInterstitialAdModule extends ReactContextBaseJavaModule {
  public static final String TAG = "RNAdMobInterstitial";
  InterstitialAd mInterstitialAd;
  String adUnitID;
  String testDeviceID;
  Callback requestAdCallback;
  Callback showAdCallback;
  private String gender;
  private Date birthday;

  @Override
  public String getName() {
    return "RNAdMobInterstitial";
  }

  public RNAdMobInterstitialAdModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mInterstitialAd = new InterstitialAd(reactContext);

    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        mInterstitialAd.setAdListener(new AdListener() {
          @Override
          public void onAdClosed() {
            sendEvent("interstitialDidClose", null);
            showAdCallback.invoke();
          }
          @Override
          public void onAdFailedToLoad(int errorCode) {
            WritableMap event = Arguments.createMap();
            String errorString = null;
            switch (errorCode) {
              case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                errorString = "ERROR_CODE_INTERNAL_ERROR";
                break;
              case AdRequest.ERROR_CODE_INVALID_REQUEST:
                errorString = "ERROR_CODE_INVALID_REQUEST";
                break;
              case AdRequest.ERROR_CODE_NETWORK_ERROR:
                errorString = "ERROR_CODE_NETWORK_ERROR";
                break;
              case AdRequest.ERROR_CODE_NO_FILL:
                errorString = "ERROR_CODE_NO_FILL";
                break;
            }
            event.putString("error", errorString);
            sendEvent("interstitialDidFailToLoad", event);
            requestAdCallback.invoke(errorString);
          }
          @Override
          public void onAdLeftApplication() {
            sendEvent("interstitialWillLeaveApplication", null);
          }
          @Override
          public void onAdLoaded() {
            sendEvent("interstitialDidLoad", null);
            requestAdCallback.invoke();
          }
          @Override
          public void onAdOpened() {
            sendEvent("interstitialDidOpen", null);
          }
        });
      }
    });
  }
  private void sendEvent(String eventName, @Nullable WritableMap params) {
    getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
  }

  @ReactMethod
  public void setAdUnitID(String adUnitID) {
    mInterstitialAd.setAdUnitId(adUnitID);
  }

  @ReactMethod
  public void setGender(String gender) {
    this.gender = gender;
  }


  @ReactMethod
  public void setBirthday(final ReadableMap birthday) {
      this.birthday = new GregorianCalendar(birthday.getInt("year"), birthday.getInt("month"), birthday.getInt("date")).getTime();
  }

  @ReactMethod
  public void setTestDeviceID(String testDeviceID) {
    this.testDeviceID = testDeviceID;
  }

  @ReactMethod
  public void requestAd(final Callback callback) {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run () {
        if (mInterstitialAd.isLoaded() || mInterstitialAd.isLoading()) {
          callback.invoke("Ad is already loaded."); // TODO: make proper error
        } else {
          requestAdCallback = callback;
          AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
          if (testDeviceID != null){
            if (testDeviceID.equals("EMULATOR")) {
              adRequestBuilder = adRequestBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
            } else {
              adRequestBuilder = adRequestBuilder.addTestDevice(testDeviceID);
            }
          }

        if ("female".equals(gender)) {
          adRequestBuilder = adRequestBuilder.setGender(AdRequest.GENDER_FEMALE);
          Log.d(TAG, "set Gender=female");
        } else if ("male".equals(gender)) {
          adRequestBuilder = adRequestBuilder.setGender(AdRequest.GENDER_MALE);
          Log.d(TAG, "set Gender=male");
        }

        if (birthday != null) {
          adRequestBuilder = adRequestBuilder.setBirthday(birthday);
          Log.d(TAG, "set birthday=" + birthday.toString());
        }

          AdRequest adRequest = adRequestBuilder.build();
          mInterstitialAd.loadAd(adRequest);
        }
      }
    });
  }

  @ReactMethod
  public void showAd(final Callback callback) {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run () {
        if (mInterstitialAd.isLoaded()) {
          showAdCallback = callback;
          mInterstitialAd.show();
        } else {
          callback.invoke("Ad is not ready."); // TODO: make proper error
        }
      }
    });
  }

  @ReactMethod
  public void isReady(final Callback callback) {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run () {
        callback.invoke(mInterstitialAd.isLoaded());
      }
    });
  }
}
