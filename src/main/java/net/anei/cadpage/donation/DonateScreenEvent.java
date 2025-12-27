package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;
import android.app.Activity;
import android.content.Intent;
import android.widget.Button;
import android.widget.LinearLayout;

public abstract class DonateScreenEvent extends DonateScreenBaseEvent {
  
  private DonateEvent[] events;

  protected DonateScreenEvent(AlertStatus alertStatus, int titleId, int textId,
                               DonateEvent ... events) {
    super(alertStatus, titleId, textId, R.layout.popup_donate_screen);
    this.events = events;
  }

  protected DonateScreenEvent(int titleId, int winTitleId, int textId,
                              DonateEvent ... events) {
    super(titleId, winTitleId, textId, R.layout.popup_donate_screen);
    this.events = events;
  }

  protected DonateScreenEvent(AlertStatus alertStatus, int titleId, int winTitleId, int textId,
                              DonateEvent ... events) {
    super(alertStatus, titleId, winTitleId, textId, R.layout.popup_donate_screen);
    this.events = events;
  }

  protected void setEvents(DonateEvent ... events) {
    this.events = events;
  }
  
  /**
   * @return array of donation events associated with this screen
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
    LinearLayout btnList = activity.findViewById(R.id.DonateButtonList);
    if (btnList == null) return;

    boolean inhibitCancel = false;
    if (events != null){
      for (DonateEvent event : events) {
        if (event instanceof DoneDonateEvent) inhibitCancel = true;
        event.addButton(activity, btnList, msg);
      }
    }
    
    // Add a cancel button at bottom of list, unless list includes a Done button
    if (!inhibitCancel) {
      Button btn = new Button(activity);
      btn.setText(R.string.donate_btn_cancel);
      btn.setOnClickListener(v -> activity.finish());
      btnList.addView(btn);
    }
  }

  @Override
  public boolean followup(Activity activity, int req, int result, Intent intent) {
    for (DonateEvent event : events) {
      if (event.followup(activity, req, result, intent)) return true;
    }
    return false;
  }
}
