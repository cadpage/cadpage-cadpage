package net.anei.cadpage.donation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import net.anei.cadpage.ContentQuery;
import net.anei.cadpage.R;

public class BatteryOptimizationYesEvent extends DonateEvent {

    public BatteryOptimizationYesEvent() {
        super(null,  R.string.battery_optimization_yes_title);
    }

    @Override
    protected void doEvent(Activity activity) {
        String packageName = activity.getPackageName();
        @SuppressLint({"InlinedApi", "BatteryLife"}) Intent newIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + packageName));
        ContentQuery.dumpIntent(newIntent);
        activity.startActivity(newIntent);
    }

    private static final BatteryOptimizationYesEvent instance = new BatteryOptimizationYesEvent();

    public static BatteryOptimizationYesEvent instance() {
        return instance;
    }

}
