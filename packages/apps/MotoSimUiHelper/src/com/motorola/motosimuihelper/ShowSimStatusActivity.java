package com.motorola.motosimuihelper;

import android.app.Service;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import com.android.internal.telephony.TelephonyIntents;
import android.telephony.ServiceState;
import android.provider.Settings;
import java.lang.reflect.Field;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ShowSimStatusActivity  extends Service {
    static final String TAG = "MotoSimUiHelper";

    private static String ACTION_SIM_SHOW = "com.motorola.motosimuihelper.SIM_SHOW_INTENT";

    static final int EF_HPLMNACT_ID = 0x6f62;
    static final int COMMAND_READ_BINARY = 0xb0;

    static final int DEFAULT_DELAY = 1000;
    static final int DEFAULT_FIRST_LTE_ABORT_DELAY = 6000;
    static final int DEFAULT_LTE_ABORT_DELAY = 45000;

    private Phone mPhone = null;
    private boolean mInAirplaneMode = false;
    private boolean mPrevInAirplaneMode = false;
    private CommandsInterface mCM = null;
    private Context mContext = null;
    private ShowSimStatusReceiver mShowSimStatusReceiver;
    private boolean mSimLoaded = false;
    public int mPreferredNetwork = 0;
    public int mAbortCounter = 0;


    private Runnable updateNetworkModeGSM = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "[SHOWSIMSTATUS] NETWORK MODE CHANGE: GSM_ONLY");
            ShowSimStatusActivity.this.mPhone.setPreferredNetworkType(Phone.NT_MODE_GSM_ONLY, ShowSimStatusActivity.this.mHandler.obtainMessage(2));
        }
    };

    private Runnable updateNetworkModeLTE = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "[SHOWSIMSTATUS] NETWORK MODE CHANGE: LTE");
            ShowSimStatusActivity.this.mPhone.setPreferredNetworkType(Phone.NT_MODE_GLOBAL, ShowSimStatusActivity.this.mHandler.obtainMessage(3));
            mAbortCounter += 1;
            if (mAbortCounter <= 1) {
                // We crash out pretty quick on the first LTE toggle, so set a short delay
                ShowSimStatusActivity.this.mHandler.postDelayed(abortNetworkModeLTE, DEFAULT_FIRST_LTE_ABORT_DELAY);
            }
            else
                ShowSimStatusActivity.this.mHandler.postDelayed(abortNetworkModeLTE, DEFAULT_LTE_ABORT_DELAY);
        }
    };

    private Runnable abortNetworkModeLTE = new Runnable() {
        @Override
        public void run() {
            mSimLoaded = false;
            Log.e(TAG, "[SHOWSIMSTATUS] NETWORK MODE CHANGE: LTE ABORT ABORT");
            ShowSimStatusActivity.this.mHandler.removeCallbacks(updateNetworkModeGSM);
            ShowSimStatusActivity.this.mHandler.postDelayed(updateNetworkModeGSM, DEFAULT_DELAY);
        }
    };

    private Runnable updateNetworkModePreferred = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "[SHOWSIMSTATUS] NETWORK MODE CHANGE: PREFERRED");
            ShowSimStatusActivity.this.mPhone.setPreferredNetworkType(ShowSimStatusActivity.this.mPreferredNetwork, ShowSimStatusActivity.this.mHandler.obtainMessage(4));
        }
    };

    private Handler mHandler = new Handler() {

        public void handleMessage(Message paramMessage) {
            switch (paramMessage.what) {

                case 1:
                    Log.d(TAG, "[SHOWSIMSTATUS] ----- ACTION SIM_STATE_CHANGE :: EVENT_READ_RECORD_DONE Message");
                    AsyncResult localAsyncResult = (AsyncResult)paramMessage.obj;
                    IccIoResult ioResult = (IccIoResult)localAsyncResult.result;
                    if (localAsyncResult.exception == null) {
                        if (ioResult.getException() == null) {
                            if ((0x40 & ioResult.payload[3]) == 0) {
                                Log.e(TAG, "[SHOWSIMSTATUS] ERROR: EUTRAN is not avaliable");
                            }
                            else {
                                Log.d(TAG, "[SHOWSIMSTATUS] MSG: EUTRAN is avaliable");
                                String lineNum = ShowSimStatusActivity.this.mPhone.getLine1Number();
                                if ((lineNum != null) && (!lineNum.startsWith("00000"))) {
                                    Log.d(TAG, "[SHOWSIMSTATUS] MSG: SIM is a valid activated Verizon 4G SIM");
                                    ShowSimStatusActivity.this.mSimLoaded = true;
                                    removeCallbacks(updateNetworkModeLTE);
                                    postDelayed(updateNetworkModeLTE, DEFAULT_DELAY);
                                }
                            }
                        }
                        else {
                            Log.e(TAG, "[SHOWSIMSTATUS] ERROR: EFHPLMNWACT not accessible.");
                        }
                    }
                    else {
                        Log.e(TAG, "[SHOWSIMSTATUS] ERROR: Read icc i/o exception");
                    }
                    break;

                case 2:
                    if (((AsyncResult)paramMessage.obj).exception != null) {
                        Log.e(TAG, "[SHOWSIMSTATUS] ERROR: Set GSM mode update fail");
                    }
                    else {
                        Log.e(TAG, "[SHOWSIMSTATUS] MSG: Set GSM mode update success");
                    }
                    break;

                case 3:
                    if (((AsyncResult)paramMessage.obj).exception != null) {
                        Log.e(TAG, "[SHOWSIMSTATUS] ERROR: Set LTE mode update fail");
                    }
                    else {
                        Log.e(TAG, "[SHOWSIMSTATUS] MSG: Set LTE mode update success");
                        int i = Settings.Secure.getInt(ShowSimStatusActivity.this.mContext.getContentResolver(), Settings.Secure.PREFERRED_NETWORK_MODE, Phone.NT_MODE_WCDMA_ONLY);
                        if (i != Phone.NT_MODE_GLOBAL) {
                            ShowSimStatusActivity.this.mPreferredNetwork = i;
                            removeCallbacks(updateNetworkModePreferred);
                            postDelayed(updateNetworkModePreferred, DEFAULT_DELAY);
                        }
                    }
                    break;

                case 4:
                    if (((AsyncResult)paramMessage.obj).exception != null)
                        Log.e(TAG, "[SHOWSIMSTATUS] ERROR: Set preferred mode update fail");
                    else
                        Log.e(TAG, "[SHOWSIMSTATUS] MSG: Set preferred mode update success");
                    break;

            }
        }

    };

    private int checkSimStatus() {
        TelephonyManager tm = (TelephonyManager)this.mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        Log.d(TAG, "[SHOWSIMSTATUS] MSG: SIM operator " + tm.getSimOperator());
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
                            Log.d(TAG, "[SHOWSIMSTATUS] ERROR: No usim application on ICC card");
                            i = 0;
                        }
                    }
                    else {
                        Log.d(TAG, "[SHOWSIMSTATUS] ERROR: The icc card needs to be provisioned");
                        i = 2;
                    }
                }
                else {
                    Log.d(TAG, "[SHOWSIMSTATUS] ERROR: Unkown SIM operator");
                    i = 1;
                }
        }
        else
             i = 2;
        
        return i;
    }

    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "[SHOWSIMSTATUS] onCreate");
        init();
    }

    private void init() {
/*
        <action android:name="android.intent.action.SIM_STATE_CHANGED" />
        <action android:name="android.intent.action.SERVICE_STATE" />
        <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
 
*/
        this.mContext = this;
        this.mPhone = PhoneFactory.getDefaultPhone();
        try {
            mShowSimStatusReceiver = new ShowSimStatusReceiver();
            IntentFilter localIntentFilter = new IntentFilter();
            localIntentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            localIntentFilter.addAction("android.intent.action.SERVICE_STATE");
            localIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            localIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            Intent localIntent = registerReceiver(mShowSimStatusReceiver, localIntentFilter);
            if (Settings.System.getInt(mPhone.getContext().getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1) {
                mInAirplaneMode = true;
            }
            PhoneProxy localPhoneProxy = (PhoneProxy)ShowSimStatusActivity.this.mPhone;

            if (localPhoneProxy != null) {

                try {
                    Field localField = PhoneProxy.class.getDeclaredField("mCommandsInterface");
                    if (localField != null)
                        localField.setAccessible(true);

                    try {
                        ShowSimStatusActivity.this.mCM = ((CommandsInterface)localField.get(localPhoneProxy));
                    }
                    catch (IllegalAccessException localIllegalAccessException) {
                        Log.e(TAG, "[SHOWSIMSTATUS] ERROR: Cannot access CommandsInterface");
                        return;
                    }
                }
                catch (NoSuchFieldException localNoSuchFieldException) {
                    Log.e(TAG, "[SHOWSIMSTATUS] ERROR: No CommandsInterface found");
                    return;
                }
            }
        }
        catch (Exception ex2) {
            Log.e(TAG, "**** Exception in init(): " + ex2);
        }
    }

    private void handleAirplaneModeChanged(Intent intent) {
        mInAirplaneMode = intent.getBooleanExtra("state", false);
        Log.d(TAG, "[SHOWSIMSTATUS] MSG AirplaneModeChanged set to: " + mInAirplaneMode);
        if (mInAirplaneMode != mPrevInAirplaneMode) {
            Log.d(TAG, "[SHOWSIMSTATUS] ----- ACTION TRIGGER mInAirplaneMode != mPrevInAirplaneMode");
            if (!mSimLoaded && !mInAirplaneMode)
                mHandler.removeCallbacks(updateNetworkModeGSM);
                mHandler.postDelayed(updateNetworkModeGSM, DEFAULT_DELAY);
            if (mInAirplaneMode) {
                Log.d(TAG, "[SHOWSIMSTATUS] ----- ACTION TRIGGER mInAirplaneMode != mPrevInAirplaneMode");
                mSimLoaded = false;
            }
        }
        mPrevInAirplaneMode = mInAirplaneMode;
    }  

    private class ShowSimStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int i = -1;

            if (action.equals("android.intent.action.SERVICE_STATE")) {
                ServiceState sState = ServiceState.newFromBundle(intent.getExtras());
                if (sState != null) {
// SERVICE_STATE [0 home ***  31000  LTE:14 CSS supported 2 2 RoamInd=64 DefRoamInd=64 EmergOnly=false] == NOT A TRIGGER
// SERVICE_STATE [0 home Verizon Wireless  31000  LTE:14 CSS supported 2 2 RoamInd=64 DefRoamInd=64 EmergOnly=false] == NOT A TRIGGER

                    if ((sState.getState() == 0) && (sState.getOperatorAlphaLong() != null)) {
                        if ((sState.getRadioTechnology() >= ServiceState.RADIO_TECHNOLOGY_1xRTT)
                                && (sState.getCdmaRoamingIndicator() >= 64)
                                && (!mSimLoaded)) {
                            Log.d(TAG, "[SHOWSIMSTATUS] ----- ACTION SERVICE_STATE [NOSIM " + sState.toString() + "] == NETWORK STATE CHANGE");
                            mHandler.removeCallbacks(updateNetworkModeGSM);
                            mHandler.postDelayed(updateNetworkModeGSM, DEFAULT_DELAY);
                        }
                        else {
                             Log.d(TAG, "[SHOWSIMSTATUS] ----- ACTION SERVICE_STATE [" + (mSimLoaded ? "SIM " : "NOSIM ") + sState.toString() + "] == NOT A TRIGGER");
                        }
                    }
                    else {
                        Log.d(TAG, "[SHOWSIMSTATUS] ----- ACTION SERVICE_STATE [" + (mSimLoaded ? "SIM " : "NOSIM ") + sState.toString() + "] == NOT A TRIGGER");
                    }
                }
            }
            else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                NetworkInfo info = (NetworkInfo)intent.getExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                String reason = (String)intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
                Log.d(TAG, "[SHOWSIMSTATUS] ----- ACTION CONNECTIVITY_CHANGE");
                if (info != null) {
                    Log.d(TAG, "[SHOWSIMSTATUS] ----- ACTION CONNECTIVITY_CHANGE [NetworkInfo = " + info.toString() + "]");
                }
                if (reason != null) {
                    Log.e(TAG, "[SHOWSIMSTATUS] ----- ACTION CONNECTIVITY_CHANGE [Reason = " + reason + "]");
                    if (reason.equals("radioTurnedOff")) {
                        mSimLoaded = false;
                    }
                    else if ((reason.equals("dependancyMet")) || (reason.equals("dataAttached")) || (reason.equals("dataEnabled"))) {
                        mHandler.removeCallbacks(abortNetworkModeLTE);
                    }
                    else if ((reason.equals("apnChanged")) && (info != null)) {
// ACTION CONNECTIVITY_CHANGE [NetworkInfo = NetworkInfo: type: mobile[LTE], state: CONNECTED/CONNECTED, reason: apnChanged, extra: VZWINTERNET, roaming: false, failover: false, isAvailable: true]
                        if (info.isConnected() && info.isAvailable()) {
                            mHandler.removeCallbacks(abortNetworkModeLTE);
                        }
                    }
                }
            }
            else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                if (!mSimLoaded) {
                    Log.d(TAG, "[SHOWSIMSTATUS] ----- ACTION SIM_STATE_CHANGED START");
                    i = checkSimStatus();
                    Log.d(TAG, "[SHOWSIMSTATUS] ----- ACTION SIM_STATE_CHANGED [CheckSimStatus == " + i + "]");
                }
            }
            else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                handleAirplaneModeChanged(intent);
            }

        }
    }

}

