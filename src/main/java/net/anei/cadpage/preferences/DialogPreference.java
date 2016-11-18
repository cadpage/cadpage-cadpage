package net.anei.cadpage.preferences;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class DialogPreference extends android.preference.DialogPreference {
  public DialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public DialogPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected View onCreateDialogView() {
    final SpannableString s = new SpannableString(getDialogMessage());
    Linkify.addLinks(s, Linkify.WEB_URLS);
    final TextView view = new TextView(getContext());
    view.setText(s);
    view.setMovementMethod(LinkMovementMethod.getInstance());
    return view;
  }
}
