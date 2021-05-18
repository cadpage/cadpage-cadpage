package net.anei.cadpage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

/**
 * Dummy activity that does nothing more than present a stand alone dialog
 * for non-activity processes that want to display a dialog box
 */
public class NoticeActivity extends AppCompatActivity {
  
  private static final String EXTRAS_TYPE = "net.anei.cadpage.NoticeActivity.TYPE";
  private static final String EXTRAS_PARMS = "net.anei.cadpage.NoticeActivity.PARMS";
  
  private static final int VENDOR_NOTICE_DLG = 1;
  private static final int MISSING_READER_DLG = 2;
  private static final int NEED_PERMISSION_DLG = 3;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!CadPageApplication.initialize(this)) {
      finish();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();

    if (isFinishing()) return;

    Intent intent = getIntent();
    int type = intent.getIntExtra(EXTRAS_TYPE, -1);
    if (type < 0) return;
    
    Bundle extras = intent.getExtras();
    if (extras == null) return;
    String[] parms = intent.getStringArrayExtra(EXTRAS_PARMS);

    new NoticeDialogFragment(type, parms).show(getSupportFragmentManager(),  "notice");
  }

  public static class NoticeDialogFragment extends DialogFragment {

    private int type;
    private String[] parms;

    public NoticeDialogFragment() {
      super();
    }

    public NoticeDialogFragment(int type, String[] parms) {
      this.type = type;
      this.parms = parms;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putInt(EXTRAS_TYPE, type);
      outState.putStringArray(EXTRAS_PARMS, parms);
    }

    @Override
    public void onCreate(Bundle inState) {
      super.onCreate(inState);
      if (inState != null) {
        type = inState.getInt(EXTRAS_TYPE);
        parms = inState.getStringArray(EXTRAS_PARMS);
      }
    }

      @Override
    @NonNull
    public Dialog onCreateDialog(Bundle bundle) {

      Activity activity = getActivity();
      assert(activity != null);

      switch (type) {

        case VENDOR_NOTICE_DLG:
          if (parms != null && parms.length >= 1) {
            String message = parms[0];

            return new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.vendor_notice_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> activity.finish())
                    .setOnCancelListener(dialog -> activity.finish())
                    .create();
          }

        case MISSING_READER_DLG:
          if (parms != null && parms.length >= 2) {
            String readerName = parms[0];
            final String packageName = parms[1];
            String message = activity.getString(R.string.missing_map_page_reader_text);
            message = message.replace("%%", "%");
            message = String.format(message, readerName);

            return new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.missing_map_page_reader_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
                      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                      try {
                        activity.startActivity(intent);
                      } catch (ActivityNotFoundException ex) {
                        Log.w("Failed to launch Google Play Store");
                        ContentQuery.dumpIntent(intent);
                      }
                      activity.finish();
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> activity.finish())
                    .setOnCancelListener(dialog -> activity.finish())
                    .create();
          }

        case NEED_PERMISSION_DLG:
          if (parms != null && parms.length >= 1) {
            String message = parms[0];
            return new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.need_permission_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> activity.finish())
                    .create();
          }
      }

      throw new RuntimeException("Invalid call to NoticeDialogFragment");
    }
  }

  /**
   * Launch call display popup activity
   * @param context context
   * @param message Message to be displayed to user
   */
  public static void showVendorNotice(Context context, String message) {
    showNotice(context, VENDOR_NOTICE_DLG, message);
  }
  
  public static void showMissingReaderNotice(Context context, int readerNameId, String readerPkg) {
    showNotice(context, MISSING_READER_DLG, context.getString(readerNameId), readerPkg);
  }
  
  public static void showNeedPermissionNotice(Context context, String text) {
    showNotice(context, NEED_PERMISSION_DLG, text);
  }

  private static void showNotice(Context context, int type, String ... parms) {
    Intent intent = new Intent(context, NoticeActivity.class);
    if (!(context instanceof Activity) || type == NEED_PERMISSION_DLG) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    intent.putExtra(EXTRAS_TYPE, type);
    intent.putExtra(EXTRAS_PARMS, parms);

    // If we are not allowed to launch background activities, create a full screen notification
    // In actual practice, this can only happen with the vendor notice dialog, so we will skip
    // logic needed to build notification for the other notice dialogs.
    if (type == VENDOR_NOTICE_DLG && CadPageApplication.restrictBackgroundActivity()) {
      ManageNotification.showNoticeNotification(context, context.getString(R.string.vendor_notice_title), parms[0], intent);
    }

    // Otherwise launch activity normally
    else {
      context.startActivity(intent);
    }
  }
}
