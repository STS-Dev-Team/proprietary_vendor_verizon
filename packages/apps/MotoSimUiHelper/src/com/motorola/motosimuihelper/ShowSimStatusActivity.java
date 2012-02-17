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
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardApplication;
import com.android.internal.telephony.IccCardApplication.AppType;
import com.android.internal.telephony.IccIoResult;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import android.provider.Settings;
import java.lang.reflect.Field;

public class ShowSimStatusActivity extends Activity
{
  private CommandsInterface mCM = null;
  private Context mContext = null;
  private Handler mHandler = new Handler()
  {
    public void handleMessage(Message paramMessage)
    {
      switch (paramMessage.what)
      {
      case 1:
        Log.d("MotoSimUiHelper", "Handle EVENT_READ_RECORD_DONE Message");
        AsyncResult localAsyncResult = (AsyncResult)paramMessage.obj;
        IccIoResult ioResult = (IccIoResult)localAsyncResult.result;
        if (localAsyncResult.exception == null)
        {
          if (ioResult.getException() == null)
          {
            if ((0x40 & ioResult.payload[3]) == 0)
            {
              Log.d("MotoSimUiHelper", "EUTRAN is not avaliable");
              ShowSimStatusActivity.this.showDialog(ShowSimStatusActivity.this.mContext, 0);
            }
            else
            {
              Log.d("MotoSimUiHelper", "EUTRAN is avaliable");
              String lineNum = ShowSimStatusActivity.this.mPhone.getLine1Number();
              if ((lineNum != null) && (!lineNum.startsWith("00000")))
              {
                Log.d("MotoSimUiHelper", "SIM is a valid activated Verizon 4G SIM");
                ShowSimStatusActivity.this.updateNetworkMode();
              }
              ShowSimStatusActivity.this.finish();
            }
          }
          else
          {
            Log.e("MotoSimUiHelper", "EFHPLMNWACT not accessible.");
            ShowSimStatusActivity.this.showDialog(ShowSimStatusActivity.this.mContext, 0);
          }
        }
        else
        {
          Log.e("MotoSimUiHelper", "read icc i/o exception");
          ShowSimStatusActivity.this.finish();
        }
      }
    }
  };
  private Phone mPhone = null;

  private int checkSimStatus()
  {
    TelephonyManager tm = (TelephonyManager)this.mPhone.getContext().getSystemService("phone");
    Log.d("MotoSimUiHelper", "SIM operator " + tm.getSimOperator());
    String simOperator = tm.getSimOperator();
    int i;
    if (simOperator != null)
    {
      if ((simOperator.equals("311480")) || (simOperator.equals("20404")))
      {
        if (!this.mPhone.needsOtaServiceProvisioning())
        {
          if (this.mPhone.getIccCard().isApplicationOnIcc(IccCardApplication.AppType.APPTYPE_USIM))
          {
            this.mCM.iccIO(176, 28514, "3F007F20", 0, 0, 5, null, null, this.mHandler.obtainMessage(1));
            i = 3;
          }
          else
          {
            Log.d("MotoSimUiHelper", "No usim application on ICC card");
            i = 0;
          }
        }
        else
        {
          Log.d("MotoSimUiHelper", "The icc card needs to be provisioned");
          i = 2;
        }
      }
      else
      {
        Log.d("MotoSimUiHelper", "Unkown SIM operator");
        i = 1;
      }
    }
    else
      i = 2;
    return i;
  }

