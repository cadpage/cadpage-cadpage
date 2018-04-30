package net.anei.cadpage.contextmenu;

import android.view.MenuItem;
import android.view.View;

/**
 * Interface for objects that wish to create and handle selections from a context
 * menu associated with a view
 */
public interface ContextMenuHandler extends View.OnCreateContextMenuListener {

  boolean onContextItemSelected(MenuItem item);
}
