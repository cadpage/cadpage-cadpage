package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.CallHistoryActivity;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.PermissionManager;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;

/*
Welcome to Cadpage!

Cadpage assists emergency first responders in getting dispatched emergency scene.
Cadpage will not do anything with your current settings.

Cadpage only works if you belong to a fire department or other emergency response organization
that is sending alerts to this device when a call is dispatched.  Alerts can be sent as text
messages, or if your department uses CodeMessaging or Active911 as a message service, as direct pages.
 */
public class HelpWelcomeEvent extends DonateScreenEvent {

  protected HelpWelcomeEvent() {
    super(R.string.help_welcome_title, R.string.help_welcome_title, R.string.help_welcome_text,
          new HelpTextDispatchEvent());
  }

  @Override
  public boolean isEnabled() {

    // Only enabled if Cadpage is not functional
    return !ManagePreferences.isFunctional();
  }

  @Override
  protected Object[] getTextParms(Activity activity, int type) {
    String text = activity.getString(CallHistoryActivity.isInitializing() ? R.string.help_welcome_1_text : R.string.help_welcome_2_text);
    return new Object[]{ text };
  }

  private static final HelpWelcomeEvent instance = new HelpWelcomeEvent();
  public static HelpWelcomeEvent instance() {
    return instance;
  }

}
