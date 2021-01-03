package net.anei.cadpage.vendors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import android.net.Uri;

import net.anei.cadpage.R;
import net.anei.cadpage.parsers.Active911ParserTable;
import net.anei.cadpage.parsers.MsgParser;

class Active911Vendor extends Vendor {

  private static final Uri WEB_URI = Uri.parse("https://console.active911.com/cadpage_registration");
  private static final Uri ACCESS_URI = Uri.parse("https://access.active911.com/interface/cadpage_api.php");
  
  Active911Vendor() {
    super(R.string.active911_title,
           R.string.active911_summary,
           R.string.active911_text,
           R.drawable.active_911_vendor,
           R.drawable.active_911_logo,
           "https://www.active911.com",
           ">A91",
           null);
  }

  @Override
  boolean isSponsored() {
    return true;
  }

  @Override
  boolean isAvailable() {
    return true;
  }
  
  @Override
  String getResponseMenu(int index) {
    if (index == 1) {
      return "R=Respond;A=Arrive;Y=Available;N=Unavailable;C=Cancel";
    }
    return null;
  }

  @Override
  Uri getBaseURI(String req) {
    if (req.equals("register") || req.equals("info") || req.equals("profile")) {
      return WEB_URI;
    } else {
      return ACCESS_URI;
    }
  }

  @Override
  protected Uri buildRequestUri(String req, String registrationId) {
    if (req.equals("profile")) return getBaseURI().buildUpon().appendPath("node").appendPath("3#").build();
    return super.buildRequestUri(req, registrationId);
  }

  @Override
  boolean isVendorAddress(String address) {
    if (address.startsWith("+")) address =address.substring(1);
    return PHONE_SET.contains(address);
  }
  
  @Override
  String[] convertLocationCode(String location) {
    StringBuilder missingParsers = null;
    StringBuilder sb = new StringBuilder();
    Set<String> parserSet = new HashSet<>();
    
    for (String loc : location.split(",")){
      loc = loc.trim();
      if (loc.contains("/") || loc.equals("Active911Summary")) {
        String newLoc = Active911ParserTable.convert(loc);
        if (newLoc == null) {
          if (missingParsers == null) {
            missingParsers = new StringBuilder(loc);
          } else {
            missingParsers.append(',').append(loc);
          }
          newLoc = "General";
        }
        if (newLoc.equals("N/A")) continue;
        loc = newLoc;
      }
      if (parserSet.add(loc)) {
        if (sb.length() > 0) sb.append(',');
        sb.append(loc);
      }
    }
    return new String[]{sb.toString(), missingParsers == null ? null : missingParsers.toString()};
  }

  @Override
  protected boolean isTestMsg(String msg) {
    return msg.equals("This is a test message from Active911");
  }

  @Override
  protected boolean isActiveSponsor(String account, String token) {
    return account != null && ACTIVE_ACCTS.getProperty(account) != null;
  }
  
  private static final Properties ACTIVE_ACCTS = MsgParser.buildCodeTable(new String[]{
      "21301",   "06082016",
      "191976",  "06232016",
      "100157",  "07082016",
      "180674",  "07122016",
      "40803",   "07282016",
      "143290",  "07282016",
      "45018",   "07282016",
      "88123",   "09212016",
      "40905",   "10152016",
      "45661",   "12062016",
      "123559",  "01152017",
      "17251",   "04072017",
      "47015",   "04122017",
      "352505",  "08042017",
      "226913",  "08192017",
      "134547",  "01062018",
      "3045",    "05222018",
      "19707",   "07202018",
      "108673",  "10152018",
      "182210",  "02172019",
      "165038",  "03032019",
      "72897",   "09242019",
      "41839",   "02192020"

  });



  private static final Set<String> PHONE_SET = new HashSet<>(Arrays.asList(
          "15123376259",
          "19145173586",
          "17272191279",
          "15417047704",
          "18434800223",
          "17172203767",
          "13364058803",
          "17783836218",
          "12027690862",
          "12032083335",
          "12052010901",
          "12072093315",
          "12706810905",
          "12765240572",
          "13046587002",
          "13072222635",
          "13134010041",
          "13172967331",
          "13603424100",
          "14012973063",
          "14029881004",
          "14046926092",
          "14052534266",
          "14062244055",
          "14242208369",
          "14433202484",
          "14805356958",
          "15013131847",
          "15046621719",
          "15052065036",
          "15132024579",
          "15744008669",
          "16013452163",
          "16052207124",
          "16086207759",
          "16093087467",
          "16122000004",
          "16156252978",
          "16363233043",
          "16418470032",
          "16783903000",
          "17012044024",
          "17196025911",
          "17572062724",
          "17736146018",
          "17752307392",
          "18019006459",
          "18022304149",
          "19134989068",
          "19783931289"));
}
