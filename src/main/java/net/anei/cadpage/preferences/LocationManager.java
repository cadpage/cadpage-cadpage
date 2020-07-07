package net.anei.cadpage.preferences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.preference.Preference;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.parsers.ManageParsers;
import net.anei.cadpage.parsers.MsgParser;

/**
 * Class coordinates everything that needs to be done about the location setting
 */
public class LocationManager {

  public interface Provider {
    LocationManager getLocationManager();
  }

  private final List<String> locationList = new ArrayList<>();
  private final List<String> nameList = new ArrayList<>();

  private String saveLocation;
  private MsgParser parser = null;

  private static final Map<String,String> STATE_MAP = new HashMap<>();
  static {
    STATE_MAP.put("AL", "ALABAMA");
    STATE_MAP.put("AK", "ALASKA");
    
    STATE_MAP.put("AZ", "ARIZONA");
    STATE_MAP.put("AR", "ARKANSAS");
    
    STATE_MAP.put("ID", "IDAHO");
    STATE_MAP.put("IL", "ILLINOIS");
    STATE_MAP.put("IN", "INDIANA");
    STATE_MAP.put("IA", "IOWA");
    
    STATE_MAP.put("ME", "MAINE");
    STATE_MAP.put("MD", "MARYLAND");
    STATE_MAP.put("MA", "MASSACHUSETTS");
    STATE_MAP.put("MI", "MICHIGAN");
    STATE_MAP.put("MN", "MINNESOTA");
    STATE_MAP.put("MS", "MISSISSIPPI");
    STATE_MAP.put("MO", "MISSOURI");
    STATE_MAP.put("MT", "MONTANA");
    
    STATE_MAP.put("NE", "NEBRASKA");
    STATE_MAP.put("NV", "NEVADA");
    STATE_MAP.put("NH", "NEW HAMPSHIRE");
    STATE_MAP.put("NJ", "NEW JERSEY");
    STATE_MAP.put("NY", "NEW YORK");
    STATE_MAP.put("NC", "NORTH CAROLINA");
    STATE_MAP.put("ND", "NORTH DAKOTA");
    
    STATE_MAP.put("VT", "VERMONT");
    STATE_MAP.put("VA", "VIRGINIA");
    
    STATE_MAP.put("WV", "WEST VIRGINIA");
    STATE_MAP.put("WI", "WISCONSIN");
  }
  
  // Special location code comparator that makes adjustments for the
  // difference between actual state order and state abbreviation order
  private static final Comparator<String> LOC_COMPARE = new Comparator<String>(){
    @Override
    public int compare(String str1, String str2) {
      str1 = convert(str1);
      str2 = convert(str2);
      return str1.compareTo(str2);
    }

    private String convert(String str) {
      if (str.equals("General")) return "  " + str;
      String pfx = str.substring(0,2);
      String state = STATE_MAP.get(pfx);
      if (state != null) str = state + str.substring(2);
      return str;
    }};
  
  public LocationManager() {
    saveLocation = ManagePreferences.location();
    for (String loc : saveLocation.split(",")) {
      locationList.add(loc);
      nameList.add(ManageParsers.getInstance().getLocName(loc));
    }
  }

  /**
   * Called when the location is set with a item in the normal location tree
   * @param location location code
s   */
  public void setNewLocation(String location) {
    locationList.clear();
    locationList.add(location);
    nameList.clear();
    nameList.add(ManageParsers.getInstance().getLocName(location));
    refresh();
  }
  
  
  /**
   * Called when the location is modified by checking or unchecking an item
   * in the multiple location code tree
   * @param checked true if item has been checked, false if unchecked
   * @param location location code 
   */
  public void adjustLocation(boolean checked, String location) {
    
    // Look through the location list to identify where this location should be
    int ndx = 0;
    boolean found = false;
    for (; ndx<locationList.size(); ndx++) {
      int cmp = LOC_COMPARE.compare(locationList.get(ndx), location);
      found = (cmp == 0);
      if (cmp >= 0) break;
    }
    
    // If found status matches requested status, we have nothing to do.
    if (checked == found) return;
    
    // Otherwise either add or remove this location and name as requested
    if (checked) {
      locationList.add(ndx, location);
      nameList.add(ndx, ManageParsers.getInstance().getLocName(location));
    } else {
      locationList.remove(ndx);
      nameList.remove(ndx);
    }
    
    // And update everything
    refresh();
  }
  
  // Return single location setting
  public String getLocSetting() {
    return (locationList.size() == 1 ? locationList.get(0) : "");
  }

  // Return list of selected locations
  public String[] getLocationList() {
    return locationList.toArray(new String[0]);
  }

  // Update everything when location setting(s) change
  private void refresh() {
    
    // First rebuild the persistent preference setting
    if (locationList.isEmpty()) {
      locationList.add("General");
      nameList.add("Generic Location");
    }
    String newLocation;
    if (locationList.size() == 1) {
      newLocation = locationList.get(0);
    } else {
      StringBuilder sb = new StringBuilder();
      for (String loc : locationList) {
        if (sb.length() > 0) sb.append(",");
        sb.append(loc);
      }
      newLocation = sb.toString();
    }

    // If new location matches saved location, we're done
    if (newLocation.equals(saveLocation)) return;

    saveLocation = newLocation;
    parser = null;

    // Set preference and
    // adjust related settings if necessary
    ManagePreferences.setLocation(newLocation);

    // Adjust filter settings
    MsgParser parser = getParser();
    ManagePreferences.setOverrideFilter(parser.getFilter().length() == 0);
    ManagePreferences.setFilter(parser.getFilter());
    ManagePreferences.setOverrideDefaults(false);
    ManagePreferences.setDefaultCity(parser.getDefaultCity());
    ManagePreferences.setDefaultState(parser.getDefaultState());

    // And recalculate payment status
    DonationManager.instance().reset();
    MainDonateEvent.instance().refreshStatus();
  }

  public MsgParser getParser() {
    if (parser == null) parser = ManageParsers.getInstance().getParser(saveLocation);
    return parser;
  }

  /**
   * @return Preference SummaryProvider to generate location summary preference line
   */
  public Preference.SummaryProvider<Preference> getSummaryProvider() {
    return preference -> {
      if (!preference.isEnabled()) return "N/A";
      StringBuilder sb = new StringBuilder();
      for (String name : nameList) {
        if (sb.length() > 0) sb.append('\n');
        sb.append(name);
      }
      return sb.toString();
    };
  }
}
