package net.anei.cadpage;

import android.content.Context;
import android.os.Bundle;

import net.anei.cadpage.parsers.ParserList;
import net.anei.cadpage.preferences.LocationListPreference;
import net.anei.cadpage.preferences.LocationManager;
import net.anei.cadpage.preferences.LocationMultiSelectListPreference;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

public class PreferenceLocationMenuFragment extends PreferenceFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    Bundle args = getArguments();
    boolean multi = args != null && args.getBoolean("multi");

    Fragment parent = getTargetFragment();
    final LocationManager locMgr =
      parent instanceof LocationManager.Provider
        ? ((LocationManager.Provider) parent).getLocationManager()
        : new LocationManager();

    // Set up the location preference screen
    Preference main = buildLocationItem(ParserList.MASTER_LIST, multi, locMgr);
    if (!(main instanceof PreferenceScreen)) {
      throw new RuntimeException("Location menu main screen is not a PreferenceScreen");
    }
    setPreferenceScreen((PreferenceScreen)main);
  }

  /**
   * Construct a preference item corresponding to a single parser entry
   * @param category root preference category
   * @param multi true if we are setting up multiple location selection menu
   * @param locMgr location manager
   * @return constructed preference
   */
  private Preference buildLocationItem(ParserList.ParserCategory category, boolean multi, LocationManager locMgr) {

    PreferenceManager prefMgr = getPreferenceManager();
    Context context = prefMgr.getContext();

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
      PreferenceScreen screen = prefMgr.createPreferenceScreen(context);
      screen.setTitle(catName);
      for (ParserList.ParserEntry entry : category.getParserList()) {
        if (!entry.isCategory()) throw new RuntimeException("Top level parser entry " + entry.getParserName() + " must be a category");
        Preference pref = buildLocationItem(entry.getCategory(), multi, locMgr);
        screen.addPreference(pref);
      }
      return screen;
    }

    // Otherwise we are handing a parser list
    // First step is to build a list of values and entry names
    String[] values = new String[entries.length];
    String[] names = new String[entries.length];
    for (int ndx = 0; ndx < entries.length; ndx++) {
      ParserList.ParserEntry entry = entries[ndx];
      values[ndx] = entry.getParserName();
      names[ndx] = stripStateAbbrv(entry.getLocName());
    }

    // Also need to make up a key name.  We don't care what it is, but there has to be one
    String key = catName.replace(",", "").replace(' ', '_');

    // And use that to construct a multi-select list or a regular single select list
    if (multi) {
      return new LocationMultiSelectListPreference(context, locMgr, key, catName, values, names);
    } else {
      return new LocationListPreference(context, this, locMgr, key, catName, values, names);
    }
  }

  private static String stripStateAbbrv(String name) {
    int pt = name.indexOf(',');
    if (pt >= 0) name = name.substring(0,pt);
    return name;
  }

  public void requestClose() {
    FragmentActivity activity = getActivity();
    if (activity == null) return;
    FragmentManager fragMgr = activity.getSupportFragmentManager();
    if (fragMgr.getBackStackEntryCount() == 0) {
      activity.finish();
    } else {
      fragMgr.popBackStack();
    }
  }
}
