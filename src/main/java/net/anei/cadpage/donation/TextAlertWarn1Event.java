package net.anei.cadpage.donation;

import net.anei.cadpage.R;

/*
    Text alert support is going away

    Due to Google Play restrictions, support for processing text alerts will soon disappear.
    If you need this capability you will either need to switch to the Cadpage Paging service, or
    install a version of Cadpage from somewhere other than the Google Play Store
 */
public class TextAlertWarn1Event extends DonateScreenEvent {

  private TextAlertWarn1Event() {
    super(AlertStatus.RED, R.string.donate_text_alert_warn_title, R.string.donate_text_alert_warn1_text,
        HelpCadpagePagingRegisterPromptEvent.instance(),
        InstallRealCadpageEvent.instance(),
        DropMsgSupportEvent.instance(),
        IgnoreWarningEvent.instance());
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  private static final TextAlertWarn1Event instance = new TextAlertWarn1Event();
  public static TextAlertWarn1Event instance() {
    return instance;
  }

}
