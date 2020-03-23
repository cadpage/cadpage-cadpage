package net.anei.cadpage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.core.app.NotificationCompat;

/*
 * This class handles the Notifications (sounds/vibrate/LED)
 */
@SuppressWarnings({"TryFinallyCanBeTryWithResources", "RedundantIfStatement"})
public class ManageNotification {

  public static final String ALERT_CHANNEL_ID = "net.anei.cadpage.ALERT_CHANNEL_ID";

  public static final String TRACKING_CHANNEL_ID = "net.anei.cadpage.TRACKING_CHANNEL_ID";

  public static final String MISC_CHANNEL_ID = "net.anei.cadpage.MISC_CHANNEL_ID";

  private static final String OLD_NOTIFY_CHANNEL_ID = "net.anei.cadpage.NOTIFY_CHANNEL_ID";
  public static final String NOTIFY_CHANNEL_ID = "net.anei.cadpage.NOTIFY_CHANNEL_ID_2";

  private static final int MAX_PLAYER_RETRIES = 4;

  private static final int NOTIFICATION_ALERT = 1337;
  
  private static MediaPlayer mMediaPlayer = null;
  
  private static final AudioFocusChangeListener afm = new AudioFocusChangeListener();
  
  private static boolean phoneMuted = false;
  
  private static boolean activeNotice = false;
  
  @SuppressWarnings("serial")
  private static class MediaFailureException extends RuntimeException {
    MediaFailureException(String desc) {
      super(desc);
    }
  }

  /**
   * Setup initial notification channels
   * @param context current context
   */
  public static void setup(Context context) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    assert nm != null;
    NotificationChannel channel = nm.getNotificationChannel(ALERT_CHANNEL_ID);
    if (channel == null) {
      channel = new NotificationChannel(ALERT_CHANNEL_ID, context.getString(R.string.regular_alert_title), NotificationManager.IMPORTANCE_HIGH);
      channel.setDescription(context.getString(R.string.regular_alert_text));
      channel.setBypassDnd(true);
      channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

      channel.enableLights(ManagePreferences.flashLED());
      channel.setLightColor(getLEDColor(context));
      // There does not seem to be a way to set the LED blink rate :(

      channel.enableVibration(ManagePreferences.vibrate());
      long[] vibratePattern = getVibratePattern(context);
      if (vibratePattern != null) channel.setVibrationPattern(vibratePattern);

      if (!ManagePreferences.notifyOverride()) {
        Uri uri = Uri.parse(ManagePreferences.notifySound());
        AudioAttributes aa = new AudioAttributes.Builder()
            .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
            .build();
        channel.setSound(uri, aa);
      } else {
        channel.setSound(null, null);
      }

      nm.createNotificationChannel(channel);
    }
    Log.v("Current Notification Channel info:" + channel.toString());

     // See if the alert is going to play an audio alert
    boolean audioAlert = channel.getImportance() >= NotificationManager.IMPORTANCE_DEFAULT &&
                         channel.getSound() != null;

     channel = nm.getNotificationChannel(TRACKING_CHANNEL_ID);
     if (channel == null) {
       channel = new NotificationChannel(TRACKING_CHANNEL_ID, context.getString(R.string.tracking_notif_title), NotificationManager.IMPORTANCE_LOW);
       channel.setDescription(context.getString(R.string.tracking_notif_text));
       channel.setBypassDnd(false);
       channel.setSound(null, null);
       nm.createNotificationChannel(channel);
     }

     channel = nm.getNotificationChannel(MISC_CHANNEL_ID);
     if (channel == null) {
       channel = new NotificationChannel(MISC_CHANNEL_ID, context.getString(R.string.misc_notif_title), NotificationManager.IMPORTANCE_NONE);
       channel.setBypassDnd(false);
       channel.setSound(null, null);
       nm.createNotificationChannel(channel);
     }

