package net.anei.cadpage.vendors;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.PermissionManager;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

public class VendorActivity extends AppCompatActivity {

  private final PermissionManager permMgr = new PermissionManager(this);

  private static final String EXTRAS_VENDOR_CODE = "net.anei.cadpage.VendorActivity.VENDOR_CODE";

  private TextView infoTextView;
  private Button moreInfoButton;
  private Button profileButton;
  private Button registerButton;
  private Button unregisterButton;
  
  private Vendor vendor;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!CadPageApplication.initialize(this)) {
      finish();
      return;
    }

    ManagePreferences.setPermissionManager(permMgr);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    WindowCompat.enableEdgeToEdge(getWindow());
    setContentView(R.layout.vendor_popup);

    View view = findViewById(android.R.id.content);
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
      Insets ins = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
      v.setPadding(ins.left, ins.top, ins.right, ins.bottom);
      return WindowInsetsCompat.CONSUMED;
    });

    // We can't do the cool stuff until we get a Vendor code
    // But can set up the button handlers here
    moreInfoButton = findViewById(R.id.more_info_button);
    moreInfoButton.setOnClickListener(v -> vendor.moreInfoReq(VendorActivity.this));
    
    profileButton = findViewById(R.id.profile_button);
    profileButton.setOnClickListener(v -> vendor.profileReq(VendorActivity.this));
    
    infoTextView = findViewById(R.id.VendorInfoView);
    
    registerButton = findViewById(R.id.register_button);
    registerButton.setOnClickListener(v -> vendor.userRegisterReq(VendorActivity.this));
    
    unregisterButton = findViewById(R.id.unregister_button);
    unregisterButton.setOnClickListener(v -> new ConfirmUnregisterDialogFragment(vendor).show(getSupportFragmentManager(), "unregister_dialog"));
    
    Button btn = findViewById(R.id.cancel_button);
    btn.setOnClickListener(v -> VendorActivity.this.finish());
  }

  private static final String VENDOR_CODE = "vendor_code";

  public static class ConfirmUnregisterDialogFragment extends DialogFragment {

    private Vendor vendor;

    ConfirmUnregisterDialogFragment() {
      super();
    }

    ConfirmUnregisterDialogFragment(Vendor vendor) {
      this.vendor = vendor;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putString(VENDOR_CODE, vendor.getCode());
    }

    @Override
    @NonNull public Dialog onCreateDialog(Bundle bundle) {

      if (bundle != null) {
        vendor = VendorManager.instance().findVendor(bundle.getString(VENDOR_CODE));
      }

      Activity activity = getActivity();
      return new AlertDialog.Builder(activity)
      .setMessage(R.string.vendor_confirm_unregister)
      .setPositiveButton(android.R.string.yes, (dialog, which) -> {
        if (SmsPopupUtils.haveNet(activity)) {
          vendor.unregisterReq(activity);
        }
      })
      .setNegativeButton(android.R.string.no, null)
      .create();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    Intent intent = getIntent();
    
    // Get the vendor associated with this activity
    vendor = null;
    String vendorCode = intent.getStringExtra(EXTRAS_VENDOR_CODE);
    if (vendorCode != null) vendor = VendorManager.instance().findVendor(vendorCode);
    if (vendor == null) { 
      finish();
      return;
    }
    
    // Register ourselves with this vendor object so it can update our
    // display if the enable status changes
    vendor.registerActivity(this);
    
    // OK, now that we have a vendor object, we can set up all of the display fields
    int resId = vendor.getIconId();
    int logoId = vendor.getLogoId();
    View imgTitleView = findViewById(R.id.VendorImageTitleView);
    ImageView logoView = findViewById(R.id.VendorLogoView);
    if (logoId > 0) {
      imgTitleView.setVisibility(View.GONE);
      logoView.setVisibility(View.VISIBLE);
      logoView.setImageResource(logoId);
    } else {
      imgTitleView.setVisibility(View.VISIBLE);
      logoView.setVisibility(View.GONE);
      if (resId > 0) {
        ImageView imgView = findViewById(R.id.VendorImageView);
        imgView.setImageResource(resId);
      }
      resId = vendor.getTitleId();
      if (resId > 0) {
        TextView txtView = findViewById(R.id.VendorTitleView);
        txtView.setText(resId);
      }
    }
    
    resId = vendor.getTextId();
    if (resId > 0) {
      TextView txtView = findViewById(R.id.VendorDescriptionView);
      txtView.setText(resId);
    }
    
    // And update the status fields
    update();
  }
  
  /**
   * Update status fields, things that might change while window is displayed
   */
  public void update() {
    
    if (vendor.isEnabled()) {
      infoTextView.setText(vendor.isInactive() ? R.string.vendor_registered_inactive : R.string.vendor_registered);
      infoTextView.setVisibility(View.VISIBLE);
      moreInfoButton.setVisibility(View.GONE);
      profileButton.setVisibility(View.VISIBLE);
      registerButton.setVisibility(View.GONE);
      unregisterButton.setVisibility(View.VISIBLE);
    } else {
      infoTextView.setVisibility(View.GONE);
      moreInfoButton.setVisibility(View.VISIBLE);
      profileButton.setVisibility(View.GONE);
      registerButton.setVisibility(View.VISIBLE);
      unregisterButton.setVisibility(View.GONE);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] granted) {
    super.onRequestPermissionsResult(requestCode, permissions, granted);
    ManagePreferences.onRequestPermissionsResult(requestCode, permissions, granted);
  }

  @Override
  protected void onDestroy() {
    
    // When activity is being destroyed, disconnect it from the vendor object
    vendor.registerActivity(null);

    ManagePreferences.releasePermissionManager(permMgr);
    super.onDestroy();
    
  }

  /**
   * Launch call display popup activity
   * @param context context
   * @param vendor Vendor object
   */
  public static void launchActivity(Context context, Vendor vendor) {
    Intent intent = new Intent(context, VendorActivity.class);
    intent.putExtra(EXTRAS_VENDOR_CODE, vendor.getVendorCode());
    context.startActivity(intent);
  }

}
