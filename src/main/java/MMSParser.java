import com.google.android.mms.pdu_alt.GenericPdu;
import com.google.android.mms.pdu_alt.PduParser;

import net.anei.cadpage.Log;
import net.anei.cadpage.MmsUtil;
import net.anei.cadpage.SmsMmsMessage;

/**
 * Test MMS message parsing
 */

public class MMSParser {

  static public void  main(String[] args) {
    Log.setTestMode(true);
    parseMsg("8C8298414B52423031303330313234303031383039313231303033303030303330303030008D918912806A65616E40636164706167652E6F726700961CEA616E6F74686572206F6E652062697465732074686520647573740086818A808E01988805810303F48083687474703A2F2F3130372E3232352E38392E3133323A383030352F592F3031323430303138303931323130303330303030333030303000");
  }

  private static void parseMsg(String data) {
    byte[] byteData = cvtHexString(data);
    GenericPdu pdu;
    try {
      PduParser parser = new PduParser(byteData);
      pdu = parser.parse();
    } catch (Exception ex) {
      ex.printStackTrace();
      return;
    }
    if (null == pdu) {
      System.err.println("Invalid PUSH data");
      return;
    }

    System.out.println("PDU:" + pdu.getClass().getName());

    SmsMmsMessage message = MmsUtil.getMessage(pdu);
    if (message == null) {
      System.err.println("Empty MMS message");
      return;
    }

    System.out.println("Parse succeeded");
    System.out.println("Subject:" + message.getSubject());
    System.out.println("ContentLoc:" + message.getContentLoc());
    System.out.println("MmsMsgId:" + message.getMmsMsgId());
  }

  private static byte[] cvtHexString(String hex) {
    int hexLen = hex.length();
    if ((hexLen & 1) != 0) throw new RuntimeException("Odd hex string length");
    byte[] result = new byte[hexLen/2];
    for (int ndx = 0; ndx < result.length; ndx++) {
      result[ndx] = (byte)((cvtHexByte(hex.charAt(2*ndx)) << 4) | cvtHexByte(hex.charAt(2*ndx+1)));
    }
    return result;
  }

  private static byte cvtHexByte(char chr) {
    if (chr >= '0' && chr <= '9') return (byte)(chr-'0');
    if (chr >= 'A' && chr <= 'F') return (byte)(chr-'A'+10);
    if (chr >= 'a' && chr <= 'f') return (byte)(chr-'a'+10);
    throw new RuntimeException("Invalid hex digit:" + chr);
  }
}
