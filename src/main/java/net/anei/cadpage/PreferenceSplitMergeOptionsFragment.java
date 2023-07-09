package net.anei.cadpage;

import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;

import net.anei.cadpage.parsers.SplitMsgOptions;

public class PreferenceSplitMergeOptionsFragment extends PreferenceRestorableFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_split_merge_options, rootKey);

    EditTextPreference timeoutPref = findPreference(getString(R.string.pref_msgtimeout_key));
    if (timeoutPref != null) {
      timeoutPref.setOnBindEditTextListener((editText) -> {
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
      });
    }
  }

  private boolean oldSplitBlank = false;
  private boolean oldSplitKeepLeadBreak = false;
  private boolean oldRevMsgOrder = false;
  private boolean oldMixedMsgOrder = false;

  @Override
  public void onStart() {

    // Save previous split message options
    SplitMsgOptions options = ManagePreferences.getDefaultSplitMsgOptions();
    oldSplitBlank = options.splitBlankIns();
    oldSplitKeepLeadBreak = options.splitKeepLeadBreak();
    oldRevMsgOrder = options.revMsgOrder();
    oldMixedMsgOrder = options.mixedMsgOrder();

    super.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();

    // If any of the split message options have changed, reparse any possibly affected calls
    SplitMsgOptions options = ManagePreferences.getDefaultSplitMsgOptions();
    boolean splitBlank = options.splitBlankIns();
    boolean splitKeepLeadBreak = options.splitKeepLeadBreak();
    boolean revMsgOrder = options.revMsgOrder();
    boolean mixedMsgOrder = options.mixedMsgOrder();
    int changeCode;
    if (revMsgOrder != oldRevMsgOrder || mixedMsgOrder != oldMixedMsgOrder) changeCode = 3;
    else if (splitBlank != oldSplitBlank) changeCode = 2;
    else if (splitKeepLeadBreak != oldSplitKeepLeadBreak) changeCode = 1;
    else changeCode = 0;
    if (changeCode > 0) SmsMessageQueue.getInstance().splitOptionChange(changeCode);
  }

}
