package net.anei.cadpage;

/**
 * Contains the options to control processing of a particular message class
 */
public class FilterOptions {
  
  private final String options;
  
  public FilterOptions() {
    this("");
  }
  
  public FilterOptions(String options) {
    this.options = options;
  }
  
  public boolean noticeEnabled() {
    return ManageNotification.isNotificationEnabled() && !options.contains("N");
  }
  
  public boolean popupEnabled() {
    return ManagePreferences.popupEnabled() && !options.contains("P");
  }
  
  public boolean historyEnabled() {
    return !options.contains("H");
  }
}
