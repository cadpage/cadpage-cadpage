package net.anei.cadpage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import net.anei.cadpage.TopExceptionHandler.TopExceptionThread;

import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;

public class HttpService extends Service {

  public static class HttpRequest {
    
    private Uri uri = null;
    private URL url = null;
    
    private int connectTimeout = 30000;
    private int readTimeout = 15000;
    
    private final boolean uiResult;
    
    private int status = -1;
    private String result = null;
    private String content = null;
    
    public HttpRequest(Uri uri) {
      this(uri, false);
    }
    
    public HttpRequest(Uri uri, boolean uiResult) {
      this.uri = uri;
      this.uiResult = uiResult;
    }
    
    public HttpRequest(URL url) {
      this(url, false);
    }
    
    public HttpRequest(URL url, boolean uiResult) {
      this.url = url;
      this.uiResult = uiResult;
    }
    
    public HttpRequest(Uri uri, boolean uiResult, int connectTimeout, int readTimeout) {
      this.uri = uri;
      this.uiResult = uiResult;
      this.connectTimeout = connectTimeout;
      this.readTimeout = readTimeout;
    }
    
    public HttpRequest(URL url, boolean uiResult, int connectTimeout, int readTimeout) {
      this.url = url;
      this.uiResult = uiResult;
      this.connectTimeout = connectTimeout;
      this.readTimeout = readTimeout;
    }
    
    /**
     * @return return requested URL code
     */
    public URL getUrl() {
      return url;
    }
    
    public int getConnectionTimeout() {
      return connectTimeout;
    }
    
    public int getReadTimeout() {
      return readTimeout;
    }
    
    /**
     * Connect to remote host and return result
     * This should not be called on the main dispatch thread
     */
    void connect() {
      
      // If we weren't passed a URL initially, it is time to convert the
      // Uri to a URL.  This this throws an malformed URL exception, ignore it
      // we will report a bad URL later on
      if (url == null) {
        try {
          url = new URL(uri.toString());
        } catch (MalformedURLException ex) {
          Log.e(ex.getMessage());
        }
      }

      // If we have a URL, connect to it
      if (url == null) {
        Log.i("Sending:" + uri.toString());
        status = 400;
        result = "Bad request:" + uri.toString();
      } else {
        Log.i("Sending:" + url.toString());
        HttpURLConnection connect = null;
        InputStream is = null;
        try {
          connect = (HttpURLConnection)url.openConnection();
          connect.setConnectTimeout(connectTimeout);
          connect.setReadTimeout(readTimeout);
          Log.v("ConnectTimeout:" + connect.getConnectTimeout());
          Log.v("ReadTimeout:" + connect.getReadTimeout());
          connect.connect();
          status = connect.getResponseCode();
          result = connect.getResponseMessage();

          ByteArrayOutputStream os = new ByteArrayOutputStream();
          is = connect.getInputStream();
          int b;
          while ((b = is.read()) >= 0) os.write(b);
          content = os.toString();
        }

        catch (IOException ex) {
          status = 408;
          result = "IO Error";
        }
        catch (SecurityException ex) {
          status = 408;
          result = "Security Exception";
        }
        finally {
          if (is != null)
            try { is.close(); } catch (IOException ignored) {}
          if (connect != null) connect.disconnect();
        }
        Log.i("Result:" + status + ": " + result + '\n' + content);
      }

      // If async result requested, run the result status on our worker thread
      if (! uiResult) {
        process();
      }

      // Otherwise, run result processing on the UI thread
      else {
        CadPageApplication.runOnMainThread(this::process);
      }
    }
    
    void process() {
      if (status != 200) {
        processError(status, result);
      } else {
        processContent(content);
      }
    }

    /**
     * This will be called when the HTTP request returns an error or failure status
     * @param status status
     * @param result result
     */
    protected void processError(int status, String result) {}

    /**
     * This will be called when the HTTP request returns successful result
     * @param content response contents
     */
    protected void processContent(String content) {
      content = HTML_TAGS_PTN.matcher(content).replaceAll("");
      processBody(content);
    }
    private static final Pattern HTML_TAGS_PTN = Pattern.compile("</?(?:html|body)/?>", Pattern.CASE_INSENSITIVE);

    /**
     * This will be called when the HTTP request returns a successful result
     * and contents contains a parsable body component
     * @param body body component from response content
     */
    protected void processBody(String body) {}
  }

  // Wake lock and synchronize lock
  private static PowerManager.WakeLock sWakeLock = null;
  
  // Master request queue
  private static final Queue<HttpRequest> reqQueue = new LinkedList<>();

  @Override
  public void onCreate() {

    SmsPopupUtils.startForeground(this,1, ManageNotification.getMiscNotification(this, R.string.notify_http_request));

    super.onCreate();
    if (!CadPageApplication.initialize(this)) return;


    // Launch the HttpServiceThread that is going to do all of the work
    new HttpServiceThread();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    SmsPopupUtils.startForeground(this,1, ManageNotification.getMiscNotification(this, R.string.notify_http_request));

    if (flags != 0) holdPowerLock(this);
    super.onStartCommand(intent, flags, startId);
    return Service.START_REDELIVER_INTENT;
  }

  @Override
  public void onTimeout(int startId) {
    Log.e("HttpService: >>>>>>>>>>>>>>>>>>>>Short Service Timeout<<<<<<<<<<<<<<<<<<");
    stopSelf();
  }

  @Override
  public void onDestroy() {
    Log.v("Shutting down HttpService");
    if (sWakeLock != null) sWakeLock.release();
  }

  private final class HttpServiceThread extends TopExceptionThread {
    
    public HttpServiceThread() {
      super("HttpServiceThread");
      setPriority(Process.THREAD_PRIORITY_BACKGROUND);
      start();
    }

    @Override
    public void run() {

      while (true) {
        
        // Get next request from request queue.  If there aren't any remaining
        // entries, it is time to kill this service
        HttpRequest req;
        synchronized(reqQueue) {
          req = reqQueue.poll();
          if (req == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) stopForeground(true);
            stopSelf();
            return;
          }
        }
        
        // Build and fire off the Http request
        req.connect();
      }
    } 
  }

  /**
   * Initiate an HTTP request
   * @param context current context
   * @param request HTTP request to be initiated
   */
  public static void addHttpRequest(Context context, HttpRequest request) {
    
    // Everything is synchronized by the request queue
    synchronized (reqQueue) {
      
      // If we haven't established a power wake lock, do that now.
      holdPowerLock(context);
      
      // Add new request to request queue and launch the HttpService
      // We don't need to pass anything, just make sure it got started
      boolean startService = reqQueue.isEmpty();
      reqQueue.add(request);
      if (startService) {
        Intent intent = new Intent(context, HttpService.class);
        SmsPopupUtils.startService(context, intent);
      }
    }
  }

  @SuppressLint("InvalidWakeLockTag")
  private static void holdPowerLock(Context context) {
    synchronized (HttpService.class) {
      if (sWakeLock == null) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Log.LOGTAG+".HttpService");
        sWakeLock.setReferenceCounted(false);
      }
      if(!sWakeLock.isHeld()) sWakeLock.acquire(10*60*1000L /*10 minutes*/);
    }
  }

}
