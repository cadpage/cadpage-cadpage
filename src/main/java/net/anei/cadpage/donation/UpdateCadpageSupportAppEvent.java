package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/*
    Update Cadpage message support app

    The Cadpage message support app you have installed is outdated and will
    need to be updated if it is to perform all of the functions that you are
    requesting of Cadpage.
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
