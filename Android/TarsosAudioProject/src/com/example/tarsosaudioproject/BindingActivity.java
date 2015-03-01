package com.example.tarsosaudioproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tarsosaudioproject.RecordingMfccService.LocalBinder;





public class BindingActivity extends Activity {
    RecordingMfccService mService;
    boolean isBound = false;
    Intent intentBindService;
    
	Button btnStartRecording, btnStopRecording;
	TextView txtMessage;
	final static String TAG = "MFCCBindingActivity";
    private static final String appProcessName = "com.example.tarsosaudioproject";

	BroadcastReceiver receiver;
	
	final static String Thesis_Tarsos_CSV_PATH = "Thesis/Tarsos/CSV";
	//final static String Thesis_Tarsos_Logs_PATH = "Thesis/Tarsos/Logs";
	final static String csvFileName = "tarsos_mfcc.csv";
	//final String batteryFileName = "battery_data.txt";
	
	FileOperations fileOprObj;
	 static volatile FileOperations instance = null;
	 

		/**
		 * singleton method to ensure FileOperations instance remains unique so that constructor is not called more than once
		 * 
		 * @param mainBindingActivity
		 * @return
		 */
		//http://howtodoinjava.com/2012/10/22/singleton-design-pattern-in-java/
		public static FileOperations getInstance(BindingActivity mainBindingActivity) {
	        if (instance == null) {
	            synchronized (FileOperations.class) {
	                instance = new FileOperations(mainBindingActivity);
	            }
	        }
	        return instance;
	    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mfcc);

        fileOprObj = getInstance(this);

    	btnStartRecording = (Button)findViewById(R.id.btnStartRecording);
		btnStopRecording = (Button)findViewById(R.id.btnStopRecording);
		txtMessage = (TextView) findViewById(R.id.txtMessage);
		
		btnStartRecording.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				
				
