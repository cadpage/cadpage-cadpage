package net.anei.cadpage.billing;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import net.anei.cadpage.HttpService;
import net.anei.cadpage.Log;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.donation.UserAcctManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

class AuthServerBilling extends Billing {

  @Override
  public void initialize(Context context) {

    // We do not have any initialization to do
    // but it would be a good idea to declare a functional status
    setStatus(BillingStatus.OK);
  }

  @Override
  public void initialize(Activity activity) {}

  /**
   * Shutdown billing manager
   */
  @Override
  public void destroy() {}

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  void doConnect() {
    setStatus(BillingStatus.OK);
  }

  @Override
  void doRestoreTransactions(Context context) {

    // First ask for permission to recover the user phone number
    ManagePreferences.checkPermPhoneInfo((ok, permissions, granted) -> {

      // Next build query with all of the possible account and phone ID's
      Uri.Builder builder = Uri.parse(context.getString(R.string.donate_server_url)).buildUpon();
      String acct = ManagePreferences.billingAccount();
      if (acct != null) {
        builder.appendQueryParameter("id", acct);
      }
      builder.appendQueryParameter("id", UserAcctManager.instance().getPhoneNumber(context));

      // Send it to the server and see what comes back
      HttpService.addHttpRequest(context, new HttpService.HttpRequest(builder.build(), true) {

        @Override
        public void processBody(String body) {
          DonationCalculator calc = new DonationCalculator(2);
          for (String line : body.split("<br>")) {

            String[] flds = line.split(",");
            if (flds.length < 2) continue;
            String stat = flds[1].trim();
            if (!STATUS_PTN.matcher(stat).matches()) {
              Log.e("Invalid status:" + stat);
              return;
            }
            String purchaseDate = null;
            if (flds.length >= 3) {
              purchaseDate = flds[2].trim();
              if (purchaseDate.length() > 0) {
                try {
                  DATE_FMT.parse(purchaseDate);
                } catch (ParseException ex) {
                  Log.e(ex);
                  return;
                }
              }
              purchaseDate = purchaseDate.replace("/", "");
            }
            String sponsor = (flds.length >= 4 ? flds[3].trim() : null);
            calc.subscription(stat, purchaseDate, sponsor, 0);
          }
          calc.save();
        }
      });
    }, R.string.perm_acct_info_for_manual_recalc);
  }

  private static final Pattern STATUS_PTN = Pattern.compile("LIFE|\\d{4}", Pattern.CASE_INSENSITIVE);
  private static final DateFormat DATE_FMT = new SimpleDateFormat("MM/dd/yyyy");

  @Override
  void doStartPurchase(BillingActivity activity) {
    throw new RuntimeException("AuthServerBilling does not support purchases");
  }
}
