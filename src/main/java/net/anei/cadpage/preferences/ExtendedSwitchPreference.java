package net.anei.cadpage.preferences;


import android.content.Context;
import android.util.AttributeSet;

public class ExtendedSwitchPreference extends SwitchPreference {
  
  private OnDataChangeListener listener = null;

  public ExtendedSwitchPreference(Context c) {
    super(c);
  }

  public ExtendedSwitchPreference(Context c, AttributeSet attrs) {
    super(c, attrs);
  }

  public ExtendedSwitchPreference(Context c, AttributeSet attrs, int defStyle) {
    super(c, attrs, defStyle);
  }
  
  public void setOnDataChangeListener(net.anei.cadpage.preferences.OnDataChangeListener onDataChangeListener) {
    this.listener = onDataChangeListener;
  }

  @Override
  protected void notifyChanged() {
    super.notifyChanged();
    if (listener != null) listener.onDataChange(this);
  }
}
