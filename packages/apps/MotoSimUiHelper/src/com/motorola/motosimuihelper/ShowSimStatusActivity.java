package com.motorola.motosimuihelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardApplication;
import com.android.internal.telephony.IccCardApplication.AppType;
import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.IccIoResult;
import com.android.internal.telephony.IccFileHandler;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import android.provider.Settings;
import java.lang.reflect.Field;

public class ShowSimStatusActivity extends Activity {
    static final int EF_HPLMNACT_ID = 0x6f62;
    static final int COMMAND_READ_BINARY = 0xb0;

    private CommandsInterface mCM = null;
    private Context mContext = null;

    private Handler mHandler = new Handler() {

        public void handleMessage(Message paramMessage) {
            switch (paramMessage.what) {

                case 1:
                    Log.d("MotoSimUiHelper", "Handle EVENT_READ_RECORD_DONE Message");
                    AsyncResult localAsyncResult = (AsyncResult)paramMessage.obj;
                    IccIoResult ioResult = (IccIoResult)localAsyncResult.result;
                    if (localAsyncResult.exception == null) {
                        if (ioResult.getException() == null) {
                            if ((0x40 & ioResult.payload[3]) == 0) {
                                Log.e("MotoSimUiHelper", "EUTRAN is not avaliable");
/* Removing Dialog
                                ShowSimStatusActivity.this.showDialog(ShowSimStatusActivity.this.mContext, 0);
*/
                            }
                            else {
                                Log.d("MotoSimUiHelper", "EUTRAN is avaliable");
                                String lineNum = ShowSimStatusActivity.this.mPhone.getLine1Number();
                                if ((lineNum != null) && (!lineNum.startsWith("00000"))) {
                                    Log.d("MotoSimUiHelper", "SIM is a valid activated Verizon 4G SIM");
                                    ShowSimStatusActivity.this.updateNetworkModeLTE();
                                }
                                // ShowSimStatusActivity.this.finish();
                            }
                        }
                        else {
                            Log.e("MotoSimUiHelper", "EFHPLMNWACT not accessible.");
/* Removing Dialog
                            ShowSimStatusActivity.this.showDialog(ShowSimStatusActivity.this.mContext, 0);
*/
                        }
                    }
                    else {
                        Log.e("MotoSimUiHelper", "read icc i/o exception");
                        // ShowSimStatusActivity.this.finish();
                    }
                    break;

                case 2:
                    if (((AsyncResult)paramMessage.obj).exception != null) {
                        Log.e("MotoSimUiHelper", "Set GSM mode update fail");
                        // ShowSimStatusActivity.this.finish();
                    }
                    else {
                        Log.e("MotoSimUiHelper", "Set GSM mode update success");
                        ShowSimStatusActivity.this.updateNetworkModeLTE();
                    }
                    break;

                case 3:
                    if (((AsyncResult)paramMessage.obj).exception != null) {
                        Log.e("MotoSimUiHelper", "Set LTE mode update fail");
                        // ShowSimStatusActivity.this.finish();
                    }
                    else {
                        Log.e("MotoSimUiHelper", "Set LTE mode update success");
                        int i = Settings.Secure.getInt(ShowSimStatusActivity.this.mContext.getContentResolver(), Settings.Secure.PREFERRED_NETWORK_MODE, Phone.NT_MODE_WCDMA_ONLY);
                        if (i != Phone.NT_MODE_GLOBAL) {
                            ShowSimStatusActivity.this.updateNetworkModePreferred(i);
                        }
                        else {
                            // ShowSimStatusActivity.this.finish();
                        }
                    }
                    break;

                case 4:
                    if (((AsyncResult)paramMessage.obj).exception != null)
                        Log.e("MotoSimUiHelper", "Set preferred mode update fail");
                    else
                        Log.e("MotoSimUiHelper", "Set preferred mode update success");
                    // ShowSimStatusActivity.this.finish();
                    break;

            }
        }

    };

