package net.anei.cadpage.donation;


/**
 * Group of events presented when we we are looking for money
 */
public class ReqMoneyGroup extends EventGroup {
  
  private ReqMoneyGroup() {
    super(VendorEvent.instance(2),
          AndroidDonateEvent.instance(),
          DonateAndroidSuppressedEvent.instance(),
          DonateAndroidUnsuportedEvent.instance(),
          AndroidDonateProblemEvent.instance(),
          DonateResetMarketEvent.instance(),
          WrongUserDonateEvent.instance(),
          InactiveSponsorDonateEvent.instance(),
          SobStoryDonateEvent.instance());
  }
  
  private static final ReqMoneyGroup instance = new ReqMoneyGroup();
  
  public static ReqMoneyGroup instance() {
    return instance;
  }

}
