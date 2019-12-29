package net.anei.cadpage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class LocationTracker {

  public interface LocationChangeListener {
    void locationChange(Location location);
  }

  // Expected accuracy degradation in m/msec
  private static final double LOC_ACC_ADJUSTMENT = .002;  // About 2 m/sec

  private String bestProvider = null;

  private final List<LocationChangeListener> listenerList = new ArrayList<>();

  private final LocationListener myListener = new LocationListener(){

    @Override
    public void onLocationChanged(Location location) {
      for (LocationChangeListener listener : listenerList) {
        listener.locationChange(location);
      }
    }

    @Override
    public void onProviderDisabled(String provider) {
      Log.v("LocationService - provider disabled:" + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
      Log.v("LocationService - provider enabled:" + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
      Log.v("LocationService - status change:" + provider + ":" + status);
    }

  };

  @SuppressLint("MissingPermission")
  public void start(Context context, int minDist, int minTime, LocationChangeListener listener) {

    if (listenerList.contains(listener)) return;

    if (bestProvider == null) {

      // If we don't have an active best provider, set one up and
      LocationManager locMgr = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
      assert locMgr != null;
      if (bestProvider == null) {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        bestProvider = locMgr.getBestProvider(criteria, true);
        if (bestProvider != null) {
          Log.v("Turning on location tracking");
          locMgr.requestLocationUpdates(bestProvider, minDist, minTime, myListener);
        }
      }
    }

    if (bestProvider != null) {
      listenerList.add(listener);
      Location location = getBestLocation(context);
      if (location != null) listener.locationChange(location);
    }
  }

  public void stop(Context context, LocationChangeListener listener) {
    if (listenerList.isEmpty()) return;

    listenerList.remove(listener);
    if (listenerList.isEmpty()) {
      Log.v("Turning off location tracking");
      LocationManager locMgr = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
      assert locMgr != null;
      locMgr.removeUpdates(myListener);
      bestProvider = null;
    }
  }

  public Location getBestLocation(Context context) {

    // Get a list of all enabled location providers see which one
    // provides the best last known location
    LocationManager locMgr = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
    Location bestLoc = null;
    assert locMgr != null;
    for (String name : locMgr.getProviders(true)) {
      try {
        Location loc = locMgr.getLastKnownLocation(name);
        if (loc == null) continue;
        Log.v("lastKnownLocation:" + loc.toString());
        if (bestLoc != null) {
          float deltaAcc = loc.getAccuracy() - bestLoc.getAccuracy();
          long deltaTime = loc.getTime() - bestLoc.getTime();
          deltaAcc -= deltaTime * LOC_ACC_ADJUSTMENT;
          if (deltaAcc < 0) continue;
        }
        bestLoc = loc;
      } catch (SecurityException ignore) {}
    }

    return bestLoc;
  }

  private static final LocationTracker instance = new LocationTracker();

  public static LocationTracker instance() {
    return instance;
  }
}
