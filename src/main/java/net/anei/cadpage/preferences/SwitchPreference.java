package net.anei.cadpage.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

/**
 * Workaround for bug in the official SwitchPreference that was not fixed until Lollipop
 */
public class SwitchPreference extends android.preference.SwitchPreference {

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public SwitchPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SwitchPreference(Context context) {
    super(context);
  }

  @Override
  protected void onBindView(View view) {
    // Clean listener before invoke SwitchPreference.onBindView
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      ViewGroup viewGroup = (ViewGroup) view;
      clearListenerInViewGroup(viewGroup);
    }

    super.onBindView(view);
  }

  /**
   * Clear listener in Switch for specify ViewGroup.
   *
   * @param viewGroup The ViewGroup that will need to clear the listener.
   */
  private void clearListenerInViewGroup(ViewGroup viewGroup) {
    if (null == viewGroup) return;

    int count = viewGroup.getChildCount();
    for(int n = 0; n < count; ++n) {
      View childView = viewGroup.getChildAt(n);
      if(childView instanceof Switch) {
        final Switch switchView = (Switch) childView;
        switchView.setOnCheckedChangeListener(null);
        return;
      } else if (childView instanceof ViewGroup){
        ViewGroup childGroup = (ViewGroup)childView;
        clearListenerInViewGroup(childGroup);
      }
    }
  }
}
