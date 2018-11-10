package net.anei.cadpage;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
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
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    RecyclerView view = (RecyclerView) inflater.inflate(R.layout.fragment_callhistory_list, container, false);

    // Set the adapter
    Context context = view.getContext();
    view.setLayoutManager(new LinearLayoutManager(context));
    view.setAdapter(SmsMessageQueue.getInstance().listAdapter(this));
    return view;
  }
}
