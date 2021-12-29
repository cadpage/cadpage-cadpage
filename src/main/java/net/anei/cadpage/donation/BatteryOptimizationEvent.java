package net.anei.cadpage.donation;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

import static android.content.Context.POWER_SERVICE;

/**
 Disable Battery Optimization for Cadpage

 We have found Cadpage works much better if you disable battery optimization.  This will have
 little or no effect on actual battery consumption
*/
public class BatteryOptimizationEvent extends DonateScreenEvent {

  public BatteryOptimizationEvent() {
    super(null, R.string.battery_optimization_title, R.string.battery_optimization_text,
          BatteryOptimizationYesEvent.instance(),
          BatteryOptimizationNoEvent.instance());
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

    // The background start provisions started with Oreo.  Before that, this is no relevant
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false;

    // If user had decided to disregard our advice, so be it
    // At least for now.  Future versions that target SDK 30 will not be so charitable.
    if (ManagePreferences.keepBatteryOptimization()) return false;

    // And if the user has followed our advice and turned off battery optimization for Cadpage,
    // we are good to go
    Context context = CadPageApplication.getContext();
    PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
    return !pm.isIgnoringBatteryOptimizations(context.getPackageName());

  }

  private static final BatteryOptimizationEvent instance = new BatteryOptimizationEvent();
  
  public static BatteryOptimizationEvent instance() {
    return instance;
  }
}
