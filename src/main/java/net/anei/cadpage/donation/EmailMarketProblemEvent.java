package net.anei.cadpage.donation;

import android.app.Activity;
import net.anei.cadpage.EmailDeveloperActivity;
import net.anei.cadpage.R;

/**
 * Generate an email to the developers
 */
public class EmailMarketProblemEvent extends AccountScreenEvent {
  
  public EmailMarketProblemEvent() {
    super(null, R.string.donate_email_title);
  }

  @Override
  void doAccountPermissionApproved(Activity activity) {
    EmailDeveloperActivity.sendMarketProblemEmail(activity);
  }

  private static final EmailMarketProblemEvent instance = new EmailMarketProblemEvent();
  
  public static EmailMarketProblemEvent instance() {
    return instance;
  }
}
