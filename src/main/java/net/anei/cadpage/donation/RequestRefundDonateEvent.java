package net.anei.cadpage.donation;

import net.anei.cadpage.R;

/*
Please refund my last Cadpage purchase

No problem.  Our policy is to issue full refunds for anyone requesting them within 3 months.
Clicking the button below will send us an email requesting your purchase refund.
 */
public class RequestRefundDonateEvent extends DonateScreenEvent {

  protected RequestRefundDonateEvent() {
    super(AlertStatus.GREEN, R.string.donate_request_refund_title, R.string.donate_request_refund_text,
           EmailRequestRefundEvent.instance());
  }

  @Override
  public boolean isEnabled() {
    return DonationManager.instance().isRefundPrompt();
  }

  private static final RequestRefundDonateEvent instance = new RequestRefundDonateEvent();
  public static RequestRefundDonateEvent instance() {
    return instance;
  }

}
