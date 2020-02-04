package net.anei.cadpage.preferences;

import android.content.Context;
import android.util.AttributeSet;

public class ListPreference extends androidx.preference.ListPreference {

  private String origSummary;

  public ListPreference(Context context) {
    super(context);
    origSummary = getSummary().toString().replace("%%", "%");
  }

  public ListPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    origSummary = getSummary().toString().replace("%%", "%");
  }

  @Override
  protected void onSetInitialValue(Object defaultValue) {
    super.onSetInitialValue(defaultValue);
    refreshSummary();
  }

  @Override
  public void setValue(String value) {
    super.setValue(value);
    refreshSummary();
    
    OnPreferenceChangeListener listener = getOnPreferenceChangeListener();
    if (listener != null) listener.onPreferenceChange(this, value);
  }

  private void refreshSummary() {
    if (origSummary == null) origSummary = getSummary().toString().replace("%%", "%");
    setSummary(String.format(origSummary, getEntry()));
  }
}
