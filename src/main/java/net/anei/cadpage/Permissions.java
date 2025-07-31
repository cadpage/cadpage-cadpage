package net.anei.cadpage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * This class handles permissions requests that must be handled
 * differently below SDK level 23 which never happens now that level 23 is our minimum support
 */
public class Permissions {

  public static boolean isGranted(Context context, String permission) {
    return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
  }

  public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
    return activity.shouldShowRequestPermissionRationale(permission);
  }
  
  public static void requestPermissions(Activity activity, String[] permissions, int requestId) {
    activity.requestPermissions(permissions, requestId);
  }

}
