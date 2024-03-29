package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import net.anei.cadpage.Log;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/**
  Tell me more
 */
public class Active911ParseInfoDonateEvent extends DonateEvent {
  
  private static final String TARGET_URL = "https://www.cadpage.org/faq/cadpage-and-active911";
  
  public Active911ParseInfoDonateEvent() {
    super(AlertStatus.YELLOW, R.string.donate_active911_parse_info_title);
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
  
  private static final Active911ParseInfoDonateEvent instance = new Active911ParseInfoDonateEvent();
  
  public static Active911ParseInfoDonateEvent instance() {
    return instance;
  }

}
