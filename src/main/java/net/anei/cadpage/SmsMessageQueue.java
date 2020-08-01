package net.anei.cadpage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import net.anei.cadpage.contextmenu.ContextMenuHandler;
import net.anei.cadpage.contextmenu.FragmentWithContextMenu;
import net.anei.cadpage.contextmenu.ViewWithContextMenu;

public class SmsMessageQueue implements Serializable {
  
  private static final long serialVersionUID = 1L;

  private static final String QUEUE_FILENAME = "message.queue";
  
  private List<SmsMmsMessage> queue = new ArrayList<>();
  private int nextMsgId = 1;
  private final Context context;
  private Adapter adapter = null;
  private int newCallCount = 0;

  @SuppressWarnings("unchecked")
  private SmsMessageQueue(Context context) {
    this.context = context;
    if (Log.DEBUG) Log.v("SmsMessageQueue: Start");
    ObjectInputStream is = null;
    try {
      is = new ObjectInputStream(
          context.openFileInput(QUEUE_FILENAME));
      queue = (ArrayList<SmsMmsMessage>) is.readObject();
    } catch (FileNotFoundException ignored) {
    } catch (Exception ex) {
      Log.e(ex);
    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException ignored) {
      }
    }

    // Set the next message ID to one more than the highest message ID
    // in the queue, and fix any obsolete location codes
    boolean assign = false;
    for (SmsMmsMessage msg : queue) {
      int msgId = msg.getMsgId();
      if (msgId == 0) assign = true;
      if (msgId >= nextMsgId) nextMsgId = msgId + 1;
    }

    // First time this release is loaded, the saved messages won't have any
    // message ID's, so they will have to be assigned now.
    if (assign) {
      for (SmsMmsMessage msg : queue) {
        if (msg.getMsgId() == 0) msg.setMsgId(nextMsgId++);
      }
    }

    // Update new call count
    calcNewCallCount();

