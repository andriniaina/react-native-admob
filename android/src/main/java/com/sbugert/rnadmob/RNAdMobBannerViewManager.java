package com.sbugert.rnadmob;

import android.app.Activity;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ReactProp;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RNAdMobBannerViewManager extends SimpleViewManager<ReactViewGroup> {

  public static final String TAG = "RNAdMob";
  public static final String REACT_CLASS = "RNAdMob";

  public static final String PROP_BANNER_SIZE = "bannerSize";
  public static final String PROP_AD_UNIT_ID = "adUnitID";
  public static final String PROP_AD_TARGETING_LOCATION = "location";
  public static final String PROP_AD_TARGETING_GENDER = "gender";
  public static final String PROP_AD_TARGETING_BIRTHDAY = "birthday";
  private String gender = null;
  private Date birthday;
  private String location;

  public enum Events {
    EVENT_SIZE_CHANGE("onSizeChange"),
    EVENT_RECEIVE_AD("onAdViewDidReceiveAd"),
    EVENT_ERROR("onDidFailToReceiveAdWithError"),
    EVENT_WILL_PRESENT("onAdViewWillPresentScreen"),
    EVENT_WILL_DISMISS("onAdViewWillDismissScreen"),
    EVENT_DID_DISMISS("onAdViewDidDismissScreen"),
    EVENT_WILL_LEAVE_APP("onAdViewWillLeaveApplication");

    private final String mName;

    Events(final String name) {
      mName = name;
    }

    @Override
    public String toString() {
      return mName;
    }
  }

  private ThemedReactContext mThemedReactContext;
  private RCTEventEmitter mEventEmitter;
  private Activity activity;

  public RNAdMobBannerViewManager(Activity activity) {
    this.activity = activity;
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  protected ReactViewGroup createViewInstance(ThemedReactContext themedReactContext) {
    mThemedReactContext = themedReactContext;
    mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
    ReactViewGroup view = new ReactViewGroup(themedReactContext);
    attachNewAdView(view);
    return view;
  }

  protected void attachNewAdView(final ReactViewGroup view) {
    final AdView adView = new AdView(activity);

    // destroy old AdView if present
    AdView oldAdView = (AdView) view.getChildAt(0);
    view.removeAllViews();
    if (oldAdView != null) oldAdView.destroy();
    view.addView(adView);
    attachEvents(view);
  }

  protected void attachEvents(final ReactViewGroup view) {
    final AdView adView = (AdView) view.getChildAt(0);
    adView.setAdListener(new AdListener() {
      @Override
      public void onAdLoaded() {
        int width = adView.getAdSize().getWidthInPixels(mThemedReactContext);
        int height = adView.getAdSize().getHeightInPixels(mThemedReactContext);
        int left = adView.getLeft();
        int top = adView.getTop();
        adView.measure(width, height);
        adView.layout(left, top, left + width, top + height);
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_RECEIVE_AD.toString(), null);
      }

      @Override
      public void onAdFailedToLoad(int errorCode) {
        WritableMap event = Arguments.createMap();
        switch (errorCode) {
          case AdRequest.ERROR_CODE_INTERNAL_ERROR:
            event.putString("error", "ERROR_CODE_INTERNAL_ERROR");
            break;
          case AdRequest.ERROR_CODE_INVALID_REQUEST:
            event.putString("error", "ERROR_CODE_INVALID_REQUEST");
            break;
          case AdRequest.ERROR_CODE_NETWORK_ERROR:
            event.putString("error", "ERROR_CODE_NETWORK_ERROR");
            break;
          case AdRequest.ERROR_CODE_NO_FILL:
            event.putString("error", "ERROR_CODE_NO_FILL");
            break;
        }

        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_ERROR.toString(), event);
      }

      @Override
      public void onAdOpened() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_PRESENT.toString(), null);
      }

      @Override
      public void onAdClosed() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_DISMISS.toString(), null);
      }

      @Override
      public void onAdLeftApplication() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_LEAVE_APP.toString(), null);
      }
    });
  }

  @Override
  @Nullable
  public Map getExportedCustomDirectEventTypeConstants() {
    MapBuilder.Builder builder = MapBuilder.builder();
    for (Events event : Events.values()) {
      builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
    }
    return builder.build();
  }

  @ReactProp(name = PROP_BANNER_SIZE)
  public void setBannerSize(final ReactViewGroup view, final String sizeString) {
    AdSize adSize = getAdSizeFromString(sizeString);

    // store old ad unit ID (even if not yet present and thus null)
    AdView oldAdView = (AdView) view.getChildAt(0);
    String adUnitId = oldAdView.getAdUnitId();

    attachNewAdView(view);
    AdView newAdView = (AdView) view.getChildAt(0);
    newAdView.setAdSize(adSize);
    newAdView.setAdUnitId(adUnitId);

    // send measurements to js to style the AdView in react
    int width;
    int height;
    WritableMap event = Arguments.createMap();
    if (adSize == AdSize.SMART_BANNER) {
      width = (int) PixelUtil.toDIPFromPixel(adSize.getWidthInPixels(mThemedReactContext));
      height = (int) PixelUtil.toDIPFromPixel(adSize.getHeightInPixels(mThemedReactContext));
    } else {
      width = adSize.getWidth();
      height = adSize.getHeight();
    }
    event.putDouble("width", width);
    event.putDouble("height", height);
    mEventEmitter.receiveEvent(view.getId(), Events.EVENT_SIZE_CHANGE.toString(), event);

    loadAd(newAdView);
  }

  @ReactProp(name = PROP_AD_UNIT_ID)
  public void setAdUnitID(final ReactViewGroup view, final String adUnitID) {
    // store old banner size (even if not yet present and thus null)
    AdView oldAdView = (AdView) view.getChildAt(0);
    AdSize adSize = oldAdView.getAdSize();

    attachNewAdView(view);
    AdView newAdView = (AdView) view.getChildAt(0);
    newAdView.setAdUnitId(adUnitID);
    newAdView.setAdSize(adSize);
    loadAd(newAdView);
  }

  @ReactProp(name = PROP_AD_TARGETING_GENDER)
  public void setGender(final ReactViewGroup view, final String gender) {
    AdView oldAdView = (AdView) view.getChildAt(0);
    AdSize adSize = oldAdView.getAdSize();
    String adUnitID = oldAdView.getAdUnitId();

    attachNewAdView(view);
    AdView newAdView = (AdView) view.getChildAt(0);
    newAdView.setAdUnitId(adUnitID);
    newAdView.setAdSize(adSize);

    this.gender = gender;
    loadAd(newAdView);
  }

  @ReactProp(name = PROP_AD_TARGETING_LOCATION)
  public void setLocation(final ReactViewGroup view, final String location) {
    AdView oldAdView = (AdView) view.getChildAt(0);
    AdSize adSize = oldAdView.getAdSize();
    String adUnitID = oldAdView.getAdUnitId();

    attachNewAdView(view);
    AdView newAdView = (AdView) view.getChildAt(0);
    newAdView.setAdUnitId(adUnitID);
    newAdView.setAdSize(adSize);

    this.location = location;
    loadAd(newAdView);
  }

  @ReactProp(name = PROP_AD_TARGETING_BIRTHDAY)
  public void setBirthday(final ReactViewGroup view, final ReadableMap birthday) {
    AdView oldAdView = (AdView) view.getChildAt(0);
    if (birthday != null)
      this.birthday = new GregorianCalendar(birthday.getInt("year"), birthday.getInt("month"), birthday.getInt("date")).getTime();
    AdSize adSize = oldAdView.getAdSize();
    String adUnitID = oldAdView.getAdUnitId();

    attachNewAdView(view);
    AdView newAdView = (AdView) view.getChildAt(0);
    newAdView.setAdUnitId(adUnitID);
    newAdView.setAdSize(adSize);

    loadAd(newAdView);
  }

  private void loadAd(final AdView adView) {
    if (adView.getAdSize() != null && adView.getAdUnitId() != null) {
      AdRequest.Builder builder = new AdRequest.Builder();

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

      if (this.location != null)
        try {
          String[] parts = location.split(",");
          double latitude = Double.parseDouble(parts[0]);
          double longitude = Double.parseDouble(parts[1]);
          Location location = new Location("");
          location.setLatitude(latitude);
          location.setLongitude(longitude);
          builder = builder.setLocation(location);
          Log.d(TAG, "set location=" + location.toString());
        } catch (Exception ex) {
          Log.e(TAG, ex.toString());
        }

      AdRequest adRequest = builder.build();
      adView.loadAd(adRequest);
    }
  }


  private AdSize getAdSizeFromString(String adSize) {
    switch (adSize) {
      case "banner":
        return AdSize.BANNER;
      case "largeBanner":
        return AdSize.LARGE_BANNER;
      case "mediumRectangle":
        return AdSize.MEDIUM_RECTANGLE;
      case "fullBanner":
        return AdSize.FULL_BANNER;
      case "leaderBoard":
        return AdSize.LEADERBOARD;
      case "smartBannerPortrait":
        return AdSize.SMART_BANNER;
      case "smartBannerLandscape":
        return AdSize.SMART_BANNER;
      case "smartBanner":
        return AdSize.SMART_BANNER;
      default:
        return AdSize.BANNER;
    }
  }
}
