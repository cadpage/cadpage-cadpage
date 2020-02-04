package net.anei.cadpage.preferences;

import android.content.Context;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class DialogPreference extends androidx.preference.DialogPreference {
  public DialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public DialogPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  // This is supposed to enable web links, but we have not figured out how to do this
  // with the Jetpack libraries.  Retained for future reference
//  @Override
//  protected View onCreateDialogView() {
//    CharSequence msg = getDialogMessage();
//    if (msg == null) return super.onCreateDialogView();
//    final SpannableString s = new SpannableString(getDialogMessage());
//    Linkify.addLinks(s, Linkify.WEB_URLS);
//    final TextView view = new TextView(getContext());
//    view.setText(s);
//    view.setMovementMethod(LinkMovementMethod.getInstance());
//    return view;
//  }
}
