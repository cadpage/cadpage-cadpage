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
    parseMsg("8C868D908B3044394146303634413039463030303032304130303030330097383530363934303232322F545950453D504C4D4E0085045B8960349581");
    parseMsg("8C888D918B3044394146303634413039463030303032304130303030330097383530363934333338322F545950453D504C4D4E00891680383530363934303232322F545950453D504C4D4E0085045B89614E9B80");
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