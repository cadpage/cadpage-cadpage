package net.anei.cadpage.contextmenu;

public interface ViewWithContextMenu {

  public void setContextMenuHandler(FragmentWithContextMenu fragment, ContextMenuHandler handler);

  public ContextMenuHandler getContextMenuHandler();
}
