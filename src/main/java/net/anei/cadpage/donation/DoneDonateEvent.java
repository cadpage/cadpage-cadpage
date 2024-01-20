package net.anei.cadpage.donation;

import android.app.Activity;
import net.anei.cadpage.R;

/**
 * Base event that displays a "Done" button, and closes the donation menu 
 * tree when pressed
 */
public class DoneDonateEvent extends DonateEvent {
  
  public DoneDonateEvent() {
    super(null, R.string.donate_btn_done);
  }

  @Override
  protected void doEvent(Activity activity) {
    done = true;
    closeEvents(activity);
  }
  
  private static final DoneDonateEvent instance = new DoneDonateEvent();
  
  public static DoneDonateEvent instance() {
    return instance;
  }

  private static boolean done = false;

  public static void clearDone() {
    done = false;
  }

  public static boolean isDone() {
    return done;
  }

}
