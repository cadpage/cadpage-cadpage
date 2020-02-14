package net.anei.cadpage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.TwoStatePreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.parsers.ManageParsers;
import net.anei.cadpage.parsers.MsgParser;
import net.anei.cadpage.parsers.ParserList;
import net.anei.cadpage.parsers.SplitMsgOptions;
import net.anei.cadpage.preferences.EditTextPreference;
import net.anei.cadpage.preferences.LocationSwitchPreference;
import net.anei.cadpage.preferences.LocationListPreference;
import net.anei.cadpage.preferences.LocationManager;
import net.anei.cadpage.preferences.OnDialogClosedListener;

public class PreferenceLocationFragment extends PreferenceRestorableFragment {

  private static final int REQ_SCANNER_CHANNEL = 1;

  private String parserDefCity = "";
  private String parserDefState = "";
  private TwoStatePreference overrideFilterPref;
  private net.anei.cadpage.preferences.EditTextPreference filterPref;

  private TwoStatePreference overrideDefaultPref;
  private EditTextPreference defCityPref;
  private EditTextPreference defStatePref;

  private Preference scannerPref;
  private String saveLocation;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preference_location);

      // Add necessary permission checks
    Preference pref = findPreference(getString(R.string.pref_enable_msg_type_key));
    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        return ManagePreferences.checkPermEnableMsgType((ListPreference)preference, (String)newValue);
      }
    });

    // Save location so we can tell when it changes
    saveLocation = ManagePreferences.location();

    // Set up the two location preference screens
    Preference descPreference = findPreference(getString(R.string.pref_loc_desc_key));
    LocationManager locMgr = new LocationManager(getActivity(), descPreference);
    setupLocationMenu(R.string.pref_location_tree_key, false, locMgr);
    setupLocationMenu(R.string.pref_location_mtree_key, true, locMgr);
    locMgr.updateDisplay();

    // The location, filter override checkbox, and filter edit box have a complex
    // relationship.  The override checkbox is enabled only when the location parser
    // has a default parser to override.  If it doesn't then it is disabled by forced
    // to true.  The filter is enabled when the override box is checked, whether it
    // is enabled or not.  We have to do this ourselves because the Android dependency
    // logic considers the value to be false if it isn't enabled.

    // On top of all that, the general alert box is enabled only if the current
    // parser has a default filter OR a user filter has been specified

    filterPref = (net.anei.cadpage.preferences.EditTextPreference)
        findPreference(getString(R.string.pref_filter_key));
    filterPref.setDialogClosedListener(new OnDialogClosedListener(){
      @Override
      public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
          if ("General".equals(saveLocation)) {
            DonationManager.instance().reset();
            MainDonateEvent.instance().refreshStatus();
          }
        }
      }
    });

    overrideFilterPref = (TwoStatePreference)
        findPreference(getString(R.string.pref_override_filter_key));
    filterPref.setEnabled(overrideFilterPref.isChecked());
    overrideFilterPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        filterPref.setEnabled((Boolean)newValue);
        return true;
      }
    });

    adjustLocationChange(ManagePreferences.location(), false);
    locMgr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        adjustLocationChange((String)newValue, true);
        return true;
      }
    });

    // Have to play some games with the override default settings
    // If the override defaults is turned on, enable the default city and state items
    // If it is turned off, force the default city and state to the current parser
    // defaults and disable them.
    overrideDefaultPref = (TwoStatePreference)
        findPreference(getString(R.string.pref_override_default_key));
    defCityPref = (EditTextPreference)
        findPreference(getString(R.string.pref_defcity_key));
    defStatePref = (EditTextPreference)
        findPreference(getString(R.string.pref_defstate_key));

    overrideDefaultPref = (TwoStatePreference)
        findPreference(getString(R.string.pref_override_default_key));
    overrideDefaultPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        if (! value) {
          defCityPref.setText(parserDefCity);
          defCityPref.refreshSummary();
          defStatePref.setText(parserDefState);
          defStatePref.refreshSummary();
        }
        defCityPref.setEnabled(value);
        defStatePref.setEnabled(value);
        return true;
      }});

    // Make an initial call to our preference change listener to set up the
    // correct default summary displays
    overrideDefaultPref.getOnPreferenceChangeListener().
        onPreferenceChange(overrideDefaultPref, ManagePreferences.overrideDefaults());

    // Set up Scanner channel selection preference
    scannerPref = findPreference(getString(R.string.pref_scanner_channel_key));
    if (scannerPref != null) {
      String channel = ManagePreferences.scannerChannel();
      scannerPref.setSummary(channel);
      scannerPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
        @Override
        public boolean onPreferenceClick(Preference pref) {

          // When clicked, ask the scanner app to select a favorite channel
          Intent intent = new Intent("com.scannerradio.intent.action.ACTION_PICK");
          try {
            startActivityForResult(intent, REQ_SCANNER_CHANNEL);
          } catch (Exception ex) {

            if (! (ex instanceof ActivityNotFoundException)) Log.e(ex);

            // Scanner radio either isn't installed, or isn't responding to the ACTION_PICK
            // request.  Check the package manager to which, if any, are currently installed
            PackageManager pkgMgr = getActivity().getPackageManager();
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
                .setPositiveButton(R.string.donate_btn_yes, new DialogInterface.OnClickListener(){
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkgName2));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                      getActivity().startActivity(intent);
                    } catch (ActivityNotFoundException ex) {
                      Log.e(ex);
                    }
                  }
                })
                .setNegativeButton(R.string.donate_btn_no, null)
                .create().show();

          }
          return true;
        }
      });
    }

    //  OK, everything should be set up
    // If we were supposed to launch a specific preference screen,
    // find it and substitute it for the top level preference screen
    Bundle args = getArguments();
    if (args != null) {
      int id = args.getInt(SmsPopupConfigActivity.EXTRA_PREFERENCE, -1);
      if (id >= 0) {
        Preference preference = findPreference(getString(id));if (preference != null) {
          setPreferenceScreen((PreferenceScreen) preference);
        }
      }
    }
  }

  @Override
  public void onPause() {
    Log.v("PreferenceLocationFragment.onPause()");
    super.onPause();
  }

  /**
   * Set up location menu tree
   * @param resId resource ID of the preference screen to be constructed
   * @param multi true if we are building a multi-location preference tree
   * false if we are building a normal single location preference tree
   */
  private void setupLocationMenu(int resId, boolean multi, LocationManager locMgr) {

    // Get the preference screen we will be building
    Resources res = getResources();
    PreferenceScreen main = (PreferenceScreen)findPreference(res.getString(resId));
    buildLocationMenu(ParserList.MASTER_LIST, main, multi, locMgr);
  }

  /**
   * Construct a preference screen corresponding to a configured parser category
   * @param parserCategory parser category
   * @param screen preference screen being set up
   * @param multi true if we are setting up a multiple location selection menu
   * @param locMgr Location manager
   */
  private void buildLocationMenu(ParserList.ParserCategory parserCategory, PreferenceScreen screen, boolean multi, LocationManager locMgr) {
    for (ParserList.ParserEntry entry : parserCategory.getParserList()) {
      if (!entry.isCategory()) throw new RuntimeException("Top level parser entry " + entry.getParserName() + " must be a category");
      Preference pref = buildLocationItem(entry.getCategory(), screen, multi, locMgr);
      screen.addPreference(pref);
    }
  }

  /**
   * Construct a preference item corresponding to a single parser entry
   * @param category root preference category
   * @param parent parent preference screen
   * @param multi true if we are setting up multiple location selection menu
   * @param locMgr location manager
   * @return constructed preference
   */
  private Preference buildLocationItem(ParserList.ParserCategory category, PreferenceScreen parent, boolean multi, LocationManager locMgr) {

    // Current rules are that category must contain only  subcategory or only parser entries.  See which this is
    String catName = category.getName();
    ParserList.ParserEntry[] entries = category.getParserList();
    boolean subcat = false;
    boolean plist = false;
    for (ParserList.ParserEntry entry : entries) {
      if (entry.isCategory()) subcat = true;
      else plist = true;
    }
    if (subcat && plist) throw new RuntimeException("Parser group " + catName + " mixes parser and category entries");
    if (!subcat && !plist) throw new RuntimeException("Parser group " + catName + " is empty");

    // If it only contains subcategories, build a new preference screen with them
    if (subcat) {
      PreferenceScreen sub = getPreferenceManager().createPreferenceScreen(getActivity());
      sub.setTitle(catName);
      buildLocationMenu(category, sub, multi, locMgr);
      return sub;
    }

    // Otherwise we are handing a parser list
    // What we do next depends on whether this is a single or multiple selection menu

    // If we are doing multiple selections, create a new preference screen and fill it
    // a location checkbox for each parser entry
    if (multi) {
      PreferenceScreen sub = getPreferenceManager().createPreferenceScreen(getActivity());
      sub.setTitle(catName);
      for (ParserList.ParserEntry entry : entries) {
        sub.addPreference(
            new LocationSwitchPreference(getActivity(), entry.getParserName(),
                stripStateAbbrv(entry.getLocName()),
                locMgr)
        );
      }
      return sub;
    }

    // If we are doing single location selections, build a list preference
    // that can select from any of the parsers in this category
    LocationListPreference list = new LocationListPreference(getActivity(), locMgr, parent);
    list.setTitle(catName);
    list.setDialogTitle(catName);

    String[] values = new String[entries.length];
    String[] names = new String[entries.length];
    for (int ndx = 0; ndx < entries.length; ndx++) {
      ParserList.ParserEntry entry = entries[ndx];
      values[ndx] = entry.getParserName();
      names[ndx] = stripStateAbbrv(entry.getLocName());
    }
    list.setEntryValues(values);
    list.setEntries(names);
    return list;
  }

  private static String stripStateAbbrv(String name) {
    int pt = name.indexOf(',');
    if (pt >= 0) name = name.substring(0,pt);
    return name;
  }

  /**
   * Make any necessary adjustments necessary
   * when the location preference is changed
   * @param location new location preference value
   * @param change true if location value has been changed
   */
  private void adjustLocationChange(String location, boolean change) {

    // If location changes, recalculate the donation status
    if (!location.equals(saveLocation)) {
      saveLocation = location;
      DonationManager.instance().reset();
      MainDonateEvent.instance().refreshStatus();
    }

    // Get the parser and see if it has a default filter
    // Save it in parserFilter so other preferences know what it is
    MsgParser parser = ManageParsers.getInstance().getParser(location);
    String parserFilter = parser.getFilter();
    parserDefCity = parser.getDefaultCity();
    parserDefState = parser.getDefaultState();

    // If the parser has a filter, enable the override checkbox, set its value to true
    // And insert the default filter value in the summary off message
    // And unilaterally enable the general alert box
    if (parserFilter.length() > 0) {
      overrideFilterPref.setEnabled(true);
      if (change) overrideFilterPref.setChecked(false);
      overrideFilterPref.setSummaryOff(getString(R.string.pref_override_filter_summaryoff, parserFilter));
      filterPref.setEnabled(overrideFilterPref.isChecked());
    }

    // If there is no parser filter, the override box is disabled but forced to true
    // the general alert box is enabled only if the user filter
    else {
      overrideFilterPref.setEnabled(false);
      overrideFilterPref.setChecked(true);
      filterPref.setEnabled(true);
    }

    // Any time the location parser changes, reset the override default loc setting
    // OK, its a little more complicated than that.  If the override default setting
    // is on, we can just turn it off and let the onPreferenceChange listener take
    // care of things.  If it is already off, we have to call the onPreferenceChange
    // listener ourselves so it will update the property summary displays
    // If that isn't complicated enough, the overrideDefaultPref setting won't
    // be initialized the first time we are called, but that first call has the
    // change parameter set to false, so this logic gets skipped.
    if (change) {
      if (overrideDefaultPref.isChecked()) {
        overrideDefaultPref.setChecked(false);
      } else {
        overrideDefaultPref.getOnPreferenceChangeListener().onPreferenceChange(overrideDefaultPref, false);
      }
    }
  }

  // If location code changes during this session, force a rebuild of
  // the call history data on the off chance that a general format message
  // can use the new location code.
  private String oldLocation = null;
  private boolean oldSplitBlank = false;
  private boolean oldSplitKeepLeadBreak = false;
  private boolean oldRevMsgOrder = false;
  private boolean oldMixedMsgOrder = false;


  @Override
  public void onStart() {

    // Save the setting that might be important if they change
    oldLocation = ManagePreferences.location();

    SplitMsgOptions options = ManagePreferences.getDefaultSplitMsgOptions();
    oldSplitBlank = options.splitBlankIns();
    oldSplitKeepLeadBreak = options.splitKeepLeadBreak();
    oldRevMsgOrder = options.revMsgOrder();
    oldMixedMsgOrder = options.mixedMsgOrder();

    super.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();

    // If any of the split message options have changed, reparse any possibly affected calls
    SplitMsgOptions options = ManagePreferences.getDefaultSplitMsgOptions();
    boolean splitBlank = options.splitBlankIns();
    boolean splitKeepLeadBreak = options.splitKeepLeadBreak();
    boolean revMsgOrder = options.revMsgOrder();
    boolean mixedMsgOrder = options.mixedMsgOrder();
    int changeCode;
    if (revMsgOrder != oldRevMsgOrder || mixedMsgOrder != oldMixedMsgOrder) changeCode = 3;
    else if (splitBlank != oldSplitBlank) changeCode = 2;
    else if (splitKeepLeadBreak != oldSplitKeepLeadBreak) changeCode = 1;
    else changeCode = 0;
    if (changeCode > 0) SmsMessageQueue.getInstance().splitOptionChange(changeCode);


    String location = ManagePreferences.location();
    if (!location.equals(oldLocation)) {
      SmsMessageQueue.getInstance().reparseGeneral();
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
