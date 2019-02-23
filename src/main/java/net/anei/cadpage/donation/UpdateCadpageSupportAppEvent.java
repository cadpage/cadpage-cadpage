package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/*
    Update Cadpage message support app

    The Cadpage app is or will soon be prohibited from processing incoming
    text messages.  If you need this functionality to continue, you need to
    install the Cadpage message support app.
 */
public class UpdateCadpageSupportAppEvent extends DonateScreenEvent {

  private UpdateCadpageSupportAppEvent() {
    super(AlertStatus.RED, R.string.donate_update_cadpage_support_app_title, R.string.donate_update_cadpage_support_app_text,
          InstallCadpageSupportAppEvent.instance());
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public void onStart(DonateActivity activity) {
    if (SmsPopupUtils.checkMsgSupport(activity, false) != 1) {
      closeEvents(activity);
    }
  }

  private static final UpdateCadpageSupportAppEvent instance = new UpdateCadpageSupportAppEvent();
  public static UpdateCadpageSupportAppEvent instance() {
    return instance;
  }

}
