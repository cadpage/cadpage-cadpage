package net.anei.cadpage.preferences;

import android.content.Context;
import android.util.AttributeSet;

public class ResponseBtnPreference extends androidx.preference.Preference {
  public ResponseBtnPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ResponseBtnPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  private int button;

  public int getButton() {
    return button;
  }

  public void setButton(int button) {
    this.button = button;
  }
}
