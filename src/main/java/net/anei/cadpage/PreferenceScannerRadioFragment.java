package net.anei.cadpage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.Preference;

public class PreferenceScannerRadioFragment extends PreferenceFragment {

  private static final int REQ_SCANNER_CHANNEL = 1;

  private Preference scannerPref;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_scanner_radio, rootKey);

    // Set up Scanner channel selection preference
    scannerPref = findPreference(getString(R.string.pref_scanner_channel_key));
    if (scannerPref != null) {
      String channel = ManagePreferences.scannerChannel();
      scannerPref.setSummary(channel);
      scannerPref.setOnPreferenceClickListener(pref1 -> {

        // When clicked, ask the scanner app to select a favorite channel
        Intent intent = new Intent("com.scannerradio.intent.action.ACTION_PICK");
        try {
          startActivityForResult(intent, REQ_SCANNER_CHANNEL);
        } catch (Exception ex) {

          if (! (ex instanceof ActivityNotFoundException)) Log.e(ex);

          // Scanner radio either isn't installed, or isn't responding to the ACTION_PICK
          // request.  Check the package manager to which, if any, are currently installed
          Activity activity = getActivity();
          assert activity != null;
          PackageManager pkgMgr = activity.getPackageManager();
          String pkgName = "com.scannerradio_pro";
          boolean installed = false;
          try {
            pkgMgr.getPackageInfo(pkgName, 0);
            installed = true;
          } catch (PackageManager.NameNotFoundException ignored) {}
          if (! installed) {
            pkgName = "com.scannerradio";
            try {
              pkgMgr.getPackageInfo(pkgName, 0);
              installed = true;
            } catch (PackageManager.NameNotFoundException ignored) {}
          }

          // OK, show a dialog box asking if they want to install Scanner Radio
          final String pkgName2 = pkgName;
          new AlertDialog.Builder(getActivity())
            .setMessage(installed ? R.string.scanner_not_current : R.string.scanner_not_installed)
            .setPositiveButton(R.string.donate_btn_yes, (dialog, which) -> {
              Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkgName2));
              intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              try {
                getActivity().startActivity(intent1);
              } catch (ActivityNotFoundException ex1) {
                Log.e(ex1);
              }
            })
            .setNegativeButton(R.string.donate_btn_no, null)
            .create().show();

        }
        return true;
      });
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (requestCode == REQ_SCANNER_CHANNEL) {
      if (resultCode != Activity.RESULT_OK || data == null) return;
      Log.v("onActivityResult()");
      ContentQuery.dumpIntent(data);
      String description = data.getStringExtra("description");
      Intent scanIntent = data.getParcelableExtra("playIntent");
      if (description == null || scanIntent == null) return;
      ContentQuery.dumpIntent(scanIntent);

      ManagePreferences.setScannerChannel(description);
      scannerPref.setSummary(description);
      ManagePreferences.setScannerIntent(scanIntent);
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }
}
