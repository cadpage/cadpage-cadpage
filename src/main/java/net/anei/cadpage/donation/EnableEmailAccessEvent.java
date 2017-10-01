package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;


public class EnableEmailAccessEvent extends DonateScreenEvent {

  private EnableEmailAccessEvent() {
    super(AlertStatus.YELLOW, R.string.donate_enable_email_access_title, R.string.donate_enable_email_access_text);
    setEvents(new AccountPermApprovedEvent(null),
              AccountPermDeniedEvent.instance()
    );
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  private static final EnableEmailAccessEvent instance = new EnableEmailAccessEvent();
  public static EnableEmailAccessEvent instance() {
    return instance;
  }
}