				// TODO Auto-generated method stub
				if (mService.isDispatcherNull() == true) {
					
					
					// Call a method from the LocalService.
		            // However, if this call were something that might hang, then this request should
		            // occur in a separate thread to avoid slowing down the activity performance.
					
					
					mService.initDispatcher();

					//monitorBatteryUsage();

					monitorBatteryUsage("Start");
	        		monitorCpuAndMemoryUsage("Start");

					mService.startMfccExtraction();
					mService.startPitchDetection();
				
					

					
					
					///Testing
					
		            //int num = mService.methodTwo();
		            //Toast.makeText(getApplicationContext(), "number: " + num, Toast.LENGTH_SHORT).show();
					
					
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
				
				//boolean num = mService.methodOne();
	            //Toast.makeText(getApplicationContext(), "number: " + num, Toast.LENGTH_SHORT).show();
	            
				
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
					//monitorBatteryUsage();
	            	monitorBatteryUsage("End  ");
			    	monitorCpuAndMemoryUsage("End  ");
					
					// storing to csv done on a separate thread
				    Runnable runnable = new Runnable() {
				      @Override
				      public void run() {
				           
							try {
								audioFeatures2csv(mService.getMfccList());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
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
    
    }//onCreate

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
    
    
    /*
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	Log.i(TAG, "Destroying activity ..");
    	
    	if(isFinishing()) {
    		Log.i(TAG, "Activity is finishing ..");
    		
    		//stop service as activity being destroyed and we wont use it anymore
    		Intent intentStopService = new Intent(this, RecordingMfccService.class);
    		stopService(intentStopService);
    	}
    }
    */
    
   

    @Override
    protected void onStop() {
        super.onStop();
    	Log.i(TAG, "onStop");

    	
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
        public void onServiceConnected(ComponentName className,
                IBinder service) {
        	Log.i(TAG, "onServiceConnected");

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            isBound = true;
            
            txtMessage.setText("Connected");
            
            
            
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        	Log.i(TAG, "onServiceDisConnected");

            isBound = false;
            txtMessage.setText("Disconnected");

        }
    };
    
    
    
    /**
     * Collection of Methods to handle response from Service
     */
    
    private void audioFeatures2csv(ArrayList<float[]> csvInput) throws IOException {

		//get path to external storage (SD card)
		String csvfileStoragePath = Environment.getExternalStorageDirectory() + File.separator + Thesis_Tarsos_CSV_PATH;
		File sdCsvStorageDir = new File(csvfileStoragePath);

		//create storage directories, if they don't exist
		if(!sdCsvStorageDir.exists())
			sdCsvStorageDir.mkdirs();

		if(sdCsvStorageDir.exists())
		{
		
			PrintWriter csvWriter;
			try
			{
				String filePath = sdCsvStorageDir.toString() + File.separator + csvFileName;

								
				csvWriter = new  PrintWriter(new FileWriter(filePath,false));
	
				for(float[] oneline: csvInput)
				{
					for(float d : oneline)
					{
						csvWriter.print(d + ",");

					}
					csvWriter.print("\r\n");

				}
	
				Log.i("CSV Writer", "CSV Data Written !!");

				runOnUiThread(new Runnable() {
				     @Override
				     public void run() {
				    	 
							txtMessage.setText("CSV Data Written Successfully !!");

				    }
				});
				
				csvWriter.close();
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}//end of audio2csv
    
    private void showPitchOnUI(final float pitchInHz)
    {
    	runOnUiThread(new Runnable() {
		     @Override
		     public void run() {
		    					    	
		    	if(pitchInHz == -1)
		    		txtMessage.setText("Silent");
		    	else
		    		txtMessage.setText("Speaking");
		    }
		});
    }
    
    /*
  //http://androidtrainningcenter.blogspot.de/2013/09/android-battery-management-api-getting.html
  		//http://virtuallyhyper.com/2012/12/find-out-battery-status-of-rooted-andoid-phone-using-adb/
  		private void monitorBatteryUsage()
  		{
  			  IntentFilter mIntentFilter = new IntentFilter();  
  	          mIntentFilter.addAction(Intent.ACTION_BATTERY_LOW);  
  	          mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);  
  	          mIntentFilter.addAction(Intent.ACTION_BATTERY_OKAY);  
  	          Intent batteryIntent = registerReceiver(null, mIntentFilter);  
  	          float batteryLevel = getBatteryLevel(batteryIntent);  
  	          //Log.i(TAG, "MFCC Battery Level : " + String.valueOf(batteryLevel));  
  	          
  	          appendFile(batteryFileName, String.valueOf(batteryLevel) + " " + getFormattedTimeStamp());
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
  		 
  		 
  		//http://examples.javacodegeeks.com/core-java/io/fileoutputstream/append-output-to-file-with-fileoutputstream/
			private void appendFile(String filename, String dataToAppend)
			{
				
				 //File direct = new File(SD_PATH + SD_FOLDER_PATH_LOGS);
				 String logsfileStoragePath = Environment.getExternalStorageDirectory() + File.separator + Thesis_Tarsos_Logs_PATH;
				 File sdLogsStorageDir = new File(logsfileStoragePath);

				 //File file = new File(SD_PATH + SD_FOLDER_PATH_LOGS + File.separator + filename);
				 File file = new File(sdLogsStorageDir.toString() + File.separator + batteryFileName);

			     if(!sdLogsStorageDir.exists())
			     {
			         sdLogsStorageDir.mkdir();
			     }        
			     
			     if (sdLogsStorageDir.exists()) {
			    	 
			    	 PrintWriter pw;
			    	 
		             try {
		            
		            	 
		                 
		                 FileOutputStream fos = new FileOutputStream(file, true);
		                 pw = new PrintWriter(fos);
		                 pw.println(dataToAppend);
		                 pw.flush();
		                 pw.close();
		                 
		                 
		                 
		                 //Toast.makeText(MfccActivity.this, "Battery data stored in text file", Toast.LENGTH_LONG).show();
		     	    	Log.i(TAG, "MFCC battery data appended : " + dataToAppend);

		             } catch (IOException e) {
		                 e.printStackTrace();
		             }
		             
			     }
			     
			     


			}
			
			*/
    

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
			line = "\n\n\n____New Experiment____";
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
			line = "\n\n\n____New Experiment____";
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
	


		

	
	
	
}