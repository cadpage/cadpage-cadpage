package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.Intent;

import net.anei.cadpage.BuildConfig;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SupportApp;
import net.anei.cadpage.SmsMmsMessage;
import net.anei.cadpage.vendors.VendorManager;

/*

My department sends me text alerts

Cadpage text message processing is currently disabled.  You may have to grant Cadpage
permission to access text messages before enabling this.
 */
public class HelpEnableSmsEvent extends DonateScreenEvent {

  private SmsMmsMessage msg;

  private HelpEnableSmsEvent() {
    super(R.string.help_text_dispatch_title, R.string.help_text_dispatch_wintitle, R.string.help_enable_sms_text,
          HelpDoEnableSmsEvent.instance());
  }

  @Override
  public boolean isEnabled() {
    return SupportApp.instance().isRecMsgSupported();
  }

  @Override
  public void create(Activity activity, SmsMmsMessage msg) {

    this.msg = msg;

    // If SMS message processing is enabled, we want to switch to the regular text processing menu
    // Otherwise process normally
    if (ManagePreferences.enableMsgType().contains("S")) {
      ((DonateActivity)activity).switchEvent(HelpTextDispatchEvent.instance(), msg);
    } else {
      super.create(activity, msg);
    }
  }

  @Override
  public void onRestart(DonateActivity activity) {
    super.onRestart(activity);
    if (ManagePreferences.enableMsgType().contains("S")) {
      activity.switchEvent(HelpTextDispatchEvent.instance(), msg);
    }
  }

  private static final HelpEnableSmsEvent instance = new HelpEnableSmsEvent();
  public static HelpEnableSmsEvent instance() {
    return instance;
  }

}
