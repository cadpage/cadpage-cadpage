package net.anei.cadpage.preferences;

import net.anei.cadpage.ManageNotification;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.SmsMessageQueue;
import net.anei.cadpage.SmsMmsMessage;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import androidx.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

public class TestNotificationDialogPreference extends Preference {
  private final Context context;

  public TestNotificationDialogPreference(Context _context, AttributeSet attrs) {
    super(_context, attrs);
    context = _context;
  }

  public TestNotificationDialogPreference(Context _context, AttributeSet attrs, int defStyle) {
    super(_context, attrs, defStyle);
    context = _context;
  }

  @Override
  protected void onClick() {

    // Show notification
    SmsMmsMessage msg = SmsMessageQueue.getInstance().getMessage(0);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ManagePreferences.popupEnabled()) {
      ManageNotification.show(context, msg, true, true);
    } else {
      ManageNotification.show(context, msg);
    }
  }
}
