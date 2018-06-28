package net.anei.cadpage;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import net.anei.cadpage.vendors.VendorManager;

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

    String regId = FirebaseInstanceId.getInstance().getToken();
    if (regId != null) {
      Log.w("FCM registration succeeded: " + regId);
      boolean change = ManagePreferences.setRegistrationId(regId);
      ManagePreferences.registerReqRelease();
      VendorManager.instance().registerC2DMId(CadPageApplication.getContext(), change, regId);
    }
  }
}