    private Phone mPhone = null;

    private int checkSimStatus() {
        TelephonyManager tm = (TelephonyManager)this.mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        Log.d("MotoSimUiHelper", "SIM operator " + tm.getSimOperator());
        String simOperator = tm.getSimOperator();
        int i;

        if (simOperator != null) {
                if ((simOperator.equals("311480")) || (simOperator.equals("20404"))) {
                    if (!this.mPhone.needsOtaServiceProvisioning()) {
                        if (this.mPhone.getIccCard().isApplicationOnIcc(IccCardApplication.AppType.APPTYPE_USIM)) {
                            this.mCM.iccIO(COMMAND_READ_BINARY, EF_HPLMNACT_ID, IccConstants.MF_SIM + IccConstants.DF_GSM, 0, 0, 5, null, null, this.mHandler.obtainMessage(1));
                            i = 3;
                        }
                        else {
                            Log.d("MotoSimUiHelper", "No usim application on ICC card");
                            i = 0;
                        }
                    }
                    else {
                        Log.d("MotoSimUiHelper", "The icc card needs to be provisioned");
                        i = 2;
                    }
                }
                else {
                    Log.d("MotoSimUiHelper", "Unkown SIM operator");
                    i = 1;
                }
        }
        else
             i = 2;
        
        return i;
    }

/*
    private void showDialog(Context paramContext, int paramInt) {
        AlertDialog localAlertDialog;

        switch (paramInt) {

            case 0:
                localAlertDialog = new AlertDialog.Builder(paramContext).setIconAttribute(android.R.attr.alertDialogIcon).setTitle(R.string.wrong_sim_title).setMessage(R.string.wrong_sim).setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        ShowSimStatusActivity.this.finish();
                    }

                }).create();
                updateNotification(0);
                localAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                if (!true)
                    localAlertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                localAlertDialog.show();
                break;

            case 1:
                localAlertDialog = new AlertDialog.Builder(paramContext).setIconAttribute(android.R.attr.alertDialogIcon).setTitle(R.string.wrong_operator_title).setMessage(R.string.wrong_operator).setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        ShowSimStatusActivity.this.finish();
                    }

                }).create();
                updateNotification(1);
                localAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                if (!true)
                    localAlertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                localAlertDialog.show();
                break;
        }

    }
*/

