package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import net.anei.cadpage.ContentQuery;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

public class BatteryOptimizationNoEvent extends DonateEvent {

    public BatteryOptimizationNoEvent() {
        super(null,  R.string.battery_optimization_no_title);
    }

    @Override
    protected void doEvent(Activity activity) {
        ManagePreferences.setKeepBatteryOptimization(true);
        closeEvents(activity);
    }

    private static final BatteryOptimizationNoEvent instance = new BatteryOptimizationNoEvent();

    public static BatteryOptimizationNoEvent instance() {
        return instance;
    }

}
