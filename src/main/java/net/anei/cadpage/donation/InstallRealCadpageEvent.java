package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/*
    I really need to process texts alerts

  Options are limited.  Email us and we will see what can be done.
 */
public class InstallRealCadpageEvent extends DonateScreenEvent {

  private InstallRealCadpageEvent() {
    super(AlertStatus.GREEN, R.string.donate_install_real_cadpage_title, R.string.donate_install_real_cadpage_text,
          EmailHelpEvent.instance(),
          DoneDonateEvent.instance());
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  private static final InstallRealCadpageEvent instance = new InstallRealCadpageEvent();
  public static InstallRealCadpageEvent instance() {
    return instance;
  }

}
