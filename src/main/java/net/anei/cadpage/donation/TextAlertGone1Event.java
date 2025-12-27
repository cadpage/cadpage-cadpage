package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.Intent;

import net.anei.cadpage.BuildConfig;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;
import net.anei.cadpage.SupportApp;
import net.anei.cadpage.vendors.VendorManager;

/*
    Text alert support is gone

    Due to Google Play restrictions, alerts for text dispatch messages are is not longer supported.
    If you need this capability you will either need to switch to the Cadpage Paging service or
    install a version of Cadpage from somewhere other than the Google Play Store
 */
public class TextAlertGone1Event extends DonateScreenEvent {

  private SmsMmsMessage msg;

  private TextAlertGone1Event() {
    super(AlertStatus.RED, R.string.help_text_dispatch_title,
          R.string.donate_text_alert_gone_title, R.string.donate_text_alert_gone1_text,
          HelpCadpagePagingRegisterPromptEvent.instance(),
          InstallRealCadpageEvent.instance(),
          DropMsgSupportEvent.instance());
  }

  @Override
  public boolean isEnabled() {
    return !SupportApp.instance().isRecMsgSupported();
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  private static final TextAlertGone1Event instance = new TextAlertGone1Event();
  public static TextAlertGone1Event instance() {
    return instance;
  }

}
