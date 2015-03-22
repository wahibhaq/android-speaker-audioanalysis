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

/**
 * Monitor cpu usage, memory usage and dump all calculated values using {@link FileOperations}
 * 
 * @author Wahib-Ul-Haq 
 * Mar 22, 2015
 *
 */

public class MonitoringData {
	
	private Context appContext;
	SharedData shared = null;
	
    final static String TAG = "MonitoringData"; //Voice Recognizer with Superpowered functionality

	FileOperations fileOprObj;
    static volatile FileOperations fileInstance = null;
    
    ArrayList<Integer> cpuUsageValuesList;
    private static DescriptiveStatistics statObj;
	DecimalFormat mathFormat = new DecimalFormat("#0.000"); // will round and display the number to four decimal places. No more, no less.

	Thread fetchCpuThread = null;

	
	public MonitoringData(Context context)	{
		appContext = context;
        
		shared = new SharedData();
	}
	
	public MonitoringData() {
		//only used by MfccService
		
		shared = new SharedData();
	}
	
	public MonitoringData(Context context, FileOperations obj) {
		appContext = context;
		fileOprObj = obj;
		shared = new SharedData();
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
			
			p = Runtime.getRuntime().exec(new String[] { "sh", "-c", "top -n 1 | grep " + SharedData.appProcessName });//appProcessName });
			
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
			
	        if(processInfo.processName.equals(SharedData.appProcessName)) {//appProcessName) ){
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
	
	static int countCpuValue = 0;
	
	public void dumpRealtimeCpuValues()
	{
		String line = "";
		
		if(countCpuValue == 0)
		{
			//first line to identify start of experiment
			
			line = "\n\n____New Experiment____" + getFormattedTimeStamp() + "____" + SharedData.fftType + "\n\n";
			fileOprObj.appendToCpuUsageFile(line);
			
	        statObj = new DescriptiveStatistics();
	        
			countCpuValue = 1;
						
		}
		else
		{
			//dump actual values
			
			line = getCpuUsage()+",";
			fileOprObj.appendToCpuUsageFile(line);
			
	    	//Log.i(TAG, "MFCC Monitor cpu usage value : " + getCpuUsage());
	    	
	    	if(getCpuUsage().length() > 0)
	    		statObj.addValue(Double.valueOf(getCpuUsage().toString()));
		}
		
	    //Log.i(TAG, "dumpRealtimeCpuValues()");

		
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
		
		countCpuValue = 0;//resetting
	}
	
	 private String getFormattedTimeStamp() {

		  	Long tsLong = System.currentTimeMillis();

		    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
		    cal.setTimeInMillis(tsLong);
		    String date = DateFormat.format("dd-MM-yyyy_HH:mm:ss", cal).toString();
		    return date;
		}
	

	
	
	
}