package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import net.anei.cadpage.Log;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/**
Donate through PayPal
 */
public class DonateWhatsUpEvent extends DonateEvent {
  
  private static final String TARGET_URL = "http://www.cadpage.org/financial-support";
  
  public DonateWhatsUpEvent() {
    super(AlertStatus.YELLOW, R.string.donate_whats_up_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    if (!SmsPopupUtils.haveNet(activity)) return;
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(TARGET_URL)); 
    try {
      activity.startActivity(intent);
    } catch (ActivityNotFoundException ex) {
      Log.e(ex);
    }
  }
  
  private static final DonateWhatsUpEvent instance = new DonateWhatsUpEvent();
  
  public static DonateWhatsUpEvent instance() {
    return instance;
  }

}
