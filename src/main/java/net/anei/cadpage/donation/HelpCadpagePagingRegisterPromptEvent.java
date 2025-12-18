package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/*
    Register with Cadpage paging service

    Due to Google Play restrictions, alerts for text dispatch messages are is not longer supported.
    If you need this capability you will either need to switch to the Cadpage Paging service or
    install a version of Cadpage from somewhere other than the Google Play Store
 */
public class HelpCadpagePagingRegisterPromptEvent extends DonateScreenEvent {

  private HelpCadpagePagingRegisterPromptEvent() {
    super(AlertStatus.GREEN, R.string.help_cadpage_paging_register_title, R.string.help_cadpage_paging_register_text,
        HelpCadpagePagingRegisterEvent.instance());
  }

  @Override
  public boolean isEnabled() {
    return !VendorManager.instance().isRegistered("Cadpage");
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  private static final HelpCadpagePagingRegisterPromptEvent instance = new HelpCadpagePagingRegisterPromptEvent();
  public static HelpCadpagePagingRegisterPromptEvent instance() {
    return instance;
  }

}
