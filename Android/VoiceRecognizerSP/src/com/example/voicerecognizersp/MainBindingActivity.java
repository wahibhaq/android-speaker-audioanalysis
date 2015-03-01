package com.example.voicerecognizersp;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voicerecognizersp.RecordingMfccService.LocalBinder;


public class MainBindingActivity extends Activity {
	
	 RecordingMfccService mService;
	 boolean isBound = false;
	 Intent intentBindService;
	    
	 Button btnStartRecording, btnStopRecording;
	 TextView txtMessage;
	 final static String TAG = "MFCCBindingActivity";
     private static final String appProcessName = "com.example.voicerecognizersp";

	 BroadcastReceiver receiver;
	 
     public static String fftType = "FFT_CT"; //FFT_CT : Cooley-Tukey and other option is FFT_SP : Superpowered

	 
	 //////File Operations related//////////
	 public static final File SD_PATH = Environment.getExternalStorageDirectory();
	 public static String SD_FOLDER_PATH = "/Thesis/VoiceRecognizer"; //"/Thesis/VoiceRecognizerSP";
	 public static String SD_FOLDER_PATH_CSV = SD_FOLDER_PATH + "/CSV"; 

	 final String csvFileName = "voicerecognizer_mfcc.csv"; //"20MfccFeatures_";
	 
	 FileOperations fileOprObj;
	 static volatile FileOperations instance = null;
	 

