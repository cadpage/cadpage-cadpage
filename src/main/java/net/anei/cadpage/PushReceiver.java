/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.anei.cadpage;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

/**
 * Receives Intent.WAP_PUSH_RECEIVED_ACTION intents and starts the
 * TransactionService by passing the push-data to it.
 */
public class PushReceiver extends BroadcastReceiver {

  private static final String ACTION_WAP_PUSH_RECEIVED =
      (BuildConfig.REC_MMS_ALLOWED ? "android.provider.Telephony.WAP_PUSH_RECEIVED"
                                   : "net.anei.cadpage.Telephony.WAP_PUSH_RECEIVED");

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v("PushReceiver: onReceive()");
    if (!CadPageApplication.initialize(context)) return;
    ContentQuery.dumpIntent(intent);

    // Check for correct action
    if (!ACTION_WAP_PUSH_RECEIVED.equals(intent.getAction())) return;

    // Hold a wake lock for 5 seconds, enough to give any
    // services we start time to take their own wake locks.
    PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    assert pm != null;
    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CadPage:MMS PushReceiver");
    wl.acquire(5000);
    
    // Pass intent on the MmsTransactionService
    MmsTransactionService.runIntentInService(context, intent);
  }
}

