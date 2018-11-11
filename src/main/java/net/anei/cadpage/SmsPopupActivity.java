package net.anei.cadpage;


import android.app.Activity;
import android.os.Bundle;
import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.parsers.MsgInfo;
import net.anei.cadpage.vendors.VendorManager;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class SmsPopupActivity extends Safe40Activity {
  
  private static final String EXTRAS_MSG_ID = "SmsPopupActivity.MSG_ID";
  
  private final PermissionManager permMgr = new PermissionManager(this);
  
  private SmsMmsMessage message;
  private MsgOptionManager optManager;

  private ImageView fromImage;
  private TextView fromTV;
  private TextView messageReceivedTV;
  private TextView messageTV;

  private LinearLayout mainLL = null;
  
  private Button donateStatusBtn = null;

  private static final double WIDTH = 0.9;
  private static final int MAX_WIDTH = 640;

  @Override
  protected void onCreate(Bundle bundle) {
    Log.v("SmsPopupActivity.onCreate()");
    super.onCreate(bundle);

    ManagePreferences.setPermissionManager(permMgr);

    requestWindowFeature(Window.FEATURE_NO_TITLE);

    setContentView(R.layout.popup);
    
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

    resizeLayout();

    // Find the main textviews
    fromImage = findViewById(R.id.FromImageView);
    fromTV = findViewById(R.id.FromTextView);
    messageTV = findViewById(R.id.MessageTextView);
    messageTV.setAutoLinkMask(Linkify.WEB_URLS);
    messageReceivedTV = findViewById(R.id.HeaderTextView);

    // Enable long-press context menu
    mainLL = findViewById(R.id.MainLinearLayout);
    registerForContextMenu(mainLL);
    
    // We can't hook the current donations status here because it may change
    // from msg to message.
    donateStatusBtn = findViewById(R.id.donate_status_button);
    
    // Populate display fields
    populateViews(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    setIntent(intent);

    // Re-populate views with new intent data (ie. new sms data)
    populateViews(intent);
  }

  @Override
  protected void onDestroy() {
    MainDonateEvent.instance().setButton(null, null, null);
    ManagePreferences.releasePermissionManager(permMgr);
    super.onDestroy();
  }
  
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] granted) {
    ManagePreferences.onRequestPermissionsResult(requestCode, permissions, granted);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {

    // Save values from most recent bundle (ie. most recent message)
    outState.putAll(getIntent().getExtras());
    
    super.onSaveInstanceState(outState);
  }


  // Populate views from intent
  private void populateViews(Intent intent) {
    
    // Log startup intent
    ContentQuery.dumpIntent(intent);
    
    // Check to see if Cadpage is operating in restricted mode, and if it is
    // launch the donation status menu.  We'll check the donation status again
    // when this menu is closed
    if (!DonationManager.instance().isEnabled()) {
      MainDonateEvent.instance().doEvent(this, null);
    }
    
    // Retrieve message from queue
    SmsMessageQueue msgQueue = SmsMessageQueue.getInstance();
    int msgId = intent.getIntExtra(EXTRAS_MSG_ID, 0);
    SmsMmsMessage msg = msgQueue.getMessage(msgId);
    
    // This shouldn't be possible, unless someone other than SmsReceiver is
    // sending rouge intents to us.  But we had better catch it, just in case
    if (msg == null) {
      finish();
      return;
    }
    
    // Flag message read
    msg.setRead(true);
    msgQueue.notifyDataChange();

    // Populate views from message
    populateViews(msg);
  }

  /*
   * Handle results of Donation status menu
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    // If Cadpage is still restricted, close activity
    if (!DonationManager.instance().isEnabled()) finish();
  }

  /*
   * Populate all the main SMS/MMS views with content from the actual
   * SmsMmsMessage
   */
  private void populateViews(SmsMmsMessage newMessage) {


    // Store message
    message = newMessage;

    // Set up regular button list
    optManager = new MsgOptionManager(this, message);
    optManager.setupButtons((ViewGroup)findViewById(R.id.RespButtonLayout),
                            (ViewGroup)findViewById(R.id.RegButtonLayout));

    MsgInfo info = message.getInfo();

    // Hook the donate status button with the current donation status
    MainDonateEvent.instance().setButton(this, donateStatusBtn, newMessage);
    
    // Update Icon to indicate direct paging source
    int resIcon = VendorManager.instance().getVendorIconId(message.getVendorCode());
    if (resIcon <= 0) resIcon = R.drawable.ic_launcher; 
    fromImage.setImageResource(resIcon);
    
    // Update TextView that contains the timestamp for the incoming message
    String headerText;
    String timeStamp = message.getFormattedTimestamp(this).toString();
    if (ManagePreferences.showSource()) {
      String source = "";
      if (info != null) source = info.getSource();
      if (source.length() == 0) source = message.getLocation();
      headerText = getString(R.string.src_text_at, source, timeStamp);//
    } else { 
      headerText = getString(R.string.new_text_at, timeStamp);
    }
    
    String detailText;
    
    // Special case if we have no parsed information (which is just about impossible)
    if (info == null) {
      detailText = message.getTitle();
    } 
    
    // Otherwise do things normally
    else {
  
      // Set the from, message and header views
      StringBuilder sb = new StringBuilder(info.getTitle());
      fromTV.setText(sb.toString());
      if (info.noCall()) fromTV.setMaxLines(2);
      sb = new StringBuilder();
      if (info.getPlace().length() > 0) {
        sb.append(info.getPlace());
        sb.append('\n');
      }
      String addr = info.getAddress();
      String apt = info.getApt();
      if (apt.length() > 0) {
        if (addr.length() > 0) addr = addr + ' ';
        addr = addr + getString(R.string.apt_label) + apt;
      }
      if (addr.length() > 0) {
        sb.append(addr);
        sb.append('\n');
      }
      String city = info.getCity();
      String st = info.getState();
      if (st.length() > 0) {
        if (city.length() > 0) city += ", ";
        city += st;
      }
      if (city.length() > 0) {
        sb.append(city);
        sb.append('\n');
      }
      if (info.getCross().length() > 0) {
        sb.append(getString(R.string.cross_label));
        sb.append(info.getCross());
        sb.append('\n');
      }
      if (info.getMap().length() > 0) {
        sb.append(getString(R.string.map_label));
        sb.append(info.getMap());
        sb.append('\n');
      }
      if (info.getBox().length() > 0) {
        sb.append(getString(R.string.box_label));
        sb.append(info.getBox());
        sb.append('\n');
      }
      if (info.getUnit().length() > 0) {
        sb.append(getString(R.string.units_label));
        sb.append(info.getUnit());
        sb.append('\n');
      }
      if (ManagePreferences.showPersonal()) {
        if (info.getName().length() > 0) {
          sb.append(getString(R.string.name_label));
          sb.append(info.getName());
          sb.append('\n');
        }
        if (info.getPhone().length() > 0) {
          sb.append(getString(R.string.phone_label));
          sb.append(info.getPhone());
          sb.append('\n');
        }
      }
      if (info.getChannel().length() > 0) {
        sb.append(getString(R.string.channel_label));
        sb.append(info.getChannel());
        sb.append('\n');
      }
      if (info.getSupp().length() >0) {
        sb.append(info.getSupp());
        sb.append('\n');
      }
      if (info.getCallId().length() >0) {
        sb.append(getString(R.string.call_id_label));
        sb.append(info.getCallId());
        sb.append('\n');
      }
      
      // Remove trailing \n
      int len = sb.length();
      if (len > 0) sb.setLength(len-1);
      detailText = sb.toString();
    }
    messageReceivedTV.setText(headerText);
    messageTV.setText(detailText);
    
    // There used to be a call to myFinish() that was invoked if this method was
    // passed a message that was not a CAD page.  I am about as certain as I can
    // possibly be that this is no longer possible, which is why this call no
    // longer exists.  But the comment remains as a possible clue if someone
    // should discover I was wrong.
    
    //Will add a Database method in the future.
    //storeFileMessage();
    
  } //end of function

  /* (non-Javadoc)
   * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    
    if (optManager != null) optManager.createMenu(menu, true);
    return true;
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
   */
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    if (optManager != null) optManager.prepareMenu(menu);
    return true;
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (optManager != null && optManager.menuItemSelected(item.getItemId(), true)) return true;
    return super.onOptionsItemSelected(item);
  }

  /*
   * Create Context Menu (Long-press menu)
   */
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    optManager.createMenu(menu, true);
  }

  /*
   * Context Menu Item Selected
   */
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (optManager.menuItemSelected(item.getItemId(), true)) return true;
    return super.onContextItemSelected(item);
  }

  /**
   * Back key pressed
   */
