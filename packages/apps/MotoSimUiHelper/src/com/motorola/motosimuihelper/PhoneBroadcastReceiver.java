package com.motorola.motosimuihelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.internal.telephony.IccCard;
import android.content.pm.PackageManager;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.RILConstants;
import android.provider.Settings;

public class PhoneBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(Context paramContext, Intent paramIntent) {
        Bundle extraBundle = paramIntent.getExtras();
        String action = paramIntent.getAction();
        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d("MotoSimUiHelper", "Receive BOOT_COMPLETED triggering NETWORK STATE CHANGE");
            Intent appIntent = new Intent("com.motorola.motosimuihelper.SIM_SHOW_INTENT");
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            paramContext.startActivity(appIntent);
        }
        else
        if (action.equals("android.intent.action.RADIO_TECHNOLOGY")) {
            Log.d("MotoSimUiHelper", "Receive RADIO_TECHNOLOGY triggering NETWORK STATE CHANGE");
            Intent appIntent = new Intent("com.motorola.motosimuihelper.SIM_SHOW_INTENT");
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            paramContext.startActivity(appIntent);
        }
        else {
            String iccState = extraBundle.getString(IccCard.INTENT_KEY_ICC_STATE);
            if ((iccState != null) && (iccState.equals(IccCard.INTENT_VALUE_ICC_LOADED))) {
                Log.d("MotoSimUiHelper", "Receive sim loaded, start to check sim status");
                Intent appIntent = new Intent("com.motorola.motosimuihelper.SIM_SHOW_INTENT");
                appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                paramContext.startActivity(appIntent);
            }
        }
    }

}

