package net.anei.cadpage.donation;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

/*
Hey, I've already paid for this

Our sincere apologies.  Cadpage tracks you by your Google Market account
"which is %s. If this doesn't match the Paypal account you used to donate"
to us, we will need some help figuring out who you are. Just clicking the
button below will send us an email that will hopefully clear things up.
 */
public class WrongUserDonateEvent extends DonateScreenEvent {

  protected WrongUserDonateEvent() {
    super(AlertStatus.YELLOW, R.string.donate_wrong_user_title, R.string.donate_wrong_user_text,
           EmailWrongUserEvent.instance());
  }

  private static final WrongUserDonateEvent instance = new WrongUserDonateEvent();
  public static WrongUserDonateEvent instance() {
    return instance;
  }

}
