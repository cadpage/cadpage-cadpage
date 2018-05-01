package net.anei.cadpage.contextmenu;

import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

public class FragmentWithContextMenu extends Fragment {

  ContextMenuHandler handler = null;

  @Override
  public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
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
