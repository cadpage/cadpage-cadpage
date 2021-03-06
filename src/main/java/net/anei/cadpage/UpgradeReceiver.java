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
import android.os.PowerManager;

/**
 * Receives Intent.ACTION_MY_PACKAGE_REPLACED intents, checks for
 * a missing permission that will crash Cadpage when processing MMS messages.
 */
public class UpgradeReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v("UpgradeReceiver: onReceive()");
    if (!CadPageApplication.initialize(context)) return;
    ContentQuery.dumpIntent(intent);

    // Call PermissionFixActivity.checkPermissions() to do the real work
    PermissionFixActivity.checkPermissions(context);

    // Something else to check.  If user has upgraded to a message restricted version of
    // Cadpage, check to make sure the message support app is installed
    if (!BuildConfig.MSG_ALLOWED) {
      SmsPopupUtils.checkMsgSupport(context);
    }
  }
}

