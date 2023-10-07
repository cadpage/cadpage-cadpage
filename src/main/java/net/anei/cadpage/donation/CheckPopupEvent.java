package net.anei.cadpage.donation;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ManageNotification;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

/**
 Popup Configuration Problem

 As of Android 10, the Cadpage show alarm popup window option requires that the \"Pop on screen\"
 option be enabled for the regular dispatch notification category
*/
public class CheckPopupEvent extends DonateScreenEvent {

  public CheckPopupEvent() {
    super(null, R.string.check_popup_title, R.string.check_popup_text,
          EnableNotifyPopupEvent.instance(),
          DisableCadpagePopupEvent.instance());
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return ManageNotification.checkPopupAlertConflict(CadPageApplication.getContext());
  }

  @Override
  public void onRestart(DonateActivity activity) {
    if (!isEnabled()) closeEvents(activity);
  }

  private static final CheckPopupEvent instance = new CheckPopupEvent();
  
  public static CheckPopupEvent instance() {
    return instance;
  }
}