    channel = nm.getNotificationChannel(NOTIFY_CHANNEL_ID);
    if (channel == null) {
      channel = new NotificationChannel(NOTIFY_CHANNEL_ID, context.getString(R.string.notify_notif_title), NotificationManager.IMPORTANCE_HIGH);
      channel.setBypassDnd(true);
      channel.setSound(null, null);
      nm.createNotificationChannel(channel);
    }

    nm.deleteNotificationChannel(OLD_NOTIFY_CHANNEL_ID);

    if (audioAlert && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      if (! ManagePreferences.notifyCheckAbort() &&
          ! PermissionManager.isGranted(context, PermissionManager.READ_EXTERNAL_STORAGE)) {
        Log.v("Checking Notification Security");
        ManagePreferences.setNotifyCheckAbort(true);
        ManageNotification.show(context, null, false, false);
        NotificationManager myNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert myNM != null;
        myNM.cancel(NOTIFICATION_ALERT);
      }
    }
  }

  /**
   * Check for potential conflict between system and Cadpage audio notification alert
   * @param context current context
   * @return True if audio alerts have been configured by both the system and Cadpage notification
   * settings
   */
  public static boolean checkNotificationAlertConflict(Context context) {

    // This is only a problem starting with Android Oreo
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false;

    Log.v("Checking for duplicate audio alert notification");

    // Check to see if Cadpage is overriding audio alerts
    if (!ManagePreferences.notifyEnabled()) return false;
    if (!ManagePreferences.notifyOverride()) return false;
    String soundURI = ManagePreferences.notifySound();
    if (soundURI == null) return false;

    // Check to see if System is generating audio alerts
    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    assert nm != null;
    NotificationChannel channel = nm.getNotificationChannel(ALERT_CHANNEL_ID);
    if (channel == null) return false;
    if (channel.getImportance() < NotificationManager.IMPORTANCE_DEFAULT) return false;
    Uri soundURI2 = channel.getSound();
    if (soundURI2 == null) return false;

    return true;
  }

  public static boolean checkPopupAlertConflict(Context context) {

    // This is only a problem if we are running Android 10 or better
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;

    // And the show alert popup option is enabled
    if (!ManagePreferences.popupEnabled()) return false;

    // And the regular dispatch notification chanel importance is less than high
    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    assert nm != null;
    NotificationChannel channel = nm.getNotificationChannel(ALERT_CHANNEL_ID);
    return channel.getImportance() < NotificationManager.IMPORTANCE_HIGH;
  }

  /**
   * Determine if Oreo notification chanel is configured to vibrate
   * @param context current context
   * @return true if notification channel is configured to vibrate
   */
  public static boolean isVibrateEnabled(Context context) {

    // Only an issue with Android Oreo
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false;
    if (!ManagePreferences.notifyEnabled()) return false;

    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    assert nm != null;
    NotificationChannel channel = nm.getNotificationChannel(ALERT_CHANNEL_ID);
    if (channel == null) return false;
    if (channel.getImportance() < NotificationManager.IMPORTANCE_DEFAULT) return false;
    return channel.shouldVibrate();
  }

  /**
   * Return misc notification used for short term tasks requiring a foreground service
   * @param context current context
   * @param textId ID text to explain what Cadpage is doing
   * @return notification
   */
  public static Notification getMiscNotification(Context context, int textId) {
    NotificationCompat.Builder nbuild = new NotificationCompat.Builder(context, MISC_CHANNEL_ID);

    // Set display icon
    nbuild.setSmallIcon(R.drawable.ic_stat_notify);

    // From Oreo on, these are set at the notification channel level
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      nbuild.setPriority(NotificationCompat.PRIORITY_MIN);
      nbuild.setVisibility(NotificationCompat.VISIBILITY_SECRET);
    }

    nbuild.setContentTitle(context.getString(R.string.app_name));
    nbuild.setContentText(context.getString(textId));

    // The default intent when the notification is clicked (Inbox)
    Intent intent = CadPageActivity.getLaunchIntent(context);
    PendingIntent notifIntent = PendingIntent.getActivity(context, 10001, intent,
        PendingIntent.FLAG_CANCEL_CURRENT);
    nbuild.setContentIntent(notifIntent);

    return nbuild.build();
  }
  
  /**
   * @return true if some kind of notification should be launched when page is received
   */
  public static boolean isNotificationEnabled() {
    return ManagePreferences.notifyEnabled();
  }

  /*
   * The main notify method
   */
  public static void show(Context context, SmsMmsMessage message) {
    
    // Check if notifications are enabled - if not, we're done :)
    if (!ManagePreferences.notifyEnabled()) return;
    
    // Schedule notification after appropriate delay
    ReminderReceiver.scheduleNotification(context, message);
  }
  
  /**
   * Called by ReminderReceiver when it is time to initiate a notification
   * @param context current context
   * @param message message this is about or null if this is a dummy onetime check
   *                to see if we can generate a notification without triggering a security exception.
   * @param fullScreen true to display full screen notification
   * @param first true if this is the initial notification for this call.
   */
  public static void show(Context context, SmsMmsMessage message, boolean fullScreen, boolean first) {
    
    // Save phone muted status while we have a context to work with
    AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    assert am != null;
    phoneMuted =  (am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);

    // Build and launch the notification
    Notification n = buildNotification(context, message, fullScreen);
    
    NotificationManager myNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    assert myNM != null;

    // Seems this is needed for the number value to take effect on the Notification
    activeNotice = true;
    myNM.cancel(NOTIFICATION_ALERT);

    // Sometimes, under conditions we cannot reproduce, this throws a security
    // exception if we do not have data storage permission
    try {
      myNM.notify(NOTIFICATION_ALERT, n);
    } catch (SecurityException ex) {
      Log.e(ex);
      ManagePreferences.setNotifyAbort(true);
      return;
    }

    // If this was a security check and we got to here, everything is OK
    // so cancel the notification and return;
    if (message == null) {
      myNM.cancel(NOTIFICATION_ALERT);
      return;
    }

    // Schedule a reminder notification
    ReminderReceiver.scheduleReminder(context, message);
    
    // If this is the initial notification, schedule the timeout event
    if (first) {
      ClearAllReceiver.setCancel(context, ManagePreferences.notifyTimeout(), ClearAllReceiver.ClearType.NOTIFY);
    }
  }

  /**

   * Build the notification from user preferences
   * @param context current context
   * @param message message this is about or null if this is a dummy onetime check
   *                to see if we can generate a notification without triggering a security exception.
   * @param fullScreen true if full screen notification should be displayed
   * @return notification object
   */
  private static Notification buildNotification(Context context, SmsMmsMessage message, boolean fullScreen) {

    /*
     * Ok, let's create our Notification object and set up all its parameters.
     */
    NotificationCompat.Builder nbuild;

    // Select the appropriate channel.  Usually this is the regular alert channel.  But if notifications
    // are not enabled, we are being called to use the full screen notification process required to
    // pop up a detail screen starting with Android 10.  In which case we do not want the regular
    // audio and vibrate options and should use the quiet background notification channel rather
    // than the regular alert channel.
    String channelId = ManagePreferences.notifyEnabled() ? ALERT_CHANNEL_ID : NOTIFY_CHANNEL_ID;
    nbuild = new NotificationCompat.Builder(context, channelId);

    // Set auto-cancel flag
    nbuild.setAutoCancel(true);

    // Set display icon
    nbuild.setSmallIcon(R.drawable.ic_stat_notify);

    // From Oreo on, these are set at the notification channel level
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

      // Maximum priority
      nbuild.setPriority(NotificationCompat.PRIORITY_MAX);

      // Message category
      nbuild.setCategory(NotificationCompat.CATEGORY_CALL);

      // Set public visibility
      nbuild.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

      // Set up LED pattern and color
      if (ManagePreferences.flashLED()) {
        /*
         * Set up LED blinking pattern
         */
        int col = getLEDColor(context);
        int[] led_pattern = getLEDPattern(context);
        nbuild.setLights(col, led_pattern[0], led_pattern[1]);
      }

      /*
       * Set up vibrate pattern
       */
      // If vibrate is ON, or if phone is set to vibrate
      AudioManager AM = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
      assert AM != null;
      if ((ManagePreferences.vibrate() || AudioManager.RINGER_MODE_VIBRATE == AM.getRingerMode())) {
        long[] vibrate_pattern = getVibratePattern(context);
        if (vibrate_pattern != null) {
          nbuild.setVibrate(vibrate_pattern);
        } else {
          nbuild.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        }
      }
    }

    if ( ManagePreferences.notifyEnabled()) {
      
      // Are we doing are own alert sound?
      // Skip this for the dummy security check
      if (ManagePreferences.notifyOverride()) {

        if (message != null) {
          // Save previous volume and set volume to max
          overrideVolumeControl(context);

          // Start Media Player
          startMediaPlayer(context, 0);
        }
      } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
        Uri alarmSoundURI = Uri.parse(ManagePreferences.notifySound());
        nbuild.setSound(alarmSoundURI);
      }
    }

    nbuild.setContentTitle(context.getString(R.string.cadpage_alert));
    if (message != null) {
      String call = message.getTitle();
      nbuild.setContentText(call);
      nbuild.setStyle(new NotificationCompat.InboxStyle().addLine(call).addLine(message.getAddress()));
      nbuild.setWhen(message.getIncidentDate().getTime());
    }

    // The default intent when the notification is clicked (Inbox)
    Intent smsIntent = CadPageActivity.getLaunchIntent(context, true, false, message);
    Log.v("Notification launch intent");
    ContentQuery.dumpIntent(smsIntent);

    PendingIntent notifIntent = PendingIntent.getActivity(context, 10001, smsIntent,
                                                          PendingIntent.FLAG_CANCEL_CURRENT);
    nbuild.setContentIntent(notifIntent);
    if (fullScreen) {
      nbuild.setFullScreenIntent(notifIntent, true);
      nbuild.setPriority(NotificationCompat.PRIORITY_MAX);
      nbuild.setCategory(NotificationCompat.CATEGORY_CALL);
      nbuild.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    // Set intent to execute if the "clear all" notifications button is pressed -
    // basically stop any future reminders.
    Intent deleteIntent = new Intent(new Intent(context, ReminderReceiver.class));
    deleteIntent.setAction(Intent.ACTION_DELETE);
    PendingIntent pendingDeleteIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);
    nbuild.setDeleteIntent(pendingDeleteIntent);

    return nbuild.build();
  }

  /**
   * Save current volume level and max out the volume
   * @param context current context
   */
  private static void overrideVolumeControl(Context context) {
    
    // If user doesn't not want the volume maxed out, do not do anything
    if (! ManagePreferences.notifyOverrideVolume()) return;

    // Grab audio focus
    Log.v("Grab Audio Focus");
    AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    assert am != null;
    am.requestAudioFocus(afm, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    
    // If a restore volume is already set, assume that we have already
    // forced maximum volume and do nothing
    int restoreMode = ManagePreferences.restoreMode();
    int restoreVol = ManagePreferences.restoreVol();
    if (restoreMode < 0 && restoreVol < 0) {
      
      // Otherwise set the ringer mode to normal if it isn't there already

      // Late versions of Android do not allow us to do any of this if we are current in a
      // "do not disturb" status and have not been granted permission to change that status.
      // Testing for this situation is complicated, much easier to just do it and catch any
      // resulting exceptions.

      try {
        int curMode = am.getRingerMode();
        if (curMode != AudioManager.RINGER_MODE_NORMAL) {
          am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
          ManagePreferences.setRestoreMode(curMode);
        }

        // And get the max and current volume for the notification stream
        // If they are not equal, save the current stream volume and set
        // it to the max value
        int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        int curVol = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        if (curVol != maxVol) {
          am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxVol, 0);
          ManagePreferences.setRestoreVol(curVol);
        }
      } catch (SecurityException ignored) {}
    }
  }


  /**
   * Start up Media player to playoverride alert sound
   * @param context current context
   */
  private synchronized static void startMediaPlayer(Context context, int startCnt) {
    
    try {
      boolean loop = ManagePreferences.notifyOverrideLoop();
      if (mMediaPlayer == null) {
        mMediaPlayer = new MediaPlayer();
        if (!loop) {
          MediaOnCompletionListener listener = new MediaOnCompletionListener(context);
          mMediaPlayer.setOnCompletionListener(listener);
        }
        MediaErrorListener listener = new MediaErrorListener(context, startCnt);
        mMediaPlayer.setOnErrorListener(listener);
        if (ManagePreferences.notifyOverrideSound()) {
          AssetFileDescriptor fd = context.getResources().openRawResourceFd(R.raw.generalquarter);
          try {
            mMediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
          } catch (IOException ex) {
            Log.e("Media Player failure: standard alert");
            Log.e(ex);
            throw ex;
          } finally {
            fd.close();
          }
        } else {
          String soundURI = ManagePreferences.notifySound();
          if (soundURI == null || soundURI.length() == 0) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            return;
          }
          setMediaPlayerDataSource(context, mMediaPlayer, soundURI);
        }
        
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
        mMediaPlayer.prepare();
        if (loop) mMediaPlayer.setLooping(true);
        listener.arm();
        mMediaPlayer.start();
      } else {
        mMediaPlayer.start();
      }
      Log.v("Playback start successful");
    } catch (IOException ex) {
      
      // Failures are fairly common and usually indicate something wrong
      // with the selected ringtone we are trying to play.  We already 
      // logged the error when it happened, so just close things up
      try {
        mMediaPlayer.stop();
      } catch (Exception ignored) {}
      mMediaPlayer.release();
      mMediaPlayer = null;
      Log.v("Playback start failed");
      Log.e(ex);
    }
  }
  
  /**
   * Really really try to set up media player data source 
   * @param context current context
   * @param mp media player
   * @param fileInfo String specifying the audio data source
   * @throws IOException if anything goes wrong
   */
  private static void setMediaPlayerDataSource(Context context, MediaPlayer mp, 
                                               String fileInfo) throws IOException {
    
    // This get's complicated.  Recommended procedure is to convert content URI's to
    // path names if possible
    if (fileInfo.startsWith("content://")) {
      Uri uri = Uri.parse(fileInfo);
      String tmp = getRingtonePathFromContentUri(context, uri);
      if (tmp != null) fileInfo = tmp;
    }

    try {
      // Starting with Honeycomb, we only try setting the source by URI
     setMediaPlayerDataSourcePostHoneyComb(context, mp, fileInfo);
    }

    // If that failed, try setting the media source using a file descriptor
    catch (IOException ex) {
      try {
        setMediaPlayerDataSourceUsingFileDescriptor(mp, fileInfo);
      } 
      
      // And if that fails, try converting the file name to a ringtone URI
      catch (IOException ex2) {
        Uri uri = getRingtoneUriFromPath(context, fileInfo);
        if (uri == null) throw ex2;
        mp.reset();
        try {
          Log.v("Backup Media Player Setup");
          mp.setDataSource(context, uri);
        } catch (IOException ex3) {
          Log.e("Media Player Failure:" + uri);
          Log.e(ex3);
          throw ex3;
        }
      }
    }
  }

  /**
   * Set Media source under post honeycomb system
   * @param context current context
   * @param mp media player
   * @param fileInfo file path name
   * @throws IOException if anything goes wrong
   */
  private static void setMediaPlayerDataSourcePostHoneyComb(Context context,
                                                            MediaPlayer mp, String fileInfo) throws IOException {
    Log.v("PostH Media Player Setup");
    mp.reset();
    Uri uri = Uri.parse(fileInfo);
    try {
      mp.setDataSource(context, uri);
    } catch (IOException ex) {
      Log.e("Media Player Failure:" + uri);
      Log.e(ex);
      throw ex;
    }
  }

  /**
   * Set Media source using a file description
   * @param mp media player
   * @param fileInfo file path name
   * @throws IOException if anything goes wrong
   */
  private static void setMediaPlayerDataSourceUsingFileDescriptor(MediaPlayer mp, String fileInfo) throws IOException {
    Log.v("File Desc Media Player Setup - " + fileInfo);
    File file = null;
    FileInputStream inputStream = null;
    try {
      file = new File(fileInfo);
      inputStream = new FileInputStream(file);
      mp.reset();
      mp.setDataSource(inputStream.getFD());
    } catch (IOException ex) {
      Log.e("Media Player Failure:" + file);
      Log.e(ex);
      throw ex;
    } finally {
      if (inputStream != null) inputStream.close();
    }
  }

  /**
   * Convert file path to ringtone URI
   * @param context current context
   * @param path file path
   * @return equivalent content URI if successful, null otherwise
   */
  private static Uri getRingtoneUriFromPath(Context context, String path) {
    
    if (!PermissionManager.isGranted(context, PermissionManager.READ_EXTERNAL_STORAGE)) return null;
    
    try {
      Uri ringtonesUri = MediaStore.Audio.Media.getContentUriForPath(path);
      if (ringtonesUri == null) return null;
      Cursor ringtoneCursor = context.getContentResolver().query(ringtonesUri, 
          new String[]{MediaStore.Audio.Media._ID}, 
          MediaStore.Audio.Media.DATA + "='?'", new String[]{path}, null);
      if (ringtoneCursor == null) return null;
      try {
        if (!ringtoneCursor.moveToFirst()) return null;
        long id = ringtoneCursor.getLong(ringtoneCursor.getColumnIndex(MediaStore.Audio.Media._ID));
        if (!ringtonesUri.toString().endsWith(String.valueOf(id))) {
          ringtonesUri = ringtonesUri.buildUpon().appendPath(Long.toString(id)).build();
        }
        return ringtonesUri;
      } finally {
        ringtoneCursor.close();
      }
    } catch (Exception ex) {
      Log.e(ex);
      return null;
    }
  }

  /**
   * Convert content URI to file path
   * @param context current context
   * @param contentUri content URI
   * @return equivalent file path if successful, null otherwise
   */
  private static String getRingtonePathFromContentUri(Context context, Uri contentUri) {
    
    if (!PermissionManager.isGranted(context, PermissionManager.READ_EXTERNAL_STORAGE)) return null;

    try {
      String[] proj = { MediaStore.Audio.Media.DATA };
      Cursor ringtoneCursor = context.getContentResolver().query(contentUri, proj, null, null, null);
      if (ringtoneCursor == null) return null;
      try {
        if (!ringtoneCursor.moveToFirst()) return null;
        return ringtoneCursor.getString(ringtoneCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
      } finally {
        ringtoneCursor.close();
      }
    } catch (Exception ex) {
      Log.e(ex);
      return null;
    }
  }

  /**
   * Stop media player
   */
  private synchronized static void stopMediaPlayer(Context context) {
    // Stop media player if running
    if (mMediaPlayer != null) {
      mMediaPlayer.stop();
      mMediaPlayer.release();
      mMediaPlayer = null;
    }
    
    // Restore volume if we messed it up
    restoreVolumeControl(context);
  }

  private static void restoreVolumeControl(Context context) {

    AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    assert am != null;

    // If a restore volume level has been set, restore the volume to that level
    //  and delete the restore volume level
    int vol = ManagePreferences.restoreVol();
    if (vol >= 0) {
      try {
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, vol, 0);
      } catch (SecurityException ignored) {}
      ManagePreferences.setRestoreVol(-1);
    }

    // If restore mode has been set, restore that ringer mode
    int mode = ManagePreferences.restoreMode();
    if (mode >= 0) {
      try {
        am.setRingerMode(mode);
      } catch (SecurityException ignored) {}
      ManagePreferences.setRestoreMode(-1);
    }
    
    // And release audio focus
    Log.v("Release Audio Focus");
    am.abandonAudioFocus(afm);

  }
  
  // Media onComplete listener
  // Restore normal volume when alert playback completes
  private static class MediaOnCompletionListener implements OnCompletionListener {
    
    private final Context context;
    
    MediaOnCompletionListener(Context context) {
      this.context = context;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
      Log.v("Playback completed");
      restoreVolumeControl(context);
    }
  }
  
  // Media error listener
  // Our job is to shut down and restart the media play if it dies for some
  // unknown reason.  
  private static class MediaErrorListener implements OnErrorListener {
    
    private final Context context;
    private final int startCnt;
    private boolean armed = false;
    
    MediaErrorListener(Context context, int startCnt) {
      this.context = context;
      this.startCnt = startCnt;
    }
    
    void arm() {
      armed = true;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
      if (armed && what == MediaPlayer.MEDIA_ERROR_SERVER_DIED && startCnt < MAX_PLAYER_RETRIES) {
        Log.e("Restarting Media Player");
        stopMediaPlayer(context);
        startMediaPlayer(context, startCnt+1);
        return true;
      }

      Log.e(new MediaFailureException("Media Player failure - what:" + what + " extra:" + extra + " cnt:" + startCnt));
      return true;
    }
  }

  // Clear a single notification
  public static void clear(Context context) {
    
    activeNotice = false;

    // Clear any pending reminders
    ReminderReceiver.cancelReminder(context);

    NotificationManager myNM =
      (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    assert myNM != null;
    myNM.cancel(NOTIFICATION_ALERT);
    
    stopMediaPlayer(context);
  }
  
  /**
   * @return true if Cadpage alert notification is active
   */
  public static boolean isActiveNotice() {
    return activeNotice;
  }

  /**
   * Parse the user provided custom vibrate pattern into a long[]
   * 
   */
  public static long[] parseVibratePattern(String stringPattern) {
    ArrayList<Long> arrayListPattern = new ArrayList<>();
    Long l;

    if (stringPattern == null) return null;

    String[] splitPattern = stringPattern.split(",");
    int VIBRATE_PATTERN_MAX_SECONDS = 60000;
    int VIBRATE_PATTERN_MAX_PATTERN = 100;

    for (String part : splitPattern) {
      try {
        l = Long.parseLong(part.trim());
      } catch (NumberFormatException e) {
        return null;
      }
      if (l > VIBRATE_PATTERN_MAX_SECONDS) {
        return null;
      }
      arrayListPattern.add(l);
    }

    int size = arrayListPattern.size();
    if (size > 0 && size < VIBRATE_PATTERN_MAX_PATTERN) {
      long[] pattern = new long[size];
      for (int i = 0; i < pattern.length; i++) {
        pattern[i] = arrayListPattern.get(i);
      }
      return pattern;
    }

    return null;
  }

  private static int getLEDColor(Context context) {

    // Check if a custom color is set
    String flashLedCol = ManagePreferences.flashLEDColor();
    if (context.getString(R.string.pref_custom_val).equals(flashLedCol)) {
      flashLedCol = ManagePreferences.flashLEDColorCustom();
    }

    // Default in case the parse fails
    int col = Color.parseColor(context.getString(R.string.pref_flashled_color_default));

    // Try and parse the color
    if (flashLedCol != null) {
      try {
        col = Color.parseColor(flashLedCol);
      } catch (IllegalArgumentException ignore) {}
    }
    return col;
  }

  private static int[] getLEDPattern(Context context) {

    int[] ledPattern;

    String flashLedPattern = ManagePreferences.flashLEDPattern();
    if (context.getString(R.string.pref_custom_val).equals(flashLedPattern)) {
      ledPattern = parseLEDPattern(ManagePreferences.flashLEDPatternCustom());
    } else {
      ledPattern = parseLEDPattern(flashLedPattern);
    }


    // Set to default if there was a problem
    if (ledPattern == null) {
      ledPattern = parseLEDPattern(context.getString(R.string.pref_flashled_pattern_default));
      assert ledPattern != null;
    }

    return ledPattern;

  }
  /**
   * Parse LED pattern string into int[]
   * 
   * @param stringPattern LED Light pattern
   * @return integer version of pattern
   */
  public static int[] parseLEDPattern(String stringPattern) {
    int[] arrayPattern = new int[2];
    int on, off;

    if (stringPattern == null) return null;

    String[] splitPattern = stringPattern.split(",");

    if (splitPattern.length != 2) return null;

    int LED_PATTERN_MIN_SECONDS = 0;
    int LED_PATTERN_MAX_SECONDS = 60000;

    try {
      on = Integer.parseInt(splitPattern[0]);
    } catch (NumberFormatException e) {
      return null;
    }

    try {
      off = Integer.parseInt(splitPattern[1]);
    } catch (NumberFormatException e) {
      return null;
    }

    if (on >= LED_PATTERN_MIN_SECONDS && on <= LED_PATTERN_MAX_SECONDS
        && off >= LED_PATTERN_MIN_SECONDS && off <= LED_PATTERN_MAX_SECONDS) {
      arrayPattern[0] = on;
      arrayPattern[1] = off;
      return arrayPattern;
    }

    return null;
  }

  private static long[] getVibratePattern(Context context) {
    String vibrate_pattern_raw = ManagePreferences.vibratePattern();
    if (context.getString(R.string.pref_custom_val).equals(vibrate_pattern_raw)) {
      vibrate_pattern_raw = ManagePreferences.vibratePatternCustom();
    }
    return parseVibratePattern(vibrate_pattern_raw);
  }

  /**
   * Called to determine if an acknowledge function is needed to clear a
   * pending alert
   * @return true if there is a reminder or alert to be cleared
   */
  public static boolean isAckNeeded() {
    
    // volume control is overridden, return true if notification sound is
    // looped or is repeated
    if (ManagePreferences.notifyOverride()) {
      if (ManagePreferences.notifyOverrideLoop()) return true;
      if (ManagePreferences.notifyRepeatInterval() > 0) return true;
    }
    
    // If volume control is not overridden, return false if ringer is silenced
    // return true if notification sound is repeating
    else {
      if (phoneMuted) return false;
      if (ManagePreferences.notifyRepeatInterval() > 0) return true;
    }
    
    // Otherwise, we really do not know, so follow user recommendation
    return ManagePreferences.notifyReqAck();
  }

  /**
   * Audio focus change listener
   */
  static class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {

    @Override
    public void onAudioFocusChange(int focusChange) {
      // We consider ourselves to be the ultimate power and won't release
      // audio control for anyone
    }
  }

  public static void showNoticeNotification(Context context, String title, String text, Intent intent) {
    NotificationCompat.Builder nbuild;
    nbuild = new NotificationCompat.Builder(context, NOTIFY_CHANNEL_ID);

    // Set auto-cancel flag
    nbuild.setAutoCancel(true);

    // Set display icon
    nbuild.setSmallIcon(R.drawable.ic_stat_notify);

    nbuild.setContentTitle(title);
    nbuild.setContentText(text);

    PendingIntent notifIntent = PendingIntent.getActivity(context, 10009, intent,
        PendingIntent.FLAG_CANCEL_CURRENT);
    nbuild.setContentIntent(notifIntent);
    nbuild.setFullScreenIntent(notifIntent, true);
    nbuild.setPriority(NotificationCompat.PRIORITY_MAX);
    nbuild.setCategory(NotificationCompat.CATEGORY_CALL);
    nbuild.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

    NotificationManager myNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    assert myNM != null;
    myNM.notify(NOTIFICATION_ALERT, nbuild.build());
  }
}
