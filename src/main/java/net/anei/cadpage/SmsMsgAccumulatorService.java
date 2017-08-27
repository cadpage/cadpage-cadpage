package net.anei.cadpage;

import android.app.IntentService;
import android.content.Intent;

public class SmsMsgAccumulatorService extends IntentService {

  public SmsMsgAccumulatorService() {
    super("SmsMsgAccumlatorService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    try {
      SmsMsgAccumulator.instance().handleIntent(this, intent);
    }

      // Any exceptions that get thrown should be rethrown on the dispatch thread
    catch( final Exception ex){
        TopExceptionHandler.reportException(ex);
    }
  }
}
