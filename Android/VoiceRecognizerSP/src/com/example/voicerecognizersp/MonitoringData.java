package com.example.voicerecognizersp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;



public class MonitoringData {
	
	private Context appContext;
    private static final String appProcessName = "com.example.voicerecognizersp";
    final static String TAG = "VoiceRecognizerSP"; //Voice Recognizer with Superpowered functionality

	FileOperations fileOprObj;
    static volatile FileOperations fileInstance = null;
    
    ArrayList<Integer> cpuUsageValuesList;
    private static DescriptiveStatistics statObj;
	DecimalFormat mathFormat = new DecimalFormat("#0.000"); // will round and display the number to four decimal places. No more, no less.

	Thread fetchCpuThread = null;

    
    /**
	 * singleton method to ensure FileOperations instance remains unique so that constructor is not called more than once
	 * 
	 * @param mainBindingActivity
	 * @return
	 */
	//http://howtodoinjava.com/2012/10/22/singleton-design-pattern-in-java/
	public static FileOperations getFileInstance() {
        if (fileInstance == null) {
            synchronized (FileOperations.class) {
                fileInstance = new FileOperations();
            }
        }
        return fileInstance;
    }
	
	
	public MonitoringData(Context context)
	{
		appContext = context;
        fileOprObj = getFileInstance();
        
		
	}
	

	/**
	 * uses linux "top" command to fetch cpu percentage value which shows how much this app is
	 * contributing to total cpu usage
	 * 
	 * @return
	 */
	//http://m2catalyst.com/tutorial-finding-cpu-usage-for-individual-android-apps/ 
	public String getCpuUsage()
	{
				
		String cpuUsageValue = "";

		Process p = null;
		try {
			
			p = Runtime.getRuntime().exec(new String[] { "sh", "-c", "top -n 1 | grep " + appProcessName });
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		
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

			int posPercentage = line.indexOf("%");
			cpuUsageValue = line.substring(posPercentage-4, posPercentage).trim();
			
			//[19472, , 0, , , 0%, S, , , , 15, 904664K, , 44148K, , fg, u0_a141, , tum.laser.voicerecognizer]
			
			//Log.i(TAG, "MFCC cpu usage value : " + cpuUsageValue);

			
		}
		else
		{
			Log.i(TAG, "MFCC command line is null");

		}
		
		
		return cpuUsageValue;
		
		
	}
	
	/**
	 * Function computes "Total Private Dirty Memory" consumed by this application only.
	 * @return
	 */
	public String getMemoryUsage()
	{
		ActivityManager mgr = (ActivityManager)appContext.getSystemService("activity");
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
	
	static int count = 0;
	
	public void dumpRealtimeCpuValues()
	{
		String line = "";
		
		if(count == 0)
		{
			//first line to identify start of experiment
			
			line = "\n\n____New Experiment____" + getFormattedTimeStamp()+"\n\n";
			fileOprObj.appendToCpuUsageFile(line);
			
	        //cpuUsageValuesList = new ArrayList<Integer>();
	        statObj = new DescriptiveStatistics();
	        
			count = 1;
						
		}
		else
		{
			//dump actual values
			
			line = getCpuUsage()+",";
			fileOprObj.appendToCpuUsageFile(line);
			
	    	//Log.i(TAG, "MFCC Monitor cpu usage value : " + getCpuUsage());
	    	
	    	if(!getCpuUsage().equals(""))
	    		statObj.addValue(Double.valueOf(getCpuUsage().toString()));
		}
		
	}
	
	public double calculateMeanCpuValues()
	{
		
		return Double.valueOf(mathFormat.format(statObj.getMean()));
	}
	
	public double calculateSdevationCpuValues()
	{
		return Double.valueOf(mathFormat.format(statObj.getStandardDeviation()));

	}
	
	public void dumpMeanAndSdForCpu()
	{
		fileOprObj.appendToCpuUsageFile("\n\nMean : " + calculateMeanCpuValues() + " --- Standard Deviation : " + calculateSdevationCpuValues() + "\n");
	}
	
	 private String getFormattedTimeStamp() {

		  	Long tsLong = System.currentTimeMillis();

		    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
		    cal.setTimeInMillis(tsLong);
		    String date = DateFormat.format("dd-MM-yyyy_HH:mm:ss", cal).toString();
		    return date;
		}
	

	
	
	
}