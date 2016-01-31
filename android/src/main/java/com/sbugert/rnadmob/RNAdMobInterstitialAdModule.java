package com.sbugert.rnadmob;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

/**
 * Created by andri on 16/01/2016.
 */
public class RNAdMobInterstitialAdModule extends ReactContextBaseJavaModule {
  public static final String TAG = "RNAdMob";
  InterstitialAd mInterstitialAd;
  String testDeviceId;

  public RNAdMobInterstitialAdModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return "RNAdMobInterstitial";
  }

  @ReactMethod
  public void init(final String adUnitID, final String testDeviceId) {
    mInterstitialAd = new InterstitialAd(getReactApplicationContext());
    this.testDeviceId = testDeviceId;

    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Setting adUnitId:" + adUnitID);
        mInterstitialAd.setAdUnitId(adUnitID);

        mInterstitialAd.setAdListener(new AdListener() {
          @Override
          public void onAdClosed() {
            // requestNewInterstitial(testDeviceId);
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
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        if (testDeviceId != null) {
          adRequestBuilder = adRequestBuilder.addTestDevice(testDeviceId);
          Log.d(TAG, "Setting testDeviceId:" + testDeviceId);
        }

        AdRequest adRequest = adRequestBuilder.build();
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
