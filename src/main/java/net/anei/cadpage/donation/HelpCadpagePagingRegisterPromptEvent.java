package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.Intent;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;
import net.anei.cadpage.vendors.VendorManager;

/*
    Register with Cadpage paging service

    OK.  Once you register with this service it will assign you a dispatch email address.  You
    need to have your dispatch alerts sent to this email address.  From there they will be
    forwarded to Cadpage.  A paid subscription will be required, but you should get a free 30
    demo rate to try it out.
 */
public class HelpCadpagePagingRegisterPromptEvent extends DonateScreenEvent {

  private HelpCadpagePagingRegisterPromptEvent() {
    super(AlertStatus.GREEN, R.string.help_cadpage_paging_register_title, R.string.help_cadpage_paging_register_text,
        HelpCadpagePagingRegisterEvent.instance());
  }

  @Override
  public boolean isEnabled() {
    return VendorManager.instance().isCadpageAvailable();
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  SmsMmsMessage msg;

  @Override
  public void create(Activity activity, SmsMmsMessage msg) {

    this.msg = msg;

    // If Cadpage paging is already registered, we want to switch to the regular text processing menu
    // Otherwise process normally
    if (VendorManager.instance().isLocationRequired()) {
      ((DonateActivity)activity).switchEvent(HelpTextDispatchEvent.instance(), msg);
    } else {
      super.create(activity, msg);
    }
  }

  @Override
  public void onRestart(DonateActivity activity) {
    super.onRestart(activity);
    if (VendorManager.instance().isLocationRequired()) {
      activity.switchEvent(HelpTextDispatchEvent.instance(), msg);
    }
  }

  @Override
  public boolean followup(Activity activity, int req, int result, Intent data) {
    if (VendorManager.instance().isLocationRequired()) {
      HelpCadpageReadyEvent.instance().launch(activity);
    }
    return true;
  }

  private static final HelpCadpagePagingRegisterPromptEvent instance = new HelpCadpagePagingRegisterPromptEvent();
  public static HelpCadpagePagingRegisterPromptEvent instance() {
    return instance;
  }

}
