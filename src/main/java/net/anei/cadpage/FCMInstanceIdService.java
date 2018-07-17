package net.anei.cadpage;

import android.support.annotation.NonNull;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import net.anei.cadpage.vendors.VendorManager;

import java.io.IOException;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;

public class FCMInstanceIdService extends FirebaseInstanceIdService {
  @Override
  public void onCreate() {

    Log.v("FCMInstanceIdService:onCreate()");

    // If initialization failure in progress, shut down without doing anything
    if (TopExceptionHandler.isInitFailure()) return;

    // Make sure everything is initialized
    CadPageApplication.initialize(this);

    super.onCreate();
  }

  @Override
  public void onTokenRefresh() {

    Log.v("FCMInstanceIdService:onTokenRefresh()");

    String regId = getInstanceId();
    if (regId != null) {
      Log.w("FCM registration succeeded: " + regId);
      VendorManager.instance().reconnect(getApplicationContext(), false, regId);
    }
  }

  public static String getInstanceId() {
    return FirebaseInstanceId.getInstance().getToken();
  }

  public static void resetInstanceId() {
    OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ResetIdWorker.class).build();
    assert WorkManager.getInstance() != null;
    WorkManager.getInstance().enqueue(req);
  }

  public static class ResetIdWorker extends Worker {
    @NonNull
    public Result doWork() {
      Log.v("Reset FCM instance ID");
      try {
        FirebaseInstanceId.getInstance().deleteInstanceId();
        Log.v("deleteInstanceId succeeded");
        return Result.SUCCESS;
      } catch (IOException ex) {
        Log.e("DeleteInstanceId failed");
        Log.e(ex);
        return Result.FAILURE;
      }
    }
  }
}
