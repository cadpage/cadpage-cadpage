package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.EmailDeveloperActivity;
import net.anei.cadpage.R;

/**
 * Email the developer
 */
public class EmailHelpEvent extends DonateEvent {

  public EmailHelpEvent() {
    super(null, R.string.help_email_title);
  }

  public void doEvent(Activity activity) {
    EmailDeveloperActivity.sendNeedHelpEmail(activity);
    closeEvents(activity);
  }

  private static final EmailHelpEvent instance = new EmailHelpEvent();
  
  public static EmailHelpEvent instance() {
    return instance;
  }

}
