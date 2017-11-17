package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public abstract class DonateScreenEvent extends DonateScreenBaseEvent {
  
  private DonateEvent[] events;

  protected DonateScreenEvent(AlertStatus alertStatus, int titleId, int textId,
                               DonateEvent ... events) {
    super(alertStatus, titleId, textId, R.layout.popup_donate_screen);
    this.events = events;
  }

  protected DonateScreenEvent(int titleId, int textId, int winTitleId,
                              DonateEvent ... events) {
    super(titleId, textId, winTitleId, R.layout.popup_donate_screen);
    this.events = events;
  }

  protected void setEvents(DonateEvent ... events) {
    this.events = events;
  }
  
  /**
   * @return list of donation events associated with this screen
   */
  public DonateEvent[] getEvents() {
    return events;
  }

  /**
   * Called to create the associated Donate activity
   * @param activity new activity being created
   */
  public void create(final Activity activity, SmsMmsMessage msg) {
    super.create(activity, msg);
    if (activity.isFinishing()) return;
    
    // Fill the button list with the appropriate event buttons
    LinearLayout btnList = (LinearLayout)activity.findViewById(R.id.DonateButtonList);
    if (btnList == null) return;

    boolean includeDone = false;
    if (events != null){
      for (DonateEvent event : events) {
        event.addButton(activity, btnList, msg);
        if (event instanceof DoneDonateEvent) includeDone = true;
      }
    }
    
    // Add a cancel button at bottom of list
    // unless the menu includes a Done event, in which case a
    // cancel button is unnecessary
    if (!includeDone) {
      Button btn = new Button(activity);
      btn.setText(R.string.donate_btn_cancel);
      btn.setTransformationMethod(null);
      btn.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          activity.finish();
        }
      });
      btnList.addView(btn);
    }
  }

  @Override
  protected void doEvent(Activity activity, SmsMmsMessage msg) {
    DonateActivity.launchActivity(activity, this, msg);
  }
}
