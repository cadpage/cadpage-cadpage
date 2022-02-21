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

 Starting with Android 12, Cadpage just will not work unless you disable battery optimization.
 This will have little or no effect on actual battery consumption
*/
public class BatteryOptimization12Event extends DonateScreenEvent {

  public BatteryOptimization12Event() {
    super(null, R.string.battery_optimization_title, R.string.battery_optimization12_text,
          BatteryOptimizationYesEvent.instance()
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

    // This only applies if we are running under Android 12
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false;

    // This cannot be overridden.  If optimization has not been disabled, user has to disable it
    Context context = CadPageApplication.getContext();
    PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
    return !pm.isIgnoringBatteryOptimizations(context.getPackageName());

  }

  private static final BatteryOptimization12Event instance = new BatteryOptimization12Event();
  
  public static BatteryOptimization12Event instance() {
    return instance;
  }
}
