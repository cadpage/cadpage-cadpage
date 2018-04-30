package net.anei.cadpage.contextmenu;

import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

public class FragmentWithContextMenu extends Fragment {

  ContextMenuHandler handler = null;

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    handler = null;
    if (v instanceof ViewWithContextMenu) {
      handler = ((ViewWithContextMenu)v).getContextMenuHandler();
      if (handler != null) handler.onCreateContextMenu(menu, v, menuInfo);
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
