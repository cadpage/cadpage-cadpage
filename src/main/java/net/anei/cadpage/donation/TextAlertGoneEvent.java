package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/*
    Text alert support is gone

    Due to Google Play restrictions, alerts for text dispatch messages are is not longer supported.
    If you need this capability you will either need to switch to the Cadpage Paging service or
    install a version of Cadpage from somewhere other than the Google Play Store
 */
public class TextAlertGoneEvent extends DonateScreenEvent {

  private TextAlertGoneEvent() {
    super(AlertStatus.RED, R.string.donate_text_alert_gone_title, R.string.donate_text_alert_gone_text,
        HelpCadpagePagingRegisterPromptEvent.instance(),
        InstallRealCadpageEvent.instance(),
        DropMsgSupportEvent.instance());
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  private static final TextAlertGoneEvent instance = new TextAlertGoneEvent();
  public static TextAlertGoneEvent instance() {
    return instance;
  }

}
