package com.motorola.motosimuihelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.view.WindowManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RILConstants;
import android.provider.Settings;

public class UpdateNetworkModeActivity extends Activity {
    private Context mContext = null;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message paramMessage) {
            switch (paramMessage.what) {
                case 1:
                    if (((AsyncResult)paramMessage.obj).exception != null)
                        Log.e("MotoSimUiHelper", "Modem preferred network mode updated fail");
                    else
                        Log.d("MotoSimUiHelper", "Modem preferred network mode updated successfully");
                    UpdateNetworkModeActivity.this.finish();
            }
        }
    };

    private Phone mPhone = null;

    public void onConfigurationChanged(Configuration paramConfiguration) {
        Log.d("MotoSimUiHelper", "onConfigurationChanged");
        super.onConfigurationChanged(paramConfiguration);
    }

    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        this.mContext = this;
        this.mPhone = PhoneFactory.getDefaultPhone();
        PackageManager localPackageManager = getPackageManager();
        ComponentName localComponentName = new ComponentName("com.motorola.motosimuihelper", "com.motorola.motosimuihelper.UpdateNetworkModeActivity");

        if (localPackageManager.getComponentEnabledSetting(localComponentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {

/*
            AlertDialog localAlertDialog = new AlertDialog.Builder(this.mContext).setIconAttribute(android.R.attr.alertDialogIcon).setTitle(R.string.network_mode_title).setMessage(R.string.network_mode).setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Settings.Secure.putInt(UpdateNetworkModeActivity.this.mPhone.getContext().getContentResolver(), Settings.Secure.PREFERRED_NETWORK_MODE, RILConstants.NETWORK_MODE_GLOBAL);
                    UpdateNetworkModeActivity.this.mPhone.setPreferredNetworkType(RILConstants.NETWORK_MODE_GLOBAL, UpdateNetworkModeActivity.this.mHandler.obtainMessage(1));
                }

            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    UpdateNetworkModeActivity.this.finish();
                }

            }).create();

            localAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            # FIXME-HASH: "config_sf_slowBlur" is set default to true, so effectively disable the line below
            if (!true)
                localAlertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

            localAlertDialog.show();
*/
            localPackageManager.setComponentEnabledSetting(localComponentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        }

    }
}