		/**
		 * singleton method to ensure FileOperations instance remains unique so that constructor is not called more than once
		 * 
		 * @param mainBindingActivity
		 * @return
		 */
		//http://howtodoinjava.com/2012/10/22/singleton-design-pattern-in-java/
		public static FileOperations getInstance(MainBindingActivity mainBindingActivity) {
	        if (instance == null) {
	            synchronized (FileOperations.class) {
	                instance = new FileOperations(mainBindingActivity);
	            }
	        }
	        return instance;
	    }
		
		
	//////////////////////////////
	 
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_main);
	        
	        fileOprObj = getInstance(this);
	        
	    	btnStartRecording = (Button)findViewById(R.id.buttonStartRec);
			btnStopRecording = (Button)findViewById(R.id.buttonStopRec);
			txtMessage = (TextView) findViewById(R.id.txtMessage);
			
			if(fftType.equals("FFT_CT"))
			{
				SD_FOLDER_PATH = "/Thesis/VoiceRecognizer";
				txtMessage.setText("MFCC with Cooley-Tukey");
			}
			else if(fftType.equals("FFT_SP"))
			{
				SD_FOLDER_PATH = "/Thesis/VoiceRecognizerSP";
				txtMessage.setText("MFCC with Superpowered");
			}
			
			btnStartRecording.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					
					
					// TODO Auto-generated method stub
					if (mService.isDispatcherNull() == true) {
						
						
						// Call a method from the LocalService.
			            // However, if this call were something that might hang, then this request should
			            // occur in a separate thread to avoid slowing down the activity performance.
						
						
						mService.initDispatcher();

						monitorBatteryUsage("Start");
		        		monitorCpuAndMemoryUsage("Start");

						mService.startMfccExtraction();
					
						txtMessage.setText("Recording in progress ...");

						
						
			        }
					else
					{
						// Bind to LocalService. Also to rebind
				        intentBindService = new Intent(getApplicationContext(), RecordingMfccService.class);
				        bindService(intentBindService, mConnection, Context.BIND_AUTO_CREATE);
				        
					}
				
					
				}

	    	});
			
			btnStopRecording.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
			        // Unbind from the service
			        if (isBound ) {
			            unbindService(mConnection);
			            isBound = false;
						Log.i(TAG, "onStopRecording Service unbinded & isbound : " + isBound);
						
					    LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);

			            
			        }
			        else
			        {
						Log.i(TAG, "onStopRecording Service unbinded & isbound : " + isBound);

			        }
			        
			        
			        
			        //stop MFCC tasks
		            if(mService.isDispatcherNull() == false)
		            {
						Log.i(TAG, "onStopRecording isDispatcher : " + mService.isDispatcherNull());

		            	mService.stopDispatcher();
		            	
		            	txtMessage.setText("Recording stopped !");
		            	monitorBatteryUsage("End  ");
				    	monitorCpuAndMemoryUsage("End  ");
				    	
						
						
						// storing to csv done on a separate thread
					    Runnable runnable = new Runnable() {
					      @Override
					      public void run() {
					           
								audioFeatures2csv(mService.getMfccList());
								
					        }
					      
					    };
					    
					    new Thread(runnable).start();
					    

		            }
		            else
						Log.i(TAG, "onStopRecording isDispatcher : " + mService.isDispatcherNull());

		           		        
			        

		            
				}

	    	});
			
			
			receiver = new BroadcastReceiver() {
		        @Override
		        public void onReceive(Context context, Intent intent) {
		        	
		            String msgFromService = intent.getStringExtra(RecordingMfccService.COPA_MESSAGE);
		            // do something here.
		            
		            txtMessage.setText(msgFromService);
		        }
		    };
			
	}
	 
	 

	    @Override
	    public void onBackPressed() {
	    	/* we customize the back button so that the activity pauses instead of finishing */
	    	moveTaskToBack(true);
	    }
	   
	    
	    @Override
	    public void onPause() {
	    	super.onPause();
	    	
	    	Log.i(TAG, "onPause");

	    }
	    
	    
	    
	    @Override
	    public void onResume() {
			super.onResume();
	    	Log.i(TAG, "onResume");

	    	// Bind to LocalService. Also in onResume so that it rebinds after coming back from homepressed
	        intentBindService = new Intent(this, RecordingMfccService.class);
	        bindService(intentBindService, mConnection, Context.BIND_AUTO_CREATE);
	        
	        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter(RecordingMfccService.COPA_RESULT));
	        
	    

	        
	    }
	    

		public void showToast(final String text) {
			runOnUiThread(new Runnable() 
			{                
				@Override
				public void run() 
				{
					Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
				}
			});
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
	            
	            //txtMessage.setText("Connected");
	            showToast("Conneted");
	            
	            
	            
	        }

	        @Override
	        public void onServiceDisconnected(ComponentName arg0) {
	        	Log.i(TAG, "onServiceDisConnected");

	            isBound = false;
	            //txtMessage.setText("Disconnected");
	            showToast("Disconneted");


	        }
	    };
	    
	    
	    
	 

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
				File file = new File(SD_PATH + SD_FOLDER_PATH_CSV + File.separator + csvFileName);
				
				if(!file.exists())
					file = new File(SD_PATH + SD_FOLDER_PATH_CSV + File.separator + csvFileName);
				
				csvWriter = new  PrintWriter(new FileWriter(file,true));
				
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
		    	

		    	

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		
		
		/**
		 * Function computes "Total Private Dirty Memory" consumed by this application only.
		 * @return
		 */
		private String getMemoryUsage()
		{
			Context context = getApplicationContext();
			ActivityManager mgr = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
			String totalPrivateDirtyMemory = "";
			
			for(RunningAppProcessInfo processInfo : mgr.getRunningAppProcesses())
			{
				
		        if(processInfo.processName.equals(appProcessName) ){
		        	//Log.i(TAG, " MFCC pid: "+processInfo.pid);                    
				    int[] pids = new int[1];
				    pids[0] = processInfo.pid;
				    android.os.Debug.MemoryInfo[] MI = mgr.getProcessMemoryInfo(pids);
		     	    Log.i(TAG,"MFCC total private dirty memory (KB): " + MI[0].getTotalPrivateDirty());
		     	    
		     	    
		     	    totalPrivateDirtyMemory = String.valueOf(MI[0].getTotalPrivateDirty());
				    
				    break;
		        }
		    }
			
			return totalPrivateDirtyMemory;
		
			    

		  /*
			    Log.e("memory","     dalvik private: " + MI[0].dalvikPrivateDirty);
			    Log.e("memory","     dalvik shared: " + MI[0].dalvikSharedDirty);
			    Log.e("memory","     dalvik pss: " + MI[0].dalvikPss);            
			    Log.e("memory","     native private: " + MI[0].nativePrivateDirty);
			    Log.e("memory","     native shared: " + MI[0].nativeSharedDirty);
			    Log.e("memory","     native pss: " + MI[0].nativePss);            
			    Log.e("memory","     other private: " + MI[0].otherPrivateDirty);
			    Log.e("memory","     other shared: " + MI[0].otherSharedDirty);
			    Log.e("memory","     other pss: " + MI[0].otherPss);

			    Log.e("memory","     total private dirty memory (KB): " + MI[0].getTotalPrivateDirty());
			    Log.e("memory","     total shared (KB): " + MI[0].getTotalSharedDirty());
			    Log.e("memory","     total pss: " + MI[0].getTotalPss());            
			
			*/
		    
		    
		}
		
		/**
		 * uses linux "top" command to fetch cpu percentage value which shows how much this app is
		 * contributing to total cpu usage
		 * 
		 * @return
		 */
		//http://m2catalyst.com/tutorial-finding-cpu-usage-for-individual-android-apps/ 
		private String getCpuUsage()
		{
			ArrayList<String> list = new ArrayList<String>();
			String cpuUsageValue = "";

			Process p = null;
			try {
				
				p = Runtime.getRuntime().exec(new String[] { "sh", "-c", "top -n 1 | grep " + appProcessName });
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			int i =0;
			String line = "";
			try {
				
				line = reader.readLine();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			//while(line != null)
			if(line != null)
			{
				//Log.i(TAG, "MFCC output of top command : " + i + " : " + line);
				//11203  0   0% S    13 901560K  43528K  fg u0_a141  tum.laser.voicerecognizer

				String lineOuput[] = line.split(" ");
				cpuUsageValue = lineOuput[5].trim();
				//[19472, , 0, , , 0%, S, , , , 15, 904664K, , 44148K, , fg, u0_a141, , tum.laser.voicerecognizer]
				
				Log.i(TAG, "MFCC cpu usage value : " + cpuUsageValue);

				list.add(line);
				try {
					line = reader.readLine();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				i++;
			}
			else
			{
				Log.i(TAG, "MFCC command line is null");

			}
			
			return cpuUsageValue;
			
			
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
				line = "\n\n\n____New Experiment____" + fftType;
				fileOprObj.appendToMemCpuFile( line );
				
			}
			else
			{
				//In case of Start and End
				
				line = "CPU Usage % : " + getCpuUsage() + " & " + "Memory (Private Dirty in KBs) : " + getMemoryUsage();
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
				line = "\n\n\n____New Experiment____" + fftType;
				fileOprObj.appendToBatteryFile( line );

			}
			else
			{
			  IntentFilter mIntentFilter = new IntentFilter();  
	          mIntentFilter.addAction(Intent.ACTION_BATTERY_LOW);  
	          mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);  
	          mIntentFilter.addAction(Intent.ACTION_BATTERY_OKAY);  
	          Intent batteryIntent = registerReceiver(null, mIntentFilter);  
	          float batteryLevel = getBatteryLevel(batteryIntent);  
	          Log.i(TAG, "MFCC Battery Level : " + String.valueOf(batteryLevel));  
	          
	          fileOprObj.appendToBatteryFile(state + " --- " + String.valueOf(batteryLevel) + " --- " + getFormattedTimeStamp());
			}
	          
		}
		
		 public float getBatteryLevel(Intent batteryIntent) {  
	         int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);  
	         int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);  
	         if (level == -1 || scale == -1) {  
	              return 50.0f;  
	         }  
	         return ((float) level / (float) scale) * 100.0f;  
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
		    	 if(mService.isRecording() )
		    		 Toast.makeText(this, "Switch not possible because recording is running !", Toast.LENGTH_SHORT).show();
		    	 else
		    	 {
		    		 if(fftType.equals("FFT_CT"))
		    		 {
		    			 //Swith to SP
		    			 
		    			 fftType = "FFT_SP";
		    			 mService.setFFTType(fftType);
		    			 txtMessage.setText("MFCC with Superpowered");
		    			 
		    			 SD_FOLDER_PATH = "/Thesis/VoiceRecognizerSP";
		    			 SD_FOLDER_PATH_CSV = SD_FOLDER_PATH + "/CSV"; 

		    			 fileOprObj.resolveDirs();

		    			 showToast("FFT Changed");

		    			 monitorBatteryUsage("AddExperiment");
		    			 monitorCpuAndMemoryUsage("AddExperiment");

		    		 }
		    		 else if(fftType.equals("FFT_SP"))
		    		 {
		    			 //Switch to CT
		
		    			 fftType = "FFT_CT";
		    			 mService.setFFTType(fftType);
		    			 txtMessage.setText("MFCC with Cooley-Tukey");
		    			 
		    			 SD_FOLDER_PATH = "/Thesis/VoiceRecognizer";
		    			 SD_FOLDER_PATH_CSV = SD_FOLDER_PATH + "/CSV";
		    			 
		    			 fileOprObj.resolveDirs();

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

		public String getFFTType()
		{
			return fftType;
		}
	
}
