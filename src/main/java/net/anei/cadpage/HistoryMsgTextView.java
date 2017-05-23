package net.anei.cadpage;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class HistoryMsgTextView extends LinearLayout {

  private SmsMmsMessage msg;
  public HistoryMsgTextView(Context context) {
    super(context);
    setup();
  }
  
  public HistoryMsgTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setup();
  }

  private void setup() {
    this.setOnClickListener(new OnClickListener(){

      @Override
      public void onClick(View v) {
        
        // Clear any active notification and wake locks
        ClearAllReceiver.clearAll(getContext());
        
        if (msg == null) return;
        
        // display message popup
        if (Log.DEBUG) Log.v("HistoryMsgTextView User launch SmsPopup for " + msg.getMsgId()); 
        SmsPopupActivity.launchActivity(getContext(), msg);
      }});
  }
  
  public void setMessage(SmsMmsMessage message) {
    this.msg = message;
    message.showHistory(getContext(), this);
  }

  public SmsMmsMessage getMessage() {
    return msg;
  }
}
