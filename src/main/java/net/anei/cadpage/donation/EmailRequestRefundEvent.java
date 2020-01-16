package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.EmailDeveloperActivity;
import net.anei.cadpage.R;

/**
 * Generate an email to the developers
 */
public class EmailRequestRefundEvent extends AccountScreenEvent {

  public EmailRequestRefundEvent() {
    super(null, R.string.donate_email_title,
        new AllowAcctPermissionAction() {
          @Override
          public void doEvent(Activity activity) {
            EmailDeveloperActivity.sendRequestRefundEmail(activity);
          }
        });
  }

  private static final EmailRequestRefundEvent instance = new EmailRequestRefundEvent();
  
  public static EmailRequestRefundEvent instance() {
    return instance;
  }

}
