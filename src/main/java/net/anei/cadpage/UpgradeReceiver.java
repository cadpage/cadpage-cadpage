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
    CadPageApplication.initialize(context);
    ContentQuery.dumpIntent(intent);
    
    // If initialization failure in progress, shut down without doing anything
    if (TopExceptionHandler.isInitFailure()) return;

    // Call PermissionFixActivity.checkMMSPermission() to do the real work
    PermissionFixActivity.checkMMSPermission(context);
  }
}

