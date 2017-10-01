package net.anei.cadpage.donation;

import android.app.Activity;
import net.anei.cadpage.EmailDeveloperActivity;
import net.anei.cadpage.R;

/**
 * Generate an email to the developers
 */
public class EmailWrongUserEvent extends AccountScreenEvent {
  
  public EmailWrongUserEvent() {
    super(null, R.string.donate_email_title,
        new AllowAcctPermissionAction() {
          @Override
          public void doEvent(Activity activity) {
            EmailDeveloperActivity.sendWrongUserEmail(activity);
          }
        });
  }

  private static final EmailWrongUserEvent instance = new EmailWrongUserEvent();
  
  public static EmailWrongUserEvent instance() {
    return instance;
  }

}
