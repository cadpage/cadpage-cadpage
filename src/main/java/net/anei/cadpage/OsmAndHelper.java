package net.anei.cadpage;

import android.content.Intent;
import android.net.Uri;

import net.anei.cadpage.parsers.MsgParser;

import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Utility class supporting the interface with the OsmAnd mapping app
 */
class OsmAndHelper {

  private static final String PREFIX = "osmand.api://";

  private static final String NAVIGATE = "navigate";
  private static final String NAVIGATE_SEARCH = "navigate_search";

  private static final String PARAM_DEST_NAME = "dest_name";
  private static final String PARAM_DEST_LAT = "dest_lat";
  private static final String PARAM_DEST_LON = "dest_lon";
  private static final String PARAM_DEST_SEARCH_QUERY = "dest_search_query";
  private static final String PARAM_PROFILE = "profile";
  private static final String PARAM_SHOW_SEARCH_RESULTS = "show_search_results";
  private static final String PARAM_FORCE = "force";

  /**
   * Return intent to launch OsmAnd map search
   * @param searchStr raw map search string
   * @param gps true if searchStr contains GPS coordinates
   * @param navigateMap true if user wishes to jump straight into turn by turn navigation
   * @param destName name to be displayed at destination
   * @return intent needed to launch OsmAnd
   */
  public static Intent getIntent(String searchStr, boolean gps, boolean navigateMap, String destName) {

    // Are we navigating to GPS coordinates?
    Uri uri;
    if (gps) {
      int pt = searchStr.indexOf(',');
      if (pt < 0) return null;
      String lat = searchStr.substring(0, pt).trim();
      String lon = searchStr.substring(pt + 1).trim();
      uri = getUri(NAVIGATE,
          PARAM_DEST_LAT, lat,
          PARAM_DEST_LON, lon,
          PARAM_DEST_NAME, destName,
          PARAM_PROFILE, "car",
          PARAM_FORCE, "true");
    }

    // Regular address search
    else {
      searchStr = convertSearchString(searchStr);
      uri = getUri(NAVIGATE_SEARCH,
                    PARAM_DEST_SEARCH_QUERY, searchStr,
                    PARAM_SHOW_SEARCH_RESULTS, "false",
                    PARAM_PROFILE, "car",
                    PARAM_FORCE, "true");
    }
    return new Intent(Intent.ACTION_VIEW, uri);
  }

  private static Uri getUri(String command, String ... params) {
    StringBuilder stringBuilder = new StringBuilder(PREFIX);
    stringBuilder.append(command);
    if (params != null) {
      char connector = '?';
      for (int ndx = 0; ndx < params.length; ndx += 2) {
        String key = params[ndx];
        String val = params[ndx+1];
        if (val != null) {
          stringBuilder.append(connector).append(key).append('=').append(Uri.encode(val));
          connector = '&';
        }
      }
    }
    return Uri.parse(stringBuilder.toString());
  }

  private static final Pattern MBLANKS_PTN = Pattern.compile("\\s+");
  /**
   * Convert normal map search string into OsmAnd compatible search string
   * @param searchStr search string to be converted
   * @return results of conversion
   */
  private static String convertSearchString(String searchStr) {
    searchStr = searchStr.toUpperCase();
    MsgParser.Parser p = new MsgParser.Parser(searchStr);
    String addr = p.get(',');
    String city = p.get(',');
    String state = p.get(',');
    if (state.length() == 0 && city.length() == 2) {
      state = city;
      city = "";
    }
    StringBuilder sb = new StringBuilder();
    if (city.length() > 0) sb.append(city);

    for (String token : MBLANKS_PTN.split(addr)) {
      String token2 = ADDRESS_CODES.getProperty(token);
      if (token2 != null) token = token2;
      if (sb.length() > 0) sb.append(' ');
      sb.append(token);
    }

    if (state.length() > 0) {
      state = STATE_CODES.getProperty(state);
      if (state != null) {
        if (sb.length() > 0) sb.append(' ');
        sb.append(state);
      }
    }

    return sb.toString();
  }

