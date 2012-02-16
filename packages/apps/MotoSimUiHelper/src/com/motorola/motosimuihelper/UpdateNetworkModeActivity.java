package com.motorola.motosimuihelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class UpdateNetworkModeActivity extends Activity
{
  private Context mContext = null;
  private Handler mHandler = new Handler()
  {
    public void handleMessage(Message paramMessage)
    {
      switch (paramMessage.what)
      {
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

  public void onConfigurationChanged(Configuration paramConfiguration)
  {
    Log.d("MotoSimUiHelper", "onConfigurationChanged");
    super.onConfigurationChanged(paramConfiguration);
  }

  protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    this.mContext = this;
    this.mPhone = PhoneFactory.getDefaultPhone();
    PackageManager localPackageManager = getPackageManager();
    ComponentName localComponentName = new ComponentName("com.motorola.motosimuihelper", "com.motorola.motosimuihelper.UpdateNetworkModeActivity");
    if (localPackageManager.getComponentEnabledSetting(localComponentName) != 2)
    {
      AlertDialog localAlertDialog = new AlertDialog.Builder(this.mContext).setIconAttribute(16843605).setTitle(2130903046).setMessage(2130903047).setCancelable(false).setPositiveButton(2130903049, new DialogInterface.OnClickListener()
      {
        public void onClick(DialogInterface paramDialogInterface, int paramInt)
        {
          Settings.Secure.putInt(UpdateNetworkModeActivity.this.mPhone.getContext().getContentResolver(), "preferred_network_mode", 7);
          UpdateNetworkModeActivity.this.mPhone.setPreferredNetworkType(7, UpdateNetworkModeActivity.this.mHandler.obtainMessage(1));
        }
      }).setNegativeButton(2130903050, new DialogInterface.OnClickListener()
      {
        public void onClick(DialogInterface paramDialogInterface, int paramInt)
        {
          UpdateNetworkModeActivity.this.finish();
        }
      }).create();
      localAlertDialog.getWindow().setType(2003);
      if (!this.mContext.getResources().getBoolean(17891331))
        localAlertDialog.getWindow().addFlags(4);
      localAlertDialog.show();
      localPackageManager.setComponentEnabledSetting(localComponentName, 2, 1);
    }
  }
}

/* Location:           /home/dhacker29/android-utility/working-folder/mod-here-solo/classes_dex2jar.jar
 * Qualified Name:     com.motorola.motosimuihelper.UpdateNetworkModeActivity
 * JD-Core Version:    0.6.0
 */