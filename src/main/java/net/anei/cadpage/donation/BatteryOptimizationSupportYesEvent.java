package net.anei.cadpage.donation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import net.anei.cadpage.ContentQuery;
import net.anei.cadpage.Log;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

public class BatteryOptimizationSupportYesEvent extends DonateEvent {

    public BatteryOptimizationSupportYesEvent() {
        super(null,  R.string.battery_optimization_support_yes_title);
    }

    @Override
    protected void doEvent(Activity activity) {

        // Turning off battery optimization for another app doesn't work, so we will have to
        // pass the request to the support app and let it do it
        SmsPopupUtils.launchSupportApp(activity, true, true);
    }

    private static final BatteryOptimizationSupportYesEvent instance = new BatteryOptimizationSupportYesEvent();

    public static BatteryOptimizationSupportYesEvent instance() {
        return instance;
    }

}
