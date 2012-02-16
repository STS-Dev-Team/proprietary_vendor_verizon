package com.motorola.motosimuihelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class PhoneBroadcastReceiver extends BroadcastReceiver
{
  public void onReceive(Context paramContext, Intent paramIntent)
  {
    Object localObject = paramIntent.getExtras();
    paramIntent.getAction();
    localObject = ((Bundle)localObject).getString("ss");
    if ((localObject != null) && (((String)localObject).equals("LOADED")))
    {
      Log.d("MotoSimUiHelper", "Receive sim loaded, start to check sim status");
      localObject = new Intent("com.motorola.motosimuihelper.SIM_SHOW_INTENT");
      ((Intent)localObject).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
      paramContext.startActivity((Intent)localObject);
    }
  }
}

/* Location:           /home/dhacker29/android-utility/working-folder/mod-here-solo/classes_dex2jar.jar
 * Qualified Name:     com.motorola.motosimuihelper.PhoneBroadcastReceiver
 * JD-Core Version:    0.6.0
 */
