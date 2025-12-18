package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/*
    Install a different Cadpage version

    Any version of Cadpage you find somewhere other than Google Play Store will work.  Since this
    version was installed from the Play Store, it is not allowed to be more specific.  Seek and
    ye shall find.

 */
public class InstallRealCadpageEvent extends DonateScreenEvent {

  private InstallRealCadpageEvent() {
    super(AlertStatus.GREEN, R.string.donate_install_real_cadpage_title, R.string.donate_install_real_cadpage_text,
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