    private void updateNetworkMode() {
        Log.d("MotoSimUiHelper", "updateNetworkMode");
        PackageManager localPackageManager = getPackageManager();
        ComponentName localComponentName = new ComponentName("com.motorola.motosimuihelper", "com.motorola.motosimuihelper.UpdateNetworkModeActivity");
        int j = localPackageManager.getComponentEnabledSetting(localComponentName);
        int i = Settings.Secure.getInt(this.mContext.getContentResolver(), Settings.Secure.PREFERRED_NETWORK_MODE, Phone.NT_MODE_WCDMA_ONLY);
        Log.d("MotoSimUiHelper", "Current Preferred network mode is " + i);
        if ((i == Phone.NT_MODE_GLOBAL) || (i == Phone.NT_MODE_CDMA)) {
            Log.d("MotoSimUiHelper", "The preferred network mode is GLOBAL or CDMA, disable the updateNetworkMode component");
            localPackageManager.setComponentEnabledSetting(localComponentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        if (((i != Phone.NT_MODE_GLOBAL) && (i != Phone.NT_MODE_CDMA)) && (j != PackageManager.COMPONENT_ENABLED_STATE_DISABLED)) {
            Intent localIntent = new Intent("com.motorola.motosimuihelper.UPDATE_NETWORK_MODE");
            localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            Log.d("MotoSimUiHelper", "start UpdateNetworkModeActivity");
            startActivity(localIntent);
        }
    }

    private void updateNetworkModeLTE() {
        Log.d("MotoSimUiHelper", "updateNetworkModeLTE");
        mPhone.setPreferredNetworkType(Phone.NT_MODE_GLOBAL, mHandler.obtainMessage(3));
    }

    private void updateNetworkModePreferred(int networkMode) {
        Log.d("MotoSimUiHelper", "updateNetworkModePreferred");
        mPhone.setPreferredNetworkType(networkMode, mHandler.obtainMessage(4));
    }

    public void onConfigurationChanged(Configuration paramConfiguration) {
        Log.d("MotoSimUiHelper", "onConfigurationChanged");
        super.onConfigurationChanged(paramConfiguration);
    }

    protected void onCreate(Bundle paramBundle) {
        int i = -1;
        super.onCreate(paramBundle);
        requestWindowFeature(1);
        this.mContext = this;
        this.mPhone = PhoneFactory.getDefaultPhone();
        PhoneProxy localPhoneProxy = (PhoneProxy)this.mPhone;

        if (localPhoneProxy != null) {

            try {
                Field localField = PhoneProxy.class.getDeclaredField("mCommandsInterface");
                if (localField != null)
                    localField.setAccessible(true);

                try {
                    this.mCM = ((CommandsInterface)localField.get(localPhoneProxy));
                    i = checkSimStatus();
                    Log.d("MotoSimUiHelper", "onCreate::CheckSimStatus == " + i);
                    if (1 == i) {
                        int j = Settings.Secure.getInt(this.mContext.getContentResolver(), Settings.Secure.PREFERRED_NETWORK_MODE, Phone.NT_MODE_GSM_ONLY);
                        Log.d("MotoSimUiHelper", "onCreate::PREFERRED_NETWORK_MODE == " + j);
                        if (j != Phone.NT_MODE_GSM_ONLY) {
                            Log.d("MotoSimUiHelper", "updateNetworkModeGSM");
                            mPhone.setPreferredNetworkType(Phone.NT_MODE_GSM_ONLY, mHandler.obtainMessage(2));
                        }
                        else {
                            updateNetworkModeLTE();
                        }
/*
                        Log.d("MotoSimUiHelper", "SIM card is from an unknown operator, show the wrong operator screen");
                        showDialog(this.mContext, 1);
*/
                        finish();
                    }
                }
                catch (IllegalAccessException localIllegalAccessException) {
                    Log.e("MotoSimUiHelper", "Cannot access CommandsInterface");
                    finish();
                    return;
                }
            }
            catch (NoSuchFieldException localNoSuchFieldException) {
                Log.e("MotoSimUiHelper", "No CommandsInterface found");
                finish();
                return;
            }

            if (i == 0) {
                Log.d("MotoSimUiHelper", "SIM card is a RUIM card");
/*
                showDialog(this.mContext, 0);
                return;
*/
            }
            else {
                if (i != 2) return;
                Log.d("MotoSimUiHelper", "This status should be ignore, don't show any screen, exit here");
                finish();
            }

        }
    }


/*
    public void updateNotification(int paramInt) {
        NotificationManager localNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (localNotificationManager != null) {
            Notification localNotification;
            switch (paramInt) {

                case 0:
                    localNotification = new Notification(R.drawable.unknownsim, getString(R.string.wrong_sim_notice), System.currentTimeMillis());
                    localNotification.flags = Notification.FLAG_ONGOING_EVENT;
                    localNotification.setLatestEventInfo(this.mContext, getString(R.string.wrong_sim_notice), null, null);
                    localNotificationManager.notify(0, localNotification);
                    break;

                case 1:
                    localNotification = new Notification(R.drawable.unknownsim, getString(R.string.wrong_operator_notice), System.currentTimeMillis());
                    localNotification.flags = Notification.FLAG_ONGOING_EVENT;
                    localNotification.setLatestEventInfo(this.mContext, getString(R.string.wrong_operator_notice), null, null);
                    localNotificationManager.notify(0, localNotification);
                    break;

                default:
                    break;

            }
        }
    }
*/
}