    // Launch reparser startup thread
    ParserServiceManager.startup();
  }

  /**
   * Save queue to persistent file
   */
  private synchronized void save() {
    ObjectOutputStream os = null;
    if (Log.DEBUG) Log.v("SmsMessageQueue: Save");
    try {
      os = new ObjectOutputStream(
        context.openFileOutput(QUEUE_FILENAME, Context.MODE_PRIVATE));
      os.writeObject(queue);
    } catch (IOException ex) {
      Log.e(ex);
    } finally {
      if (os != null) try {os.close();} catch (IOException ignored) {}
    }
  }
  
  /**
   * Rebuild parse information (if necessary) after the insert blanks between
   * spit message option has changed
   * @param changeCode Level of split message option change<br>
   * 0 - No Change<br>
   * 1 - keep lead break option change<br>
   * 2 - insert blank option change<br>
   * 3 - merge message option change
   */
  public void splitOptionChange(int changeCode) {
    ParserServiceManager.reparseSplitMsg(changeCode);
  }
  
  /**
   * Attempt to reparse any messages parsed with general parsers
   * after the location parser setting has been changed
   */
  public void reparseGeneral() {
    ParserServiceManager.reparseGeneral();
  }

  /**
   * Notify call history list that something has changed and view needs to
   * be refreshed
   */
  public void notifyDataChange() {
    notifyDataChange(false);
  }
  
  /**
   * Notify call history list that something has changed and view needs to 
   * be refreshed
   * @param displayOnly true if only transient information has been changed
   */
  public void notifyDataChange(boolean displayOnly) {
	  if (Log.DEBUG) Log.v("SmsMessageQueue: notifyDataChange");  
    if (adapter != null) adapter.notifyDataSetChanged();
    if (!displayOnly) save();
  }
  
  /**
   * Add new message to queue and delete any old messages down to the
   * requested history queue count
   * @param msg message to be added
   */
  public synchronized void addNewMsg(SmsMmsMessage msg) {
	  if (Log.DEBUG) Log.v("SmsMessageQueue: addNewMsg");
    // In theory, the next message ID will overflow after a couple thousand
    // years.  Sounds unlikely, but we must at least consider the possibility
    if (nextMsgId < 0) clearAll();
    
    // Log new message ID while we are trying to track a mysterious problem
    // where Cadpage occasionally brings up a wrong or inappropriate message
    Log.w("New Message " + nextMsgId + ": " + msg.getMessageBody());
    
    // Assign next msg ID
    msg.setMsgId(nextMsgId++);
    
    // Add message to beginning of queue
    queue.add(0, msg);
    
    // Get history limit
    int limit = ManagePreferences.historyCount();
    
    // Do we have to delete anything
    int deleteCnt = queue.size() - limit;
    if (deleteCnt > 0) {
      
      // Count the number of messages that can be deleted
      // (read and not locked)
      int availCnt = 0;
      for (SmsMmsMessage m : queue) {
        if (m.canDelete()) availCnt++;
      }
      
      // How many of these need to be kept to get us to the right limit
      int keepCnt = Math.max(0, availCnt - deleteCnt);
      
      // Make another pass through the list deleting anything over the keep limit
      for (Iterator<SmsMmsMessage> itr = queue.iterator(); itr.hasNext(); ) {
        SmsMmsMessage m = itr.next();
        if (m.canDelete()) {
          if (keepCnt <= 0) itr.remove();
          else keepCnt--;
        }
      }
    }
    calcNewCallCount();
    notifyDataChange();
  }
  
  /**
   * Return message from queue with specified message ID
   * @param msgId requested message ID
   * @return requested message if found, null otherwise
   */
  public synchronized SmsMmsMessage getMessage(int msgId) {
	  if (Log.DEBUG) Log.v("SmsMessageQueue: getMessage");
    for (SmsMmsMessage msg : queue) {
      if (msgId == msg.getMsgId()) return msg;
    }
    
    // Zero is a special case that returns a test message
    if (msgId == 0) {
      String testMsg = context.getString(R.string.pref_notif_test_title);
      return new SmsMmsMessage("1234567890", testMsg, testMsg,
                                0, SmsMmsMessage.MESSAGE_TYPE_SMS);
    }
    
    // Otherwise return null
    return null;
  }
  
  /**
   * Delete message from message queue (if preset, read and not locked)
   * @param msg message to be deleted
   */
  public synchronized void deleteMessage(SmsMmsMessage msg) {
	  if (Log.DEBUG) Log.v("SmsMessageQueue: deleteMessage");
    // Don't delete unread or locked messages
    if (!msg.canDelete()) return;
    queue.remove(msg);
    notifyDataChange();
  }
  
  /**
   * Mark all messages as opened
   */
  public synchronized void markAllRead() {
    if (Log.DEBUG) Log.v("SmsMessageQueue: markAllOpened");
    for (SmsMmsMessage msg : queue) msg.setRead(true);
    notifyDataChange();
  }
  
  /**
   * Remove all expendable (read and not locked) messages
   */
  public synchronized void clearAll() {
	  if (Log.DEBUG) Log.v("SmsMessageQueue: clearAll");
    // Delete everything this has been read and isn't locked
    for (Iterator<SmsMmsMessage> itr = queue.iterator(); itr.hasNext(); ) {
      SmsMmsMessage m = itr.next();
      if (m.canDelete()) itr.remove();
    }
    
    if (queue.isEmpty()) nextMsgId = 1;
    
    notifyDataChange();
  }

  /**
   * @return true if message queue contains any text alerts
   */
  public synchronized boolean containsTextAlerts() {
    for (SmsMmsMessage msg : queue) {
      if (msg.isTextAlert()) return true;
    }
    return false;
  }

  /**
   * Retrieve list of messages in the queue at this moment.  Returned as an array that can be
   * safely worked on a worker thread free of concerns about changes made to the original queue.
   * @return list of message currently in message queue
   */
  public synchronized SmsMmsMessage[] getMessageList() {
    return queue.toArray(new SmsMmsMessage[0]);
  }
  
  /**
   * @return RecyclerView.Adapter that can be bound to RecyclerView
   */
  public RecyclerView.Adapter<Adapter.ViewHolder> listAdapter(FragmentWithContextMenu fragment) {
    adapter = new Adapter(fragment);
    return adapter;
  }

  public void releaseAdapter() {
    adapter = null;
  }
  
  /**
   * Private ListAdapter class
   */
   private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private final FragmentWithContextMenu fragment;
    private final CadPageActivity activity;

    Adapter(FragmentWithContextMenu fragment) {
      this.fragment = fragment;
      this.activity = (CadPageActivity)fragment.getActivity();
      setHasStableIds(true);
    }

    @NonNull
    @Override
    public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(context)
          .inflate(R.layout.msg_list_item, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final Adapter.ViewHolder holder, int position) {
      holder.setMessage(queue.get(position));
    }

    @Override
    public int getItemCount() {
      return queue.size();
    }

    @Override
    public long getItemId(int position) {
      return queue.get(position).getMsgId();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements ContextMenuHandler {
      private final TextView mDateTimeView;
      private final TextView mCallDescView;
      private final TextView mAddrView;
      private SmsMmsMessage mMessage = null;

      ViewHolder(View view) {
        super(view);
        mDateTimeView = view.findViewById(R.id.HistoryDateTime);
        mCallDescView = view.findViewById(R.id.HistoryCallDesc);
        mAddrView = view.findViewById(R.id.HistoryAddress);

        ((ViewWithContextMenu)view).setContextMenuHandler(fragment, this);

        view.setOnClickListener(v -> {

          // Clear any active notification and wake locks
          ClearAllReceiver.clearAll(context);

          if (mMessage == null) return;

          // display message popup
          if (mMessage.updateParseInfo()) SmsMessageQueue.getInstance().notifyDataChange();

          activity.showAlert(mMessage);
        });
      }

      void setMessage(SmsMmsMessage msg) {
        mMessage = msg;
        msg.showHistory(context, mDateTimeView, mCallDescView, mAddrView);
      }

      @Override
      public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (mMessage == null) return;
        MsgOptionManager optMgr = new MsgOptionManager(activity, mMessage);
        optMgr.createMenu(menu, null,false);
      }

      @Override
      public boolean onContextItemSelected(MenuItem item) {

        if (mMessage == null) return false;
        MsgOptionManager optMgr = new MsgOptionManager(activity, mMessage);
        return optMgr.menuItemSelected(item.getItemId(), false);
      }
    }
  }

  /**
   * Recalculate unopened call count
   */
  public void calcNewCallCount() {
    int oldCount = newCallCount;
    newCallCount = 0;
    for (SmsMmsMessage msg : queue) {
      if (!msg.isRead()) newCallCount++;
    }
    if (newCallCount != oldCount) CadPageWidget.update(context);
  }
  
  /**
   * @return unopened call count
   */
  public int getNewCallCount() {
    return newCallCount;
  }

  /**
   * @param force force popup even when not configured
   * @return message to be displayed when Cadpage starts up, or null if no
   * message should be displayed
   */
  public SmsMmsMessage getDisplayMessage(boolean force) {
    
    // We don't display a message if there are no queued message, or if the
    // automatic popup is not enabled
    if (queue.size() == 0) return null;
    if (!force && !ManagePreferences.popupEnabled()) return null;
    
    // First message in queue will be displayed if it has not yet been opened
    SmsMmsMessage msg = queue.get(0);
    if (msg.isRead()) return null;
    return msg;
  }
  
  @SuppressLint("StaticFieldLeak")
  private static SmsMessageQueue msgQueue = null;
  
  /**
   * Set up singleton message queue object
   * @param context context used to create object
   */
  synchronized public static void setupInstance(Context context) {
	  if (Log.DEBUG) Log.v("SmsMessageQueue: SetupInstance");
    if (msgQueue == null) {
      msgQueue = new SmsMessageQueue(context);
    }
  }
  
  /**
   * @return singleton message queue object
   */
  public static SmsMessageQueue getInstance() {
    return msgQueue;
  }
}
