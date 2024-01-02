package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/*
    Install Cadpage message support app

    This version of Cadpage is not allowed to process incoming MMS messages.
    If you need this functionality, you will need to install the Cadpage message support app.
 */
public class NeedCadpageSupportApp2Event extends DonateScreenEvent {

  private NeedCadpageSupportApp2Event() {
    super(AlertStatus.RED, R.string.donate_need_cadpage_support_app_title, R.string.donate_need_cadpage_support_app2_text,
          InstallCadpageSupportAppEvent.instance(),
          FixMsgSupportEvent.instance());
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public void onRestart(DonateActivity activity) {
    if (SmsPopupUtils.checkMsgSupport(activity, false) != 1) {
      if (BatteryOptimizationSupportEvent.instance().isEnabled()) {
        activity.switchEvent(BatteryOptimizationSupportEvent.instance(), null);
      } else {
        closeEvents(activity);
      }
    }
  }

  private static final NeedCadpageSupportApp2Event instance = new NeedCadpageSupportApp2Event();
  public static NeedCadpageSupportApp2Event instance() {
    return instance;
  }

}
