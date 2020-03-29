package net.anei.cadpage;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceDialogFragmentCompat;

public class PreferenceDialogFragment extends PreferenceDialogFragmentCompat {

  public static PreferenceDialogFragment newInstance(String key) {
    final PreferenceDialogFragment fragment = new PreferenceDialogFragment();
    final Bundle b = new Bundle(1);
    b.putString(ARG_KEY, key);
    fragment.setArguments(b);
    return fragment;
  }

  @Override
  protected View onCreateDialogView(Context context) {
    CharSequence msg = getPreference().getDialogMessage();
    if (msg == null) return super.onCreateDialogView(context);
    final SpannableString s = new SpannableString(msg);
    Linkify.addLinks(s, Linkify.WEB_URLS);
    final TextView view = new TextView(context);
    view.setText(s);
    view.setMovementMethod(LinkMovementMethod.getInstance());
    return view;
  }

  @Override
  public void onDialogClosed(boolean positiveResult) {}
}
