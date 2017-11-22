package net.anei.cadpage.donation;

import net.anei.cadpage.R;

/*

CodeMessaging alerts

You need to register Cadpage with the CodeMessaging service.  The process is pretty much self-explanatory.
The only tricky part is that you have to answer "YES" when you are asked if you
are receiving CodeMessaging alerts.

 */
public class HelpCodeMessagingEvent extends DonateScreenEvent {

  protected HelpCodeMessagingEvent() {
    super(R.string.help_codemessaging_title, R.string.help_codemessaging_title, R.string.help_codemessaging_text,
          HelpCodeMessagingRegisterEvent.instance(),
          DoneDonateEvent.instance());
  }

  private static final HelpCodeMessagingEvent instance = new HelpCodeMessagingEvent();
  public static HelpCodeMessagingEvent instance() {
    return instance;
  }

}
