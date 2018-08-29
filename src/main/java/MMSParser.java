import net.anei.cadpage.Log;
import net.anei.cadpage.SmsMmsMessage;
import net.anei.cadpage.mms.GenericPdu;
import net.anei.cadpage.mms.PduParser;

/**
 * Test MMS message parsing
 */

public class MMSParser {

  static public void  main(String[] args) {
    Log.setTestMode(true);
    parseMsg("8C8298443430383239303432343334363030303132303030303630303030008D908911806B656E40636164706167652E6F726700961F29EA5965612C2069742773206120766572792074696E79206D65737361676520746869732074696D65008A808E018F8805810303F48083687474703A2F2F3136362E3231362E3136362E36373A383030362F592F3038323930343234333436303030313230303030363030303000");
  }

  private static void parseMsg(String data) {
    byte[] byteData = cvtHexString(data);
    GenericPdu pdu = null;
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

    SmsMmsMessage message = pdu.getMessage();
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