  private void showDialog(Context paramContext, int paramInt)
  {
    AlertDialog localAlertDialog;
    switch (paramInt)
    {
    case 0:
      localAlertDialog = new AlertDialog.Builder(paramContext).setIconAttribute(android.R.attr.alertDialogIcon).setTitle(R.string.wrong_sim_title).setMessage(R.string.wrong_sim).setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
      {
        public void onClick(DialogInterface paramDialogInterface, int paramInt)
        {
          ShowSimStatusActivity.this.finish();
        }
      }).create();
      updateNotification(0);
      break;
    case 1:
      localAlertDialog = new AlertDialog.Builder(paramContext).setIconAttribute(android.R.attr.alertDialogIcon).setTitle(R.string.wrong_operator_title).setMessage(R.string.wrong_operator).setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
      {
        public void onClick(DialogInterface paramDialogInterface, int paramInt)
        {
          ShowSimStatusActivity.this.finish();
        }
      }).create();
      updateNotification(1);
      localAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
      if (!paramContext.getResources().getBoolean(17891331))
        localAlertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAGS_CHANGED);
      localAlertDialog.show();
    }
  }

  private void updateNetworkMode()
  {
    Log.d("MotoSimUiHelper", "updateNetworkMode");
    PackageManager localPackageManager = getPackageManager();
    ComponentName localComponentName = new ComponentName("com.motorola.motosimuihelper", "com.motorola.motosimuihelper.UpdateNetworkModeActivity");
    int j = localPackageManager.getComponentEnabledSetting(localComponentName);
    int i = Settings.Secure.getInt(this.mContext.getContentResolver(), "preferred_network_mode", 0);
    Log.d("MotoSimUiHelper", "Current Preferred network mode is " + i);
    if (i == 7)
    {
      Log.d("MotoSimUiHelper", "The preferred network mode is GLOBAL, disable the updateNetworkMode component");
      localPackageManager.setComponentEnabledSetting(localComponentName, 2, 1);
    }
    if ((i != 7) && (j != 2))
    {
      Intent localIntent = new Intent("com.motorola.motosimuihelper.UPDATE_NETWORK_MODE");
      localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
      Log.d("MotoSimUiHelper", "start UpdateNetworkModeActivity");
      startActivity(localIntent);
    }
  }

  public void onConfigurationChanged(Configuration paramConfiguration)
  {
    Log.d("MotoSimUiHelper", "onConfigurationChanged");
    super.onConfigurationChanged(paramConfiguration);
  }

  protected void onCreate(Bundle paramBundle)
  {
    int i;
    super.onCreate(paramBundle);
    requestWindowFeature(1);
    this.mContext = this;
    this.mPhone = PhoneFactory.getDefaultPhone();
    PhoneProxy localPhoneProxy = (PhoneProxy)this.mPhone;
    if (localPhoneProxy != null);
    try
    {
      Field localField = PhoneProxy.class.getDeclaredField("mCommandsInterface");
      if (localField != null)
        localField.setAccessible(true);
      try
      {
        this.mCM = ((CommandsInterface)localField.get(localPhoneProxy));
        i = checkSimStatus();
        if (1 == i)
        {
          Log.d("MotoSimUiHelper", "SIM card is from an unknown operator, show the wrong operator screen");
          showDialog(this.mContext, 1);
          return;
        }
      }
      catch (IllegalAccessException localIllegalAccessException)
      {
          Log.e("MotoSimUiHelper", "Cannot acess CommandsInterface");
          finish();
          return;
      }
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
        Log.e("MotoSimUiHelper", "No CommandsInterface found");
        finish();
        return;
    }
    if (i == 0)
    {
      Log.d("MotoSimUiHelper", "SIM card is a RUIM card");
      showDialog(this.mContext, 0);
      return;
    }
    else {
      if (2 != i) return;
      Log.d("MotoSimUiHelper", "This status should be ignore, don't show any screen, exit here");
      finish();
    }
  }


  public void updateNotification(int paramInt)
  {
    NotificationManager localNotificationManager = (NotificationManager)getSystemService("notification");
    if (localNotificationManager != null)
    {
      Notification localNotification;
      switch (paramInt)
      {
      default:
        break;
      case 0:
        localNotification = new Notification(R.drawable.unknownsim, getString(R.string.wrong_sim_notice), System.currentTimeMillis());
        localNotification.flags = 2;
        localNotification.setLatestEventInfo(this.mContext, getString(R.string.wrong_sim_notice), null, null);
        localNotificationManager.notify(0, localNotification);
        break;
      case 1:
        localNotification = new Notification(R.drawable.unknownsim, getString(R.string.wrong_operator_notice), System.currentTimeMillis());
        localNotification.flags = 2;
        localNotification.setLatestEventInfo(this.mContext, getString(R.string.wrong_operator_notice), null, null);
        localNotificationManager.notify(0, localNotification);
      }
    }
  }
}

/* Location:           /home/dhacker29/android-utility/working-folder/mod-here-solo/classes_dex2jar.jar
 * Qualified Name:     com.motorola.motosimuihelper.ShowSimStatusActivity
 * JD-Core Version:    0.6.0
 */
