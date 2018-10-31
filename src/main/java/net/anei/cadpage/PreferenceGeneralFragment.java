package net.anei.cadpage;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.TwoStatePreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import net.anei.cadpage.donation.DeveloperToolsManager;
import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.preferences.BillingAccountPreference;

import static android.app.Activity.RESULT_OK;

public class PreferenceGeneralFragment extends PreferenceFragment {

  private static final int BILLING_ACCT_REQ = 99991;

  private TwoStatePreference mEnabledPreference;
  private BillingAccountPreference mBillingAccountPreference;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preference_general);

    // Save specific preferences we might need later
    mEnabledPreference = (TwoStatePreference) findPreference(getString(R.string.pref_enabled_key));

    // Set up the payment status tracking screens
    Preference donate = findPreference(getString(R.string.pref_payment_status_key));
    MainDonateEvent.instance().setPreference(getActivity(), donate);

    mBillingAccountPreference = (BillingAccountPreference)findPreference(getString(R.string.pref_billing_account_key));

    // Email developer response
    Preference emailPref = findPreference(getString(R.string.pref_email_key));
    emailPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
      @Override
      public boolean onPreferenceClick(Preference preference) {
        EmailDeveloperActivity.sendGeneralEmail(getActivity());
        return true;
      }});

    // Add developer dialog preference if appropriate
    DeveloperToolsManager.instance().addPreference(getActivity(), getPreferenceScreen());
  }

  @Override
  public void onResume() {
    super.onResume();

    // Check for changes to values that are accessable from the widget
    mEnabledPreference.setChecked(ManagePreferences.enabled());

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

  @Override
  public void onDestroy() {
    MainDonateEvent.instance().setPreference(null, null);
    super.onDestroy();
  }
}