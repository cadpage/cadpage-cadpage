package net.anei.cadpage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import net.anei.cadpage.contextmenu.FragmentWithContextMenu;

public class CallHistoryFragment extends FragmentWithContextMenu {

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public CallHistoryFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    if (Log.DEBUG) Log.v("CallHistoryFragment.onCreateView() = Activity:" + getActivity());
    RecyclerView view = (RecyclerView) inflater.inflate(R.layout.fragment_callhistory_list, container, false);

    // Set the adapter
    Context context = view.getContext();
    view.setLayoutManager(new LinearLayoutManager(context));
    view.setAdapter(SmsMessageQueue.getInstance().listAdapter(this));
    return view;
  }

  @Override
  public void onDetach() {
    SmsMessageQueue.getInstance().releaseAdapter();
    super.onDetach();
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
   */
  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);

    inflater.inflate(R.menu.history_menu, menu);
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    // Handle item selection
    SmsMessageQueue msgQueue = SmsMessageQueue.getInstance();
    switch (item.getItemId()) {

      case R.id.settings_item:
        SmsPopupConfigActivity.launchSettings(getActivity());
        return true;

      case R.id.markallopened_item:
        msgQueue.markAllRead();
        return true;

      case R.id.clearall_item:
        new DeleteAllDialogFragment(getActivity()).show(getParentFragmentManager(), "delete-all");
        return true;

      case R.id.exit_item:
        requireActivity().finish();
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public static class DeleteAllDialogFragment extends DialogFragment {

    private final Activity activity;

    public DeleteAllDialogFragment(Activity activity) {
      this.activity = activity;
    }
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle bundle) {
      return new AlertDialog.Builder(activity)
              .setIcon(R.drawable.ic_launcher)
              .setTitle(R.string.confirm_delete_all_title)
              .setMessage(R.string.confirm_delete_all_text)
              .setPositiveButton(R.string.yes, (dialog, which) -> SmsMessageQueue.getInstance().clearAll())
              .setNegativeButton(R.string.no, null)
              .create();

    }
  }
}
