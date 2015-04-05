package com.example.voicerecognizersp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;



	public class TelephonyBroadcastReceiver extends BroadcastReceiver {
	
	private static final String TAG = TelephonyBroadcastReceiver.class.getSimpleName();
	public static final String SMS_CONTENT = "sms_content";
    public static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
			if(intent.getAction().equals(ACTION)) {
			    Log.i(TAG, "Intent recieved: " + intent.getAction());
			    Log.i(TAG, "SMS received");
			
			    
			    Toast.makeText(context, "SMS RECEIVED:", Toast.LENGTH_LONG).show();
			}
			else
			    Log.i(TAG, "SMS received but different action");

		}
	}