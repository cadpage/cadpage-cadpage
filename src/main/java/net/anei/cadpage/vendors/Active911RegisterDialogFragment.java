package net.anei.cadpage.vendors;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.HttpService;
import net.anei.cadpage.R;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Active911RegisterDialogFragment extends DialogFragment {

    private static final String ARG_REGISTRATION_ID = "registrationId";

    private EditText txtCode;

    public Active911RegisterDialogFragment() {}

    private String registrationId;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @param registrationId registration ID
     *
     * @return A new instance of fragment Active911RegisterDialogFragment.
     */
    public static Active911RegisterDialogFragment newInstance(String registrationId) {
        Bundle args = new Bundle();
        args.putString(ARG_REGISTRATION_ID, registrationId);
        Active911RegisterDialogFragment frg = new Active911RegisterDialogFragment();
        frg.setArguments(args);
        return frg;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle args = getArguments();
        registrationId = args.getString(ARG_REGISTRATION_ID);

        // Use the Builder class for convenient dialog construction
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.active911_register_dialog, null);
        txtCode = view.findViewById(R.id.activation_code);
        txtCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setRegisterStatus();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setPositiveButton(R.string.vendor_register, (dialog, which) -> sendRegisterReq(txtCode.getText()));
        builder.setNegativeButton(R.string.donate_btn_cancel, null);
        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        setRegisterStatus();
    }

    private void setRegisterStatus() {
        AlertDialog dialog = (AlertDialog)getDialog();
        assert dialog != null;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isCodeValid(txtCode.getText()));
    }

    private static final Pattern CODE_PTN = Pattern.compile("(\\d+)-([A-Za-z]{4,6})");

    private boolean isCodeValid(CharSequence code) {
        Matcher match = CODE_PTN.matcher(code);
        if (!match.matches()) return false;
        String token = match.group(2);
        assert token != null;
        if (token.length() == 4) return true;
        if (token.length() != 6) return false;
        token = token.substring(4).toUpperCase();
        return token.equals("ES") || token.equals("EU");
    }

    private void sendRegisterReq(CharSequence activationCode) {

        Matcher match = CODE_PTN.matcher(activationCode);
        if (!match.matches()) return;
        String device = match.group(1);
        String qual = match.group(2);

        // We need the Active911 vendor for some help
        final Vendor vendor = VendorManager.instance().findVendor("Active911");

        // There is absolutely no way we can get here if Active911 is active
        // but will make a final absolute check before we really mess things up
        if (vendor.isEnabled())
            throw new RuntimeException("Attempt to register Active911 when enabled");

        // Send a reregister request to Active911 and see if it likes the result
        vendor.setAccountToken(device, qual);
        Uri uri = vendor.buildRequestUri("reregister", registrationId, true).buildUpon().appendQueryParameter("userReq", "Y").build();
        HttpService.addHttpRequest(getActivity(), new HttpService.HttpRequest(uri){

            @Override
            protected void processContent(String content) {
                try {
                    JsonReader jr = new JsonReader(new StringReader(content));
                    String result = null;
                    String message = null;
                    jr.beginObject();
                    while (jr.hasNext()) {
                        String name = jr.nextName();
                        if (name.equals("result")) {
                            result = jr.nextString();
                        } else if (name.equals("message")) {
                            message = jr.nextString();
                        } else {
                            jr.skipValue();
                        }
                    }
                    jr.endObject();

                    if (result == null) throw new IOException();
                    if (result.equals("success")) {
                        reportSuccess();
                    } else {
                        if (message == null) throw new IOException();
                        reportFailure(message);
                    }
                } catch (IOException ex) {
                    reportFailure("Request rejected by Active911");
                }
            }

            @Override
            public void processError(int status, String result) {
                reportFailure(result);
            }

            private void reportSuccess() {
                vendor.setEnabled(true);
                vendor.showNotice(CadPageApplication.getContext(), R.string.vendor_connect_msg, null);
            }

            private  void reportFailure(String message) {
                vendor.showNotice(CadPageApplication.getContext(), R.string.vendor_register_fail_msg, message);
            }
        });
    }
}