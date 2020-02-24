package net.anei.cadpage;

import com.google.android.mms.ContentType;
import com.google.android.mms.pdu_alt.CharacterSets;
import com.google.android.mms.pdu_alt.PduBody;

import java.io.UnsupportedEncodingException;


public class MmsPartParser {
  public static String getMessageText(PduBody body) {
    String bodyText = null;

    for (int i=0;i<body.getPartsNum();i++) {
      if (ContentType.isTextType(MmsUtil.toIsoString(body.getPart(i).getContentType()))) {
        String partText;

        try {
          String characterSet = CharacterSets.getMimeName(body.getPart(i).getCharset());

          if (characterSet.equals(CharacterSets.MIMENAME_ANY_CHARSET))
            characterSet = CharacterSets.MIMENAME_UTF_8;

          if (body.getPart(i).getData() != null) {
            partText = new String(body.getPart(i).getData(), characterSet);
          } else {
            partText = "";
          }
        } catch (UnsupportedEncodingException e) {
          Log.e("PartParser", e);
          partText = "Unsupported Encoding!";
        }

        bodyText = (bodyText == null) ? partText : bodyText + " " + partText;
      }
    }

    return bodyText;
  }
}
