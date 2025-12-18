package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/*
    Text alert support is going away

    Due to Google Play restrictions, support for processing text alerts will soon disappear.
    If you need this capability you will either need to switch to the Cadpage Paging service, or
    install a version of Cadpage from somewhere other than the Google Play Store
 */
public class TextAlertWarnEvent extends DonateScreenEvent {

  private TextAlertWarnEvent() {
    super(AlertStatus.RED, R.string.donate_text_alert_warn_title, R.string.donate_text_alert_warn_text,
        HelpCadpagePagingRegisterPromptEvent.instance(),
        InstallRealCadpageEvent.instance(),
        DropMsgSupportEvent.instance(),
        IgnoreWarningEvent.instance());
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  private static final TextAlertWarnEvent instance = new TextAlertWarnEvent();
  public static TextAlertWarnEvent instance() {
    return instance;
  }

}
