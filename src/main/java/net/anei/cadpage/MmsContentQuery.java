package net.anei.cadpage;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import net.anei.cadpagesupport.IContentService;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles MMS content queries, either directly if Message access is allowed
 * or by relaying them through to the support app if not
 */
public class MmsContentQuery {

  private ContentResolver qr = null;

  private IContentService mContentService = null;
  private ServiceConnection mServiceConnect = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.v("MmsContentQuery service connected");
      mContentService = IContentService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.v("MmsContentQuery service disconnected");
      mContentService = null;
    }
  };


  public MmsContentQuery(Context context) {

    // Context has to be an Activity or Service for the service binding to work
    if (!(context instanceof Service || context instanceof Activity)) {
      throw new RuntimeException("MmsContentQuery context must be Service or Activity");
    }

    // If message access is allowed, we can use a normal content resolver
    if (MsgAccess.ALLOWED) {
      qr = context.getContentResolver();
    }

    // Otherwise, set up the service connection to
    // the support app that will do our content queries
    else {
      Intent intent = new Intent();
      intent.setClassName("net.anei.cadpagesupport", "net.anei.cadpagesupport.ContentService");
      if (context.bindService(intent, mServiceConnect, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT)) {
        Log.v("MmsContentQuery binding succeeded");
      } else {
        Log.v("MsgContentQuery binding failed");
      };
    }
  }

  /**
   * @return true if content query system is up and functioning
   */
  public boolean isActive() {
    return qr != null || mContentService != null;
  }

  public Cursor query(String url, String[] projection, String selection, String[] selectArgs, String sortOrder) {

    // If we have a content resolver, we can just use it as is
    if (qr != null) {
      Uri uri = Uri.parse(url);
      return qr.query(uri, projection, selection, selectArgs, sortOrder);
    }

    // If not, then we should have a content service we can use
    // If not, we are just SOL
    if (mContentService == null) {
      Log.v("MmsContentQuery failed - no service connection");
      return null;
    }

    // Otherwise invoke the support app to do the content query
    try {
      return mContentService.query(url, projection, selection, selectArgs, sortOrder);
    } catch (RemoteException ex) {
      Log.e(ex);
      return null;
    }
  }
}
