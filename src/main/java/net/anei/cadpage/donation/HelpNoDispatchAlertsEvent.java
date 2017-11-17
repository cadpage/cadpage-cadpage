package net.anei.cadpage.donation;

import net.anei.cadpage.R;

/*

No dispatch alerts

Cadpage will not do anything for you if you are not receiving dispatch alerts.
If you are part of an emergency response organization, talk to your department's IT people and
see if they can send you the department's dispatch alerts.

If you are not part of an emergency response organization, Cadpage will not be useful to you.

If you have any questions or suggestions as to how Cadpage might be more useful, feel free to use
the "Email the Developer" setting to contact us.

 */
public class HelpNoDispatchAlertsEvent extends DonateScreenEvent {

  protected HelpNoDispatchAlertsEvent() {
    super(R.string.help_no_dispatch_alerts_title, R.string.help_no_dispatch_alerts_title, R.string.help_no_dispatch_alerts_text,
          EmailHelpEvent.instance(),
          DoneDonateEvent.instance());
  }

  private static final HelpNoDispatchAlertsEvent instance = new HelpNoDispatchAlertsEvent();
  public static HelpNoDispatchAlertsEvent instance() {
    return instance;
  }

}
