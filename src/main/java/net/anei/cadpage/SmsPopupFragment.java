package net.anei.cadpage;

import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.parsers.MsgInfo;
import net.anei.cadpage.vendors.VendorManager;

public class SmsPopupFragment extends DialogFragment implements LocationTracker.LocationChangeListener {

  private View mainView = null;
  private ImageView fromImage;
  private TextView fromTV;
  private TextView messageReceivedTV;
  private TextView messageTV;

  private Button donateStatusBtn;
  private ViewGroup respBtnLayout;
  private ViewGroup mainBtnLayout;

  private SmsMmsMessage message = null;
  private MsgOptionManager optManager = null;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
  }

  public int getMsgId() {
    if (message == null) return -1;
    return message.getMsgId();
  }

  public void setMsgId(int msgId) {
    SmsMmsMessage msg = (msgId < 0 ? null : SmsMessageQueue.getInstance().getMessage(msgId));
    setMessage(msg);
  }

  public void setMessage(SmsMmsMessage msg) {
    message = msg;
    if (visible) startTracking();
    populateViews();
  }

  public SmsMmsMessage getMessage() {
    return message;
  }

  // This is a workaround for a probably bug in the latest support library.  It does a great job
  // of populating and display the context menu, but the never calls our onContextItemSelected()
  // method.  Instead it calls the dialog onMenuItemSelected() method which always returns false.
  // We get around that by creating our own Dialog subclass that transfers these calls to the
  // correct fragment method

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    return new PopupDialog();
  }

  private class PopupDialog extends Dialog {

    public PopupDialog() {
      super(requireContext(), getTheme());
      setCanceledOnTouchOutside(false);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
      if (featureId != Window.FEATURE_CONTEXT_MENU) return false;
      return SmsPopupFragment.this.onContextItemSelected(item);
    }

    @Override
    public void onBackPressed() {

      // Suppress back activity if response button menu is visible
      if (ManageNotification.isActiveNotice()) return;

      // Otherwise carry on with back function
      super.onBackPressed();

      // Clear any active notification and wake locks
      ClearAllReceiver.clearAll(getContext());

      // Flag message acknowledgment
      SmsMmsMessage message = getMessage();
      if (message != null) message.acknowledge(getContext());
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {

    mainView = inflater.inflate(R.layout.popup, container, false);

    // Find the main textviews
    fromImage = mainView.findViewById(R.id.FromImageView);
    fromTV = mainView.findViewById(R.id.FromTextView);
    messageTV = mainView.findViewById(R.id.MessageTextView);
    messageTV.setAutoLinkMask(Linkify.WEB_URLS);
    messageReceivedTV = mainView.findViewById(R.id.HeaderTextView);

    donateStatusBtn = mainView.findViewById(R.id.donate_status_button);
    respBtnLayout = mainView.findViewById(R.id.RespButtonLayout);
    mainBtnLayout = mainView.findViewById(R.id.RegButtonLayout);

    // Enable long-press context menu
    registerForContextMenu(mainView);

    // Populate display fields
    populateViews();

    return mainView;
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    populateViews();
  }

  /*
   * Populate all the main SMS/MMS views with content from the actual
   * SmsMmsMessage
   */
  private void populateViews() {

    if (mainView == null) return;
    CadPageActivity activity = (CadPageActivity)getActivity();
    if (activity ==  null) return;

    if (message == null) {
      mainView.setVisibility(View.INVISIBLE);
    }

    else {

      mainView.setVisibility(View.VISIBLE);

      // Flag message read
      if (!message.isRead()) {
        message.setRead(true);
        SmsMessageQueue.getInstance().notifyDataChange();
      }

      // Set up regular button list
      optManager = new MsgOptionManager(activity, message);
      optManager.setupButtons(respBtnLayout, mainBtnLayout);

      MsgInfo info = message.getInfo();

      // Hook the donate status button with the current donation status
      MainDonateEvent.instance().setButton(getActivity(), donateStatusBtn, message);

      // Update Icon to indicate direct paging source
      int resIcon = VendorManager.instance().getVendorIconId(message.getVendorCode());
      if (resIcon <= 0) resIcon = R.drawable.ic_launcher;
      fromImage.setVisibility(View.VISIBLE);
      fromImage.setImageResource(resIcon);

      // Update TextView that contains the timestamp for the incoming message
      String headerText;
      String timeStamp = message.getFormattedTimestamp(activity).toString();
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
        if (info.getAlert().length() > 0) {
          sb.append(info.getAlert());
          sb.append('\n');
        }
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
        if (info.getSupp().length() > 0) {
          sb.append(info.getSupp());
          sb.append('\n');
        }
        if (info.getCallId().length() > 0) {
          sb.append(getString(R.string.call_id_label));
          sb.append(info.getCallId());
          sb.append('\n');
        }

        // Remove trailing \n
        int len = sb.length();
        if (len > 0) sb.setLength(len - 1);
        detailText = sb.toString();
      }
      messageReceivedTV.setText(headerText);
      messageTV.setText(detailText);
    }
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
   */
  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);

    if (optManager != null) optManager.createMenu(menu, inflater,true);
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
   */
  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    if (optManager != null) optManager.prepareMenu(menu);
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
   */
  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (optManager != null && optManager.menuItemSelected(item.getItemId())) return true;
    return super.onOptionsItemSelected(item);
  }

  /*
   * Create Context Menu (Long-press menu)
   */
  @Override
  public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    optManager.createMenu(menu, null,true);
  }

  /*
   * Context Menu Item Selected
   */
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (optManager.menuItemSelected(item.getItemId())) return true;
    return super.onContextItemSelected(item);
  }

  boolean visible = false;

  @Override
  public void onStart() {
    visible = true;
    super.onStart();
    startTracking();
  }

  @Override
  public void onStop() {
    visible = false;
    if (Log.DEBUG) Log.v("SmsPopupActivty.onStop()");
    super.onStop();
    startTracking(false);

  }

  private void startTracking() {
    startTracking(message != null);
  }

  private boolean trackingActive = false;

  /**
   * Start location tracking if OSM mapping option is selected, just in case we need it
   * @param start true if tracking should be started, false if it should be stopped
   */
  private void startTracking(boolean start) {

    // If OSM mapping is  not selected, we always turn this off
    if (start) {
      String mapOption = ManagePreferences.appMapOption();
      if (!mapOption.equals("OsmAnd")) start = false;
    }

    // enable or disable location tracking as required
    if (start && ! trackingActive) {
      trackingActive = true;
      LocationTracker.instance().start(requireActivity(), 10, 10, this);
    }
    else if (!start && trackingActive) {
      trackingActive = false;
      LocationTracker.instance().stop(requireActivity(), this);
    }
  }

  @Override
  public void locationChange(Location location) {
    // We do not actually do anything with the location.  Just need to keep tracking active
  }

}
