package net.anei.cadpage;

import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import androidx.core.app.NotificationCompat;

public class TrackingService extends Service implements LocationTracker.LocationChangeListener {


  private static final String ACTION_SHUTDOWN = "ACTION_SHUTDOWN";
  private static final String ACTION_REPORT = "ACTION_REPORT";
  private static final String EXTRA_URL = "EXTRA_URL";
  private static final String EXTRA_END_TIME = "EXTRA_END_TIME";
  private static final String EXTRA_MIN_TIME = "EXTRA_MIN_TIME";
  private static final String EXTRA_MIN_DIST = "EXTRA_MIN_DIST";

  private static final int TRACKING_NOTIFICATION = 298;

  // Wake lock and synchronize lock
  static private PowerManager.WakeLock sWakeLock = null;

  /**
   * Internal class defining a location report request
   */
  private class LocationRequest {
    final String URL;
    long endTime;

    // Runnable that we will post to run at the time we want this request to go away
    final Runnable terminator = new Runnable(){
      @Override
      public void run() {
        requestQueue.remove(LocationRequest.this);
        if (requestQueue.size() == 0) stopSelf();
      }
    };

    LocationRequest(String URL, long endTime) {
      this.URL = URL;
      this.endTime = endTime;
      mHandler.postAtTime(terminator, endTime);
    }

    boolean mergeRequest(String URL, long endTime) {
      if (!URL.equals(this.URL)) return false;
      if (endTime > this.endTime) {
        this.endTime = endTime;
        mHandler.removeCallbacks(terminator);
        mHandler.postAtTime(terminator, endTime);
      }
      return true;
    }

    void report(Context context, Location loc) {
      Uri.Builder bld = Uri.parse(URL).buildUpon().appendQueryParameter("type", "LOCATION");
      bld.appendQueryParameter("lat", Double.toString(loc.getLatitude()));
      bld.appendQueryParameter("long", Double.toString(loc.getLongitude()));
      if (loc.hasAccuracy()) bld.appendQueryParameter("acc", Float.toString(loc.getAccuracy()));
      if (loc.hasAltitude()) bld.appendQueryParameter("alt", Double.toString(loc.getAltitude()));
      if (loc.hasBearing()) bld.appendQueryParameter("bearing", Float.toString(loc.getBearing()));
      if (loc.hasSpeed()) bld.appendQueryParameter("speed", Float.toString(loc.getSpeed()));
      bld.appendQueryParameter("time", Long.toString(loc.getTime()));

      HttpService.addHttpRequest(context, new HttpService.HttpRequest(bld.build()));
    }
  }

  private Handler mHandler = null;

  // Queue of outstanding location requests
  private final List<LocationRequest> requestQueue = new LinkedList<>();

  @SuppressLint("NewApi")
  @Override
  public void onCreate() {
    Log.v("LocationService starting up");

    // Set up a handler to manage location tracking termination
    mHandler = new Handler();

    // Put ourselves in foreground mode, also notifying user that tracking has been activated
    Intent intent = new Intent(ACTION_SHUTDOWN, null, this, TrackingService.class);
    PendingIntent pint = PendingIntent.getService(this, 0, intent, 0);
    NotificationCompat.Builder nb = new NotificationCompat.Builder(this, ManageNotification.TRACKING_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_notify)
        .setWhen(System.currentTimeMillis())
        .setContentTitle(getString(R.string.tracking_title))
        .setContentIntent(pint);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
      nb.setContentText(getString(R.string.tracking_text));
    } else {
      nb.addAction(R.drawable.ic_stat_notify, getString(R.string.stop_tracking_text), pint);

    }
    startForeground(TRACKING_NOTIFICATION, nb.build());

    CadPageApplication.initialize(this);
  }

  @SuppressLint("MissingPermission")
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    if (ACTION_SHUTDOWN.equals(intent.getAction())) {
      stopSelf();
      return START_NOT_STICKY;
    }

    // We shouldn't get here without location permission enabled.
    // But make one last check just in case
    if (!PermissionManager.isGranted(this, PermissionManager.ACCESS_FINE_LOCATION)) {
      stopSelf();
      return START_NOT_STICKY;
    }

    String url = intent.getStringExtra(EXTRA_URL);
    long endTime = intent.getLongExtra(EXTRA_END_TIME, 0L);
    int minDist = intent.getIntExtra(EXTRA_MIN_DIST, 10);
    int minTime = intent.getIntExtra(EXTRA_MIN_TIME, 10);
    if (url == null) return START_NOT_STICKY;

    if (flags != 0) holdPowerLock(this);

    // See if we can merge this request into an existing one. 
    boolean found = false;
    for (LocationRequest tReq : requestQueue) {
      if (tReq.mergeRequest(url, endTime)) {
        found = true;
        break;
      }
    }

    // If not, create a new entry and add it to the queue
    if (!found) requestQueue.add(new LocationRequest(url, endTime));

    LocationTracker.instance().start(this, minDist, minTime, this);

    return Service.START_REDELIVER_INTENT;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void locationChange(Location location) {

    // If no location, skip it
    if (location == null) return;
    
    Log.v("Location:" + location.toString());
    
    // Do not allow location reports closer that .5 sec apart
    // unless accuracy is significantly improved
    long locTime = location.getTime();
    float locAcc = location.getAccuracy();
    long lastTime = ManagePreferences.lastLocTime();
    if (locTime - lastTime <= 500) {
      float lastAcc = ManagePreferences.lastLocAcc();
      if (lastAcc - locAcc < .5) return;
    }
    
    // Save current location time and accuracy for future adjustments
    ManagePreferences.setLastLocTime(locTime);
    ManagePreferences.setLastLocAcc(locAcc);
    
    // Report location to all requesters
    for (LocationRequest req : requestQueue) req.report(this, location);
  }
  
  @Override
  public void onDestroy() {
    Log.v("Shutting down LocationService");
    LocationTracker.instance().stop(this, this);
    if (sWakeLock != null) sWakeLock.release();
  }

  /**
   * Initiate location report request
   * @param context current context
   * @param URL URL where location reports will be sent
   * @param duration requested reporting period in msecs
   * @param minDist minimum delta reporting distance in meters
   * @param minTime minimum delta reporting time in seconds
   */
  public static void addLocationRequest(Context context, String URL, int duration, int minDist, int minTime) {
    
    if (URL == null) return;
    
    // Create and hold partial power lock
    holdPowerLock(context);
    
    Intent intent = new Intent(ACTION_REPORT, null, context, TrackingService.class);
    intent.putExtra(EXTRA_URL, URL);
    intent.putExtra(EXTRA_END_TIME, SystemClock.uptimeMillis() + duration);
    intent.putExtra(EXTRA_MIN_DIST, minDist);
    intent.putExtra(EXTRA_MIN_TIME, minTime);
    context.startService(intent);
  }

  private static void holdPowerLock(Context context) {
    synchronized (TrackingService.class) {
      if (sWakeLock == null) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Log.LOGTAG+".TrackingService");
        sWakeLock.setReferenceCounted(false);
      }
      if(!sWakeLock.isHeld()) sWakeLock.acquire(30*60*1000L /*30 minutes*/);
    }
  }
}
