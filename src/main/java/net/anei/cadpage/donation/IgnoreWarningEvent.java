package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;

/**
 * Ignore this warning
 */
public class IgnoreWarningEvent extends DonateEvent {

  public IgnoreWarningEvent() {
    super(null, R.string.donate_ignore_warning_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    closeEvents(activity);
  }
  
  private static final IgnoreWarningEvent instance = new IgnoreWarningEvent();
  
  public static IgnoreWarningEvent instance() {
    return instance;
  }

}
