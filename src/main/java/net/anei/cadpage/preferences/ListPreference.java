package net.anei.cadpage.preferences;

import android.content.Context;
import android.util.AttributeSet;

public class ListPreference extends androidx.preference.ListPreference {

  private String origSummary = null;

  public ListPreference(Context context) {
    super(context);
    setOrigSummary();
  }

  public ListPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrigSummary();
  }

  @Override
  public void setSummary(CharSequence summary) {
    super.setSummary(summary);
    setOrigSummary();
  }

  private void setOrigSummary() {
    if (origSummary != null) return;
    CharSequence summary = getSummary();
    if (summary == null) return;
    origSummary = summary.toString().replace("%%", "%");
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
    if (origSummary == null) return;
    setSummary(String.format(origSummary, getEntry()));
  }
}
