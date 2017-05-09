package net.anei.cadpage.donation;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import net.anei.cadpage.CallHistoryActivity;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.MsgOptionManager;
import net.anei.cadpage.R;

/**
 Launch Active911 app
 */
public class Active911WarnA911Event extends DonateEvent {

  public Active911WarnA911Event() {
    super(null, R.string.active911_warn_A911_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    MsgOptionManager.launchActive911(activity, true);
  }
  
  private static final Active911WarnA911Event instance = new Active911WarnA911Event();
  
  public static Active911WarnA911Event instance() {
    return instance;
  }

}
