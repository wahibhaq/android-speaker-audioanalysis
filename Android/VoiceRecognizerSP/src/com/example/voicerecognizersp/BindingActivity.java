package com.example.voicerecognizersp;


import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voicerecognizersp.MfccService.LocalBinder;

public class BindingActivity extends Activity {
	
	 MfccService mService;
	
	 boolean isBound = false;
	    
	 Button btnStartRecording, btnStopRecording;
	 TextView txtMessage;
	 final static String TAG = "BindingActivity";

	 BroadcastReceiver receiver;
	
	 FileOperations fileOprObj;
	 MonitoringData monitorOprObj;
	 
	 
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_main);

	        fileOprObj = new FileOperations(this);
	        monitorOprObj = new MonitoringData(this, fileOprObj);

	    	btnStartRecording = (Button)findViewById(R.id.buttonStartRec);
			btnStopRecording = (Button)findViewById(R.id.buttonStopRec);
			txtMessage = (TextView) findViewById(R.id.txtMessage);
						
			btnStartRecording.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {

					if(fileOprObj.isAllDirsExist())
					{
					
						if(mService != null) {
							
							// TODO Auto-generated method stub
							if (!mService.isRecording()) {
								
								performStartRecording();
						        
								txtMessage.setText("Recording in progress ...");
								btnStartRecording.setEnabled(false);
																
					        }
							else {
								showToast("Audio process already running !");
							}
								
						}
						else {//for testing only without binder
							
							showToast("Can't connect with mService !");
							
						}
							
						
					}
					else
					{
						Toast.makeText(getApplicationContext(), "All Dirs Not Exist .. recreating !", Toast.LENGTH_LONG).show();
						fileOprObj.recreateDirsIfExist();
					}
					
				} 

	    	});
			
			btnStopRecording.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					   
			         performStopRecording();
			         
					 btnStartRecording.setEnabled(true);
		            
				}

	    	});

			receiver = new BroadcastReceiver() {
		        @Override
		        public void onReceive(Context context, Intent intent) {
		        	
		            String msgFromService = intent.getStringExtra(MfccService.COPA_MESSAGE);
		            // do something here.
		            
		            txtMessage.setText(msgFromService);
		        }
		    };
		    
		    
		    //initiating service
			Intent intentService = new Intent(this, MfccService.class);
			startService(intentService);
			
			//to reset UI every time activity comes active - mainly to show if recording is running or not
            resetUI();
			
			
	 }//onCreate ends here
 
    
	 @Override
	 protected void onStart() {
		 super.onStart();
		 Log.i(TAG, "BindingActivity - onStart - binding ..");
		 
		 //bind to the service
		 doBindToService();

         
	    	
	 }
	 
	 @Override
	 protected void onStop() {
		 super.onStop();
		 Log.i(TAG, "BindingActivity - onStop - unbinding ..");
		 
		 //bind to the service
		 doUnbindService();
	 }
	 

    @Override
    public void onBackPressed() {
    	/* we customize the back button so that the activity pauses instead of finishing */
    	moveTaskToBack(true);
    }
    
    @Override
    public void onResume() {
		super.onResume();
    	//Log.i(TAG, "onResume");
    }
    
    @Override
    public void onDestroy() {
    	
    	super.onDestroy();
    	Log.i(TAG, "BindingActivity  - onDestroy called");
    	    
    }
   
	 
 	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
        	Log.i(TAG, "onServiceConnected");
        	
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            isBound = true;
            
            showToast("Connected");
    
            //to reset UI every time activity comes active - mainly to show if recording is running or not
            resetUI();
         
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        	Log.i(TAG, "onServiceDisConnected");

            isBound = false;
            showToast("Disconnected");
            
            mService = null;
            


        }
    };
    
    private void resetUI() {
    	
    	
     	
		if(mService != null && isBound) {
			//connection is already done
			
			if(mService.isRecording()) {
				
				txtMessage.setText("Recording in progress ...");
				btnStartRecording.setEnabled(false);
	    	}
	    	else
	    	{
	    		setFFTtype();
				btnStartRecording.setEnabled(true);
	    	}
		}
		else
		{
			setFFTtype();
		}
    }
    
    private void doUnbindService() {
    	//showToast("Unbidning ..");
    	unbindService(mConnection);
    	isBound = false;
        
    	//optimization
    	mService = null;
        mConnection = null;

    }
    
    private void doBindToService() {
    	//showToast("Binding ..");
    	if(!isBound) {
    		Intent bindIntent = new Intent(this, MfccService.class);
    		isBound = bindService(bindIntent, mConnection, Context.BIND_AUTO_CREATE);//Context.BIND_ABOVE_CLIENT);//
    	}
    		
    }
    
    
    
    public void showToast(final String text) {
		
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
		
	}
    
   
    
    
    ////////////////////////MFCC Operations//////
    
    private void performStopRecording() {
    	
        if(!isBound)
        {
			Log.i(TAG, "not bound - everything definitely stopped now !");
			txtMessage.setText("not bound - everything definitely stopped now !");
        	
    		//Toast.makeText(getApplicationContext(), "Recording is definitey stopped now !!", Toast.LENGTH_SHORT).show();

        }
        else
        {
        	if(mService != null) {
        		
		        //stop MFCC tasks running in background service
    			if(mService.isRecording()) {
		
		        	mService.stopRecording();
		        	
		        	txtMessage.setText("Recording stopped !");
		        	monitorBatteryUsage("End  ");
			    	monitorCpuAndMemoryUsage("End  ");
			    	
			    	monitorOprObj.dumpMeanAndSdForCpu();
					
				    
				    //stopping service
				   // Intent intent = new Intent(this, MfccService.class);
				   // stopService(intent);
		
		        }
        	}
        }
    }
    
    private void performStartRecording() {
    	
    	fileOprObj.resetDirs();//recreate before dumping fresh data
    	
		monitorBatteryUsage("Start");
		monitorCpuAndMemoryUsage("Start");

		mService.startMfccExtraction();
	
    }
	    
	 
    private void setFFTtype()
    {
    	if(SharedData.fftType.equals("FFT_CT"))
		{
    		SharedData.SD_FOLDER_PATH = "/Thesis/VoiceRecognizer";
			txtMessage.setText("MFCC with Cooley-Tukey");
		}
		else if(SharedData.fftType.equals("FFT_SP"))
		{
			SharedData.SD_FOLDER_PATH = "/Thesis/VoiceRecognizerSP";
			txtMessage.setText("MFCC with Superpowered");
		}
    }
    
	
	


	/**
	 * Takes the final compiled list of cepstral features and stores them in a csv file on the sdcard.
	 * Each frame is represented by its MFCCs (without energy). 
	 * 
	 * @param arrayList
	 */
	public void audioFeatures2csv(ArrayList<LinkedList<ArrayList<double[]>>> arrayList) {
		
		

		PrintWriter csvWriter;
		try
		{
		    
			
			//storage/emulated/0/Thesis/VoiceRecognizerSP/CSV/20MfccFeatures_1.csv
			File csvFile = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_CSV + File.separator + SharedData.csvFileName);
			
			//if(!file.exists())
			//	file = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_CSV + File.separator + SharedData.csvFileName);
			
			csvWriter = new  PrintWriter(new FileWriter(csvFile,true));
			
			for(LinkedList<ArrayList<double[]>> linkedList : arrayList) {
				for (ArrayList<double[]> window : linkedList) {
					for (double[] newline : window) {
						for (double element : newline) {
							csvWriter.print(element + ",");
						}
						csvWriter.print("\r\n");
					}
				}
			}

			csvWriter.close();
			
			
			
          // code runs in a thread
          runOnUiThread(new Runnable() {
        	@Override
          	public void run() {
				Toast.makeText(getApplicationContext(), "Features stored in csv file !", Toast.LENGTH_SHORT).show();

        	  	txtMessage.setText("Features successfully stored in csv file !");
          	}
          });
          
			
	    	Log.i(TAG, "MFCC audio2csv() done");
	    	
	    	arrayList.clear();
	    	arrayList = null;
	    	
	    	//mService.releaseLists();
	    	

	    	

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	
	
	/**
	 * Function to monitor CPU usage and Memory allocation solely due to this application.
	 * Its called at start and end of experiment
	 * 
	 * @param state : 3 types of states right now. AddExperiement/Start/End which reflects state of experiment.
	 * Add Experiment just append a new line with title so that its easir to read txt file
	 */
	private void monitorCpuAndMemoryUsage(String state)
	{
		String line = "";
		
		if(state.equals("AddExperiment"))
		{
			line = "\n\n\n____New Experiment____" + SharedData.fftType;
			fileOprObj.appendToMemCpuFile( line );
			
		}
		else
		{
			//In case of Start and End
			
			line = "CPU Usage % : " + monitorOprObj.getCpuUsage() + " & " + "Memory (Private Dirty in KBs) : " + monitorOprObj.getMemoryUsage();
			fileOprObj.appendToMemCpuFile( state + " --- " + line + " --- " + getFormattedTimeStamp() );
		}
		
		

	}
	
	/**
	 * Function to monitor Battery usage due to this application. Its called at start and end of experiment
	 *
	 * @param state : 3 types of states right now. AddExperiement/Start/End which reflects state of experiment.
	 * Add Experiment just append a new line with title so that its easir to read txt file
	 */
	//http://androidtrainningcenter.blogspot.de/2013/09/android-battery-management-api-getting.html
	//http://virtuallyhyper.com/2012/12/find-out-battery-status-of-rooted-andoid-phone-using-adb/
	private void monitorBatteryUsage(String state)
	{
		String line = "";
		
		if(state.equals("AddExperiment"))
		{
			line = "\n\n\n____New Experiment____" + SharedData.fftType;
			fileOprObj.appendToBatteryFile( line );

		}
		else
		{
		  IntentFilter mIntentFilter = new IntentFilter();  
          mIntentFilter.addAction(Intent.ACTION_BATTERY_LOW);  
          mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);  
          mIntentFilter.addAction(Intent.ACTION_BATTERY_OKAY);  
          Intent batteryIntent = registerReceiver(null, mIntentFilter);  
          double batteryLevel = getBatteryLevel(batteryIntent);  
          Log.i(TAG, "MFCC Battery Level : " + String.valueOf(batteryLevel));  
          
          fileOprObj.appendToBatteryFile(state + " --- " + String.valueOf(batteryLevel) + " --- " + getFormattedTimeStamp());
          
            

		}
          
	}
	
	 public double getBatteryLevel(Intent batteryIntent) {  
         int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);  
         int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);  
         if (level == -1 || scale == -1) {  
              return 50.0f;  
         }  
         return ((double) level / (double) scale) * 100.0f;  
    }  
	 
	 private String getFormattedTimeStamp() {

		  	Long tsLong = System.currentTimeMillis();

		    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
		    cal.setTimeInMillis(tsLong);
		    String date = DateFormat.format("dd-MM-yyyy_HH:mm:ss", cal).toString();
		    return date;
		}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	@Override
	  public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // action with ID action_addexperiment was selected
	    case R.id.action_addexperiment:
	    	monitorBatteryUsage("AddExperiment");
	    	monitorCpuAndMemoryUsage("AddExperiment");
	      Toast.makeText(this, "Add Experiment selected", Toast.LENGTH_SHORT).show();

	      break;
	    // action with ID action_resetexperiment was selected
	    case R.id.action_resetexperiment:
	    	
	      fileOprObj.resetFiles();
	      Toast.makeText(this, "Reset Experiment selected", Toast.LENGTH_SHORT).show();
	      break;
	      
	    case R.id.action_switchfft:
	    	
			if (mService.isRecording())
	    		 Toast.makeText(this, "Switch not possible because recording is running !", Toast.LENGTH_SHORT).show();
	    	 else
	    	 {
	    		 if(SharedData.fftType.equals("FFT_CT"))
	    		 {
	    			 //Swith to SP
	    			 
	    			 SharedData.fftType = "FFT_SP";
	    			 mService.setFFTType(SharedData.fftType);
	    			 txtMessage.setText("MFCC with Superpowered");
	    			 
	    			 SharedData.SD_FOLDER_PATH = "/Thesis/VoiceRecognizerSP";
	    			 SharedData. SD_FOLDER_PATH_CSV = SharedData.SD_FOLDER_PATH + "/CSV"; 

	    			 fileOprObj.resetDirs();

	    			 showToast("FFT Changed");

	    			 monitorBatteryUsage("AddExperiment");
	    			 monitorCpuAndMemoryUsage("AddExperiment");

	    		 }
	    		 else if(SharedData.fftType.equals("FFT_SP"))
	    		 {
	    			 //Switch to CT
	
	    			 SharedData.fftType = "FFT_CT";
	    			 mService.setFFTType(SharedData.fftType);
	    			 txtMessage.setText("MFCC with Cooley-Tukey");
	    			 
	    			 SharedData.SD_FOLDER_PATH = "/Thesis/VoiceRecognizer";
	    			 SharedData.SD_FOLDER_PATH_CSV = SharedData.SD_FOLDER_PATH + "/CSV";
	    			 
	    			 fileOprObj.resetDirs();

	    			 showToast("FFT Changed");
	    			 
	    			 monitorBatteryUsage("AddExperiment");
	    			 monitorCpuAndMemoryUsage("AddExperiment");

	    		 }
	    		 

	    	 }
		      break;
		 
	    default:
	      break;
	    }

	    return true;
	  } 


}//class ends here
