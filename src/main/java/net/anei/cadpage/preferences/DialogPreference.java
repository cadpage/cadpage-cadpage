package net.anei.cadpage.preferences;

import android.content.Context;
import android.util.AttributeSet;

public class DialogPreference extends androidx.preference.DialogPreference {

  public DialogPreference(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
    super(context, attrs, defStyle, defStyleRes);
  }

  public DialogPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public DialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public DialogPreference(Context context) {
    super(context);
  }

}
