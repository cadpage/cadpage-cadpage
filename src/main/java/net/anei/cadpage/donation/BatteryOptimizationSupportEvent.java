package net.anei.cadpage.donation;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

import static android.content.Context.POWER_SERVICE;

/**
 Disable Battery Optimization for support app

 Cadpage cannot initiate phone calls unless battery optimization is disabled for support app
*/
public class BatteryOptimizationSupportEvent extends DonateScreenEvent {

  public BatteryOptimizationSupportEvent() {
    super(null, R.string.battery_optimization_support_title, R.string.battery_optimization_support_text,
          BatteryOptimizationSupportYesEvent.instance(),
          FixMsgSupportEvent.instance()
    );
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return isActive();
  }

  @Override
  public void onRestart(DonateActivity activity) {
    super.onRestart(activity);
    if (!isActive()) closeEvents(activity);
  }

  private boolean isActive() {

    // Not appropriate when Cadpage handles phone calls itself
    if (!SmsPopupUtils.isSupportAppAvailable()) return false;

    // We shouldn't be called unless the support app is required and is already installoed
    // so don't check for those conditions

    // This only applies if we are running under Android 12
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false;

    //  And Cadpage is requested to initiate a phone call when a response button is pressed
    if (!ManagePreferences.callbackTypeSummary().contains("P")) return false;

    // And battery optimization has not been disabled for the support app
    Context context = CadPageApplication.getContext();
    PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
    return !pm.isIgnoringBatteryOptimizations(SmsPopupUtils.CADPAGE_SUPPORT_PKG);

  }

  private static final BatteryOptimizationSupportEvent instance = new BatteryOptimizationSupportEvent();
  
  public static BatteryOptimizationSupportEvent instance() {
    return instance;
  }
}
