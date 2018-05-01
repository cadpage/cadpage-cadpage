package net.anei.cadpage.contextmenu;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class LinearLayoutWithContextMenu extends LinearLayout implements ViewWithContextMenu {

  public LinearLayoutWithContextMenu(Context context) {
    super(context);
  }
  
  public LinearLayoutWithContextMenu(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  private ContextMenuHandler handler;

  @Override
  public void setContextMenuHandler(FragmentWithContextMenu fragment, ContextMenuHandler handler) {
    this.handler = handler;
    setOnCreateContextMenuListener(fragment);
  }

  @Override
  public ContextMenuHandler getContextMenuHandler() {
    return handler;
  }
}
