package net.anei.cadpage;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;
import androidx.preference.Preference;

import net.anei.cadpage.donation.DeveloperToolsManager;
import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.preferences.BillingAccountPreference;

import static android.app.Activity.RESULT_OK;

public class PreferenceGeneralFragment extends PreferenceFragment {

  private static final int BILLING_ACCT_REQ = 99991;

  private BillingAccountPreference mBillingAccountPreference;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_general, rootKey);

    mBillingAccountPreference = findPreference(getString(R.string.pref_billing_account_key));

    // Email developer response
    Preference emailPref = findPreference(getString(R.string.pref_email_key));
    assert emailPref != null;
    emailPref.setOnPreferenceClickListener(preference -> {
      EmailDeveloperActivity.sendGeneralEmail(getActivity());
      return true;
    });
  }

  @Override
  public void onResume() {
    super.onResume();

    // And might have been changed by deep submenus
    mBillingAccountPreference.setText(ManagePreferences.billingAccount());
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);

    if (requestCode == BILLING_ACCT_REQ && resultCode == RESULT_OK) {
      mBillingAccountPreference.setText(intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
    }
  }
}
