package net.anei.cadpage.donation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ContentQuery;
import net.anei.cadpage.EmailDeveloperActivity;
import net.anei.cadpage.Log;
import net.anei.cadpage.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Handles download and installation of Cadpage support app
 */
public class InstallSupportAppActivity extends AppCompatActivity {

  private static final String SUPPORT_URL = "https://drive.google.com/u/0/uc?id=1iNRe4sW2_iG0iG4nffMyK5fToooJ_WmH&export=download&confirm=t&uuid=21dcd2cb-6fed-4753-8638-a74738fbf7fc&at=ANzk5s4cGLeUbXfDhj07L_VPCf5w:1681346808204";
  private static final String SUPPORT_FILENAME = "cadpage-support.apk";

  private TextView textView;
  private Button cancelBtn;

  private Button tryAgainBtn;
  private Button problemBtn;
  private Button doneBtn;

  private File supportAppFile;
  private DownloadManager dlMgr = null;
  private long lastDownload=-1L;

  private final StringBuilder textBuilder = new StringBuilder();
  private int textMarker;
  private boolean downloadDone = false;

  private final Handler handler = new Handler(Looper.getMainLooper());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!CadPageApplication.initialize(this)) {
      finish();
    }
    setContentView(R.layout.popup_support_app_screen);

    dlMgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    //noinspection ResultOfMethodCallIgnored
    dir.mkdir();
    supportAppFile = new File(dir, SUPPORT_FILENAME);
   }

  @Override
  protected void onStart() {
    super.onStart();
    if (isFinishing()) return;

    textView = findViewById(R.id.SupportAppTextView);
    cancelBtn = findViewById(R.id.btnCancel);
    cancelBtn.setOnClickListener(v -> InstallSupportAppActivity.this.finish());
    tryAgainBtn = findViewById(R.id.btnTryAgain);
    tryAgainBtn.setOnClickListener(v -> {
      if (!downloadDone) startDownload();
      else startInstallApp();
    });
    problemBtn = findViewById(R.id.btnProblem);
    problemBtn.setOnClickListener(v -> EmailDeveloperActivity.sendSupportAppProblemEmail(InstallSupportAppActivity.this));
    doneBtn = findViewById(R.id.btnDone);
    doneBtn.setOnClickListener(v -> InstallSupportAppActivity.this.finish());

    startDownload();
  }

  private void startDownload() {
    Log.v("Start support app download");
    cancelBtn.setEnabled(true);
    tryAgainBtn.setVisibility(View.GONE);
    problemBtn.setVisibility(View.GONE);
    doneBtn.setEnabled(false);

    if (textBuilder.length() > 0) textBuilder.append('\n');
    textBuilder.append(getString(R.string.donate_downloading));
    textBuilder.append(" ... ");
    textMarker = textBuilder.length();
    textBuilder.append("0%");
    textView.setText(textBuilder);

    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(SUPPORT_URL));
    request.setDescription(SUPPORT_FILENAME);
    request.setTitle(getString(R.string.support_app));
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, SUPPORT_FILENAME);

    //noinspection ResultOfMethodCallIgnored
    supportAppFile.delete();
    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
    lastDownload = manager.enqueue(request);

    handler.postDelayed(timerTick, 1000L);
  }

  private final Runnable timerTick = new Runnable(){
    public void run() {
      if (lastDownload < 0) return;
      Cursor c = getDownloadCursor();
      if (c == null) return;
      @SuppressLint("Range")
      long doneBytes = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
      @SuppressLint("Range")
      long totalBytes = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
      long progress = 200*doneBytes+totalBytes/2*totalBytes;
      textBuilder.setLength(textMarker);
      textBuilder.append(progress);
      textBuilder.append("%");
      textView.setText(textBuilder);

      handler.postDelayed(timerTick, 1000L);
    }
  };

  private final BroadcastReceiver onComplete=new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      Log.v("Support app install completed");
      ContentQuery.dumpIntent(intent);
      if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != lastDownload) return;
      downloadDone = false;
      Cursor c = getDownloadCursor();
      if (c != null) {
        @SuppressLint("Range")
        int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
        downloadDone = status == DownloadManager.STATUS_SUCCESSFUL;
        Log.v("Download result:" + downloadDone + " Status:" + status);
      }
      dlMgr.remove(lastDownload);
      lastDownload = -1;

      textBuilder.setLength(textMarker);
      textBuilder.append(getString(downloadDone ? R.string.donate_complete : R.string.donate_failed));
      textView.setText(textBuilder);

      if (downloadDone) {
        startInstallApp();
      } else {
        tryAgainBtn.setVisibility(View.VISIBLE);
        problemBtn.setVisibility(View.VISIBLE);
        cancelBtn.setEnabled(false);
        doneBtn.setEnabled(true);
      }
    }
  };

  private Cursor getDownloadCursor() {
    Cursor c = dlMgr.query(new DownloadManager.Query().setFilterById(lastDownload));
    if (c == null) return null;
    c.moveToFirst();
    return c;
  }

  private void startInstallApp() {
    Log.v("Start support app install");

    cancelBtn.setEnabled(true);
    tryAgainBtn.setVisibility(View.GONE);
    problemBtn.setVisibility(View.GONE);
    doneBtn.setEnabled(false);

    textBuilder.append('\n');
    textBuilder.append(getString(R.string.donate_installing));
    textBuilder.append(" ... ");
    textView.setText(textBuilder);

    if (!startLollipopInstallApp()) startOldInstallApp();
  }

  private void finishInstallApp(boolean success, String message) {
    if (success) message = getString(R.string.donate_complete);
    else if (message == null) message = getString(R.string.donate_failed);
    textBuilder.append(message);
    textView.setText(textBuilder);

    cancelBtn.setEnabled(false);
    doneBtn.setEnabled(true);
    if (success) {
      //noinspection ResultOfMethodCallIgnored
      supportAppFile.delete();
    } else {
      tryAgainBtn.setVisibility(View.VISIBLE);
      problemBtn.setVisibility(View.VISIBLE);
    }
  }

  private static final String PACKAGE_INSTALLED_ACTION =
      "net.anei.cadpage.InstallSupportAppActivity.SESSION_API_PACKAGE_INSTALLED";
  private PackageInstaller.Session session = null;

  private boolean startLollipopInstallApp() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
    try {
      PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
      PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
          PackageInstaller.SessionParams.MODE_FULL_INSTALL);
      params.setAppIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
      params.setAppLabel(getString(R.string.support_app));
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        params.setInstallReason(PackageManager.INSTALL_REASON_DEVICE_SETUP);
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        params.setInstallScenario(PackageManager.INSTALL_SCENARIO_FAST);
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        params.setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE);
      }

      int sessionId = packageInstaller.createSession(params);
      session = packageInstaller.openSession(sessionId);

      try (OutputStream os = session.openWrite("cadpage-support.apk", 0, supportAppFile.length());
           InputStream is = new FileInputStream(supportAppFile)) {
        byte[] buffer = new byte[16384];
        int n;
        while ((n = is.read(buffer)) >= 0) {
          os.write(buffer, 0, n);
        }
      }

      // Create an install status receiver.
      Intent intent = new Intent(this, InstallSupportAppActivity.class);
      intent.setAction(PACKAGE_INSTALLED_ACTION);
      int flags = 0;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_MUTABLE;
      PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

      // Commit the session (this will start the installation workflow).
      session.commit(pendingIntent.getIntentSender());
    } catch (IOException ex) {
      throw new RuntimeException("Couldn't install package", ex);
    } catch (RuntimeException ex) {
      if (session != null) {
        session.abandon();
        session = null;
      }
      throw ex;
    }
    return true;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  protected void onNewIntent(Intent intent) {
    Log.v("InstallSupportAppActivity.onNewIntent()");
    ContentQuery.dumpIntent(intent);
    super.onNewIntent(intent);
    Bundle extras = intent.getExtras();
    if (PACKAGE_INSTALLED_ACTION.equals(intent.getAction())) {
      assert extras != null;
      int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
      String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
      boolean success;
      switch (status) {
        case PackageInstaller.STATUS_PENDING_USER_ACTION:
          // This test app isn't privileged, so the user has to confirm the install.
          Intent confirmIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
          startActivity(confirmIntent);
          return;

        case PackageInstaller.STATUS_SUCCESS:
          success = true;
          break;

        default:
          success = false;
      }

      session.close();
      finishInstallApp(success, message);
    }
  }

  static final int REQUEST_INSTALL = 1;

  private void startOldInstallApp() {
    Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
    intent.setData(Uri.fromFile(supportAppFile));
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
    intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
    intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, getApplicationInfo().packageName);
    //noinspection deprecation
    startActivityForResult(intent, REQUEST_INSTALL);

  }
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == REQUEST_INSTALL) {
      finishInstallApp(resultCode == Activity.RESULT_OK, null);
     } else {
      super.onActivityResult(requestCode, resultCode, intent);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    unregisterReceiver(onComplete);
  }

  /**
   * Start support app installation
   * @param activity current activity
   */
  public static void start(Activity activity) {
    Intent intent = new Intent(activity, InstallSupportAppActivity.class);
    activity.startActivity(intent);
  }
}
