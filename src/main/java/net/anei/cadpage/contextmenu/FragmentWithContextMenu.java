package net.anei.cadpage.contextmenu;

import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

public class FragmentWithContextMenu extends Fragment {

  ContextMenuHandler handler = null;

  @Override
  public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {

    // I have no idea how this happens, or why we would be responsible
    // but we did have one crash because there was no associated activity
    // so now we check for it
    if (getActivity() == null) return;

    super.onCreateContextMenu(menu, view, menuInfo);
    handler = null;
    if (view instanceof ViewWithContextMenu) {
      handler = ((ViewWithContextMenu)view).getContextMenuHandler();
      if (handler != null) handler.onCreateContextMenu(menu, view, menuInfo);
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (handler != null) {
      if (handler.onContextItemSelected(item)) return true;
    }
    return super.onContextItemSelected(item);
  }
}
