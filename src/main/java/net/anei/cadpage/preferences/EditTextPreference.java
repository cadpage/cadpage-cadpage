package net.anei.cadpage.preferences;

import android.content.Context;
import android.util.AttributeSet;

public class EditTextPreference extends androidx.preference.EditTextPreference {

  private String origSummary = null;

  public EditTextPreference(Context context) {
    super(context);
    setOrigSummary();
  }

  public EditTextPreference(Context context, AttributeSet attrs) {
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
  public void setText(String value) {
    super.setText(value);
    refreshSummary();
  }
  
  public void refreshSummary() {
    String value = getText();
    if (value == null) value = "";
    refreshSummary(value);
  }

  private void refreshSummary(String newValue) {
    if (origSummary == null) return;
    setSummary(String.format(origSummary, translateValue(newValue)));
  }

  protected String translateValue(String value) {
    return value;
  }
}
