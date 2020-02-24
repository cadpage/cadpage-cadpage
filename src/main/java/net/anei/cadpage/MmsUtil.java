/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.anei.cadpage;

import android.telephony.SmsMessage;

import com.google.android.mms.pdu_alt.CharacterSets;
import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.GenericPdu;
import com.google.android.mms.pdu_alt.NotificationInd;

import java.io.UnsupportedEncodingException;

import androidx.annotation.NonNull;

@SuppressWarnings("CharsetObjectCanBeUsed")
public class MmsUtil {

  public static SmsMmsMessage getMessage(GenericPdu pdu) {
    if (!(pdu instanceof NotificationInd)) return null;
    NotificationInd npdu = (NotificationInd)pdu;

    SmsMessage.MessageClass msgCls = SmsMessage.MessageClass.UNKNOWN;
    byte[] baMsgCls = npdu.getMessageClass();
    if (baMsgCls != null) {
      try {
        msgCls = SmsMessage.MessageClass.valueOf(new String(baMsgCls));
      } catch (Exception ignored) {}
    }
    String from = getStringValue(npdu.getFrom());
    String subject = getStringValue(npdu.getSubject());
    String content = getStringValue(npdu.getContentLocation());
    String msgId = getStringValue(npdu.getTransactionId());
    return new SmsMmsMessage(msgCls, from, subject, content, msgId,
                             System.currentTimeMillis());
  }

  private static String getStringValue(EncodedStringValue val) {
    if (val == null) return "";
    String rtn = val.getString();
    if (rtn == null) return "";
    return rtn;
  }

  private static String getStringValue(byte[] val) {
    if (val == null) return "";
    return new String(val);
  }

  public static @NonNull
  String toIsoString(byte[] bytes) {
    try {
      return new String(bytes, CharacterSets.MIMENAME_ISO_8859_1);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("ISO_8859_1 must be supported!");
    }
  }
}
