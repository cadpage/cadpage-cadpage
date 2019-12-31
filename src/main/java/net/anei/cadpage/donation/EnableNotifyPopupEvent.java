package net.anei.cadpage.donation;

import android.app.Activity;
import android.os.Build;
import androidx.annotation.RequiresApi;

import net.anei.cadpage.PreferenceNotificationFragment;
import net.anei.cadpage.R;

/**
 Enable notification "Pop on screen" option
 */
public class EnableNotifyPopupEvent extends DonateEvent {

  private EnableNotifyPopupEvent() {
    super(null, R.string.enable_notify_popup_title);
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  protected void doEvent(Activity activity) {
    PreferenceNotificationFragment.launchChannelConfig(activity);
  }
  
  private static final EnableNotifyPopupEvent instance = new EnableNotifyPopupEvent();
  
  public static EnableNotifyPopupEvent instance() {
    return instance;
  }

}
