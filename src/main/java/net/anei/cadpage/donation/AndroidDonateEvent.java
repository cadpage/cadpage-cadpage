package net.anei.cadpage.donation;

/**
 * Purchase through Google Play Store
 */
public class AndroidDonateEvent extends EventGroup {
  
  private AndroidDonateEvent() {
    super(AndroidDonateConfirmEvent.instance(), AndroidDonate1Event.instance());
  }

  private static final AndroidDonateEvent instance = new AndroidDonateEvent();
  
  public static AndroidDonateEvent instance() {
    return instance;
  }

}