  private static final Properties STATE_CODES = MsgParser.buildCodeTable(new String[]{
      "AL", "ALABAMA",
      "AK", "ALASKA",
      "AZ", "ARIZONA",
      "AR", "ARKANSAS",
      "CA", "CALIFORNIA",
      "CO", "COLORADO",
      "CT", "CONNECTICUT",
      "DE", "DELAWARE",
      "FL", "FLORIDA",
      "GA", "GEORGIA",
      "HI", "HAWAII",
      "ID", "IDAHO",
      "IL", "ILLINOIS",
      "IN", "INDIANA",
      "IA", "IOWA",
      "KS", "KANSAS",
      "KY", "KENTUCKY",
      "LA", "LOUISIANA",
      "ME", "MAINE",
      "MD", "MARYLAND",
      "MA", "MASSACHUSETTS",
      "MI", "MICHIGAN",
      "MN", "MINNESOTA",
      "MS", "MISSISSIPPI",
      "MO", "MISSOURI",
      "MT", "MONTANA",
      "NE", "NEBRASKA",
      "NV", "NEVADA",
      "NH", "NEW HAMPSHIRE",
      "NJ", "NEW JERSEY",
      "NM", "NEW MEXICO",
      "NY", "NEW YORK",
      "NC", "NORTH CAROLINA",
      "ND", "NORTH DAKOTA",
      "OH", "OHIO",
      "OK", "OKLAHOMA",
      "OR", "OREGON",
      "PA", "PENNSYLVANIA",
      "RI", "RHODE ISLAND",
      "SC", "SOUTH CAROLINA",
      "SD", "SOUTH DAKOTA",
      "TN", "TENNESSEE",
      "TX", "TEXAS",
      "UT", "UTAH",
      "VT", "VERMONT",
      "VA", "VIRGINIA",
      "WA", "WASHINGTON",
      "WV", "WEST VIRGINIA",
      "WI", "WISCONSIN",
      "WY", "WYOMING",

      "DC", "DISTRICT OF COLUMBIA",
      "MH", "MARSHALL ISLANDS",

      "AB", "ALBERTA",
      "BC", "BRITISH COLUMBIA",
      "MB", "MANITOBA",
      "NB", "NEW BRUNSWICK",
      "NS", "NOVA SCOTIA",
      "NT", "NORTWEST TERRITORIES",
      "ON", "ONTARIO",
      "PE", "PRINCE EDWARD ISLAND",
      "QC", "QUEBEC",
      "SK", "SASKATCHEWAN",
      "YT", "YUKON"
  });

  private static final Properties ADDRESS_CODES = MsgParser.buildCodeTable(new String[]{
      "ALY",    "ALLEY",
      "AVE",    "AVENUE",
      "AT",     "-",
      "BD",     "BOULEVARD",
      "BL",     "BOULEVARD",
      "BLV",    "BOULEVARD",
      "BLVD",   "BOULEVARD",
      "BND",    "BEND",
      "BV",     "BOULEVARD",
      "BVD",    "BOULEVARD",
      "BYP",    "BYPASS",
      "CI",     "CIRCLE",
      "CIR",    "CIRCLE",
      "CL",     "CIRCLE",
      "CR",     "CIRCLE",
      "CT",     "COURT",
      "CV",     "COVE",
      "DR",     "DRIVE",
      "ESTS",   "ESTATES",
      "EXPW",   "EXPRESSWAY",
      "EXPY",   "EXPRESSWAY",
      "FWY",    "FREEWAY",
      "GR",     "GRADE",
      "GRN",    "GREEN",
      "GRV",    "GROVE",
      "GTWY",   "GATEWAY",
      "HT",     "HEIGHTS",
      "HTS",    "HEIGHTS",
      "HW",     "HIGHWAY",
      "HWY",    "HIGHWAY",
      "HY",     "HIGHWAY",
      "LN",     "LANE",
      "PASS",   "PASSAGE",
      "PK",     "PIKE",
      "PKE",    "PIKE",
      "PKW",    "PARKWAY",
      "PKWAY",  "PARKWAY",
      "PKWY",   "PARKWAY",
      "PKY",    "PARKWAY",
      "PL",     "PLACE",
      "PLAZ",   "PLAZA",
      "PW",     "PARKWAY",
      "PWY",    "PARKWAY",
      "PY",     "PARKWAY",
      "RCH",    "REACH",
      "RD",     "ROAD",
      "SQ",     "SQUARE",
      "ST",     "STREET",
      "TE",     "TERRACE",
      "TER",    "TERRACE",
      "TERR",   "TERRACE",
      "TL",     "TRAIL",
      "TP",     "TURNPIKE",
      "TPK",    "TURNPIKE",
      "TPKE",   "TURNPIKE",
      "TR",     "TRAIL",
      "TRC",    "TERRACE",
      "TRCE",   "TERRACE",
      "TRL",    "TRAIL",
      "TRNPK",  "TURNPIKE",
      "TRPK",   "TURNPIKE",
      "WY",     "WAY",
      "XING",   "CROSSING"
  });
}
