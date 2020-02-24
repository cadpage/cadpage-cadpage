package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;
import net.anei.cadpage.SmsPopupUtils;

/*
    Install Cadpage mesasge support app

    The Cadpage app is or will soon be prohibited from processing incoming
    text messages.  If you need this functionality to continue, you need to
    install the Cadpage message support app.
 */
public class NeedCadpageSupportAppEvent extends DonateScreenEvent {

  private NeedCadpageSupportAppEvent() {
    super(AlertStatus.RED, R.string.donate_need_cadpage_support_app_title, R.string.donate_need_cadpage_support_app_text,
        InstallCadpageSupportAppEvent.instance(),
        DropMsgSupportEvent.instance());
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public void onRestart(DonateActivity activity) {
    if (SmsPopupUtils.checkMsgSupport(activity, false) != 1) {
      closeEvents(activity);
    }
  }

  private static final NeedCadpageSupportAppEvent instance = new NeedCadpageSupportAppEvent();
  public static NeedCadpageSupportAppEvent instance() {
    return instance;
  }

}
