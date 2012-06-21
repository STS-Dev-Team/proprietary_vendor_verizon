package com.motorola.motosimuihelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.internal.telephony.IccCard;
import android.content.pm.PackageManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.RILConstants;
import android.provider.Settings;
import android.telephony.ServiceState;

public class PhoneBroadcastReceiver extends BroadcastReceiver {
    private static String ACTION_SIMUI_SERVICE = "com.motorola.motosimuihelper.START_SERVICE";

    public void onReceive(Context paramContext, Intent paramIntent) {
        paramContext.startService(new Intent(ACTION_SIMUI_SERVICE));
    }

}