@Override
  public void onBackPressed() {
  
    // Suppress back activity if response button menu is visible
    if (ManageNotification.isActiveNotice()) return;
    
    // Otherwise carry on with back function
    super.onBackPressed();
    
    // Clear any active notification and wake locks
    ClearAllReceiver.clearAll(this);
    
    // Flag message acknowledgment
    message.acknowledge(this);
  }

  private void resizeLayout() {
    // This sets the minimum width of the activity to a minimum of 80% of the screen
    // size only needed because the theme of this activity is "dialog" so it looks
    // like it's floating and doesn't seem to fill_parent like a regular activity
    if (mainLL == null) {
      mainLL = findViewById(R.id.MainLinearLayout);
    }
    Display d = getWindowManager().getDefaultDisplay();

    int width = d.getWidth() > MAX_WIDTH ? MAX_WIDTH : (int) (d.getWidth() * WIDTH);

    mainLL.setMinimumWidth(width);
    mainLL.invalidate();
  }

  @Override
  protected void onStart() {
    if (Log.DEBUG) Log.v("SmsPopupActivty.onStart()");
    super.onStart();
  }

  @Override
  protected void onRestart() {
    if (Log.DEBUG) Log.v("SmsPopupActivty.onRestart()");
    super.onRestart();
  }

  @Override
  protected void onResume() {
  if (Log.DEBUG) Log.v("SmsPopupActivty.onResume()");
    super.onResume();
  }

  @Override
  protected void onPause() {
    if (Log.DEBUG) Log.v("SmsPopupActivty.onPause()");
    super.onPause();
  }

  @Override
  protected void onStop() {
    if (Log.DEBUG) Log.v("SmsPopupActivty.onStop()");
    super.onStop();
  }

  /**
   * Launch call display popup activity
   * @param activity parent activity
   * @param message message to be displayed
   */
  public static void launchActivity(Activity activity, SmsMmsMessage message) {
    launchActivity(activity, message.getMsgId());
  }
  
  /**
   * Launch call display popup activity
   * @param activity parent activity
   * @param msgId message ID of message to be displayed
   */
  public static void launchActivity(Activity activity, int msgId) {
    Intent popup = new Intent(activity, SmsPopupActivity.class);
    popup.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    popup.putExtra(EXTRAS_MSG_ID, msgId);
    ContentQuery.dumpIntent(popup);
    activity.startActivityForResult(popup, 0);
  }
}

