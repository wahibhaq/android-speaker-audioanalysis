/**
 * This file is just to manage file operations of creation, appending and deletion in Logs Dir i.e battery data and memory & cpu data
 * 
 */

package com.example.voicerecognizersp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.util.Log;


/**
 * Manages all operations which involved writing to files on sd card
 * 
 * @author Wahib-Ul-Haq 
 * Mar 22, 2015
 *
 */

public class FileOperations
{
	

	final static String TAG = "FileOperations"; //Voice Recognizer with Superpowered functionality

	Context activityContext = null;
	Double[] deltaList;
	
	DecimalFormat doublePrecision = new DecimalFormat("#0.00000");
	
	public FileOperations(Context context)
	{
		//only to be used by MonitoringData and BindingActivity
		activityContext = context;


	}
	
	public FileOperations() {
		//only to be used by MfccService
		
		deltaList= new Double[17];
	}
	
		
	public void resetDirs()
	{

		if(SharedData.fftType.equals("FFT_CT"))
			SharedData.SD_FOLDER_PATH = "/Thesis/VoiceRecognizer";
		else if(SharedData.fftType.equals("FFT_SP"))
			SharedData.SD_FOLDER_PATH = "/Thesis/VoiceRecognizerSP";
		
		SharedData.SD_FOLDER_PATH_LOGS = SharedData.SD_FOLDER_PATH + "/Logs"; 
		SharedData.SD_FOLDER_PATH_CSV = SharedData.SD_FOLDER_PATH + "/CSV"; 

		if(recreateDirsIfExist())
		{
			//if successfully dir created
			appendToBatteryFile("____New Experiment____" + SharedData.fftType);
			appendToMemCpuFile("____New Experiment____" + SharedData.fftType);
			
		}
		
	}
	
	private void deleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	    	for (File child : fileOrDirectory.listFiles())
	    		child.delete();

	    //fileOrDirectory.delete(); //to prevent deletion of dirs. sometimes dirs are not recreated fully 
	}
	
	//http://examples.javacodegeeks.com/core-java/io/fileoutputstream/append-output-to-file-with-fileoutputstream/
		/**
		 * To add battery stats data to the file in Logs folder
		 * 
		 * @param dataToAppend
		 */
		public void appendToBatteryFile(String dataToAppend)
		{
			 
		     
			 String logsfileStoragePath = SharedData.SD_PATH + File.separator + SharedData.SD_FOLDER_PATH_LOGS;
			 File sdLogsStorageDir = new File(logsfileStoragePath);

			 File file = new File(sdLogsStorageDir.toString() + File.separator + SharedData.batteryFileName);

		     
		     if (sdLogsStorageDir.exists()) {

		    	 PrintWriter pw;

	             try {
	            
	            	 
	                 
	                 FileOutputStream fos = new FileOutputStream(file, true);
	                 pw = new PrintWriter(fos);
	                 pw.println(dataToAppend);
	                 pw.flush();
	                 pw.close();
	                 
	                 
	                 if(activityContext != null)
	                	 ((BindingActivity) activityContext).showToast("Battery data stored in text file");
	                 
	     	    	Log.i(TAG, "MFCC battery data appended : " + dataToAppend);

	             } catch (IOException e) {
	                 e.printStackTrace();
	             }
	         }
		     else {
                 
		    	 if(activityContext != null)
                	 ((BindingActivity) activityContext).showToast("Battery File doesn't exist !");
		     }
		     


		}
		
		
		
		/**
		 * To check if Logs Dir exist or if not then create it
		 */
		public boolean recreateDirsIfExist()
		{
			File dirParent = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_PARENT);
			
			File dirMain = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH);
			
			File dirCSV = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_CSV);
			if(dirCSV.exists())
				deleteRecursive(dirCSV);
			
			File dirLogs = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_LOGS);
			if(dirLogs.exists())
				deleteRecursive(dirLogs);
			
			try
			{
				if(!dirParent.exists())
			    	dirParent.mkdir();
			    if(!dirMain.exists())
			    	dirMain.mkdir();
			    if(!dirCSV.exists())
			    	 dirCSV.mkdir();
			    if(!dirLogs.exists())
			    	 dirLogs.mkdir();
			    
			    
			}
			catch(Exception e)
			{
                if(activityContext != null)
                	((BindingActivity) activityContext).showToast("Unable to create Directories !");


				e.printStackTrace();
				return false;
			}
			
			
			
			return true;
		     
		}
		
		/**
		 * To check if all dirs exist or not
		 * @return
		 */
		public boolean isAllDirsExist()
		{
			File dirParent = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_PARENT);	
			File dirMain = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH);
			File dirCSV = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_CSV);
			File dirLogs = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_LOGS);
			
			if(dirParent.exists() && dirMain.exists() && dirCSV.exists() && dirLogs.exists())
				return true;
			else 
				return false;
		}
		
		public void resetFiles()
		{
			 String logsfileStoragePath = SharedData.SD_PATH + File.separator + SharedData.SD_FOLDER_PATH_LOGS;
			 File sdLogsStorageDir = new File(logsfileStoragePath);

			 File fileBattery = new File(sdLogsStorageDir.toString() + File.separator + SharedData.batteryFileName);
			 File fileMemCpu = new File(sdLogsStorageDir.toString() + File.separator +SharedData. memcpuFileName);
			 
			 fileBattery.delete();
		     fileMemCpu.delete();
		     
		     String csvfileStoragePath = SharedData.SD_PATH + File.separator + SharedData.SD_FOLDER_PATH_CSV;
			 File sdCsvStorageDir = new File(csvfileStoragePath);

			 File fileCSV = new File(sdCsvStorageDir.toString() + File.separator + SharedData.csvFileName);
			 fileCSV.delete();
		     
		}
		
		/**
		 * To add cpu and memory stats data to the file in Logs folder
		 * 
		 * @param dataToAppend
		 */
		public void appendToMemCpuFile(String dataToAppend)
		{
			 		     
			 String logsfileStoragePath = SharedData.SD_PATH + File.separator + SharedData.SD_FOLDER_PATH_LOGS;
			 File sdLogsStorageDir = new File(logsfileStoragePath);

			 File file = new File(sdLogsStorageDir.toString() + File.separator + SharedData.memcpuFileName);

		     if (sdLogsStorageDir.exists()) {

		    	 PrintWriter pw;

	             try {
	                 
	                 FileOutputStream fos = new FileOutputStream(file, true);
	                 pw = new PrintWriter(fos);
	                 pw.println(dataToAppend);
	                 pw.flush();
	                 pw.close();
	                 
	                 if(activityContext != null)
	                	 ((BindingActivity) activityContext).showToast("Memory & CPU data stored in text file");

	              
	                 
	     	    	Log.i(TAG, "MFCC Memory & CPU data appended : " + dataToAppend);

	             } catch (IOException e) {
	                 e.printStackTrace();
	             }
	         }
		     else {
		    	 
                 if(activityContext != null)
                	 ((BindingActivity) activityContext).showToast("MemCpu File doesn't exist !");
		     }
		     
		     


		}
		
		/**
		 * To add real-time cpu usage values 
		 * 
		 * @param dataToAppend
		 */
		public void appendToCpuUsageFile(String dataToAppend)
		{
			 		     
			 String logsfileStoragePath = SharedData.SD_PATH + File.separator + SharedData.SD_FOLDER_PATH_LOGS;
			 File sdLogsStorageDir = new File(logsfileStoragePath);

			 File file = new File(sdLogsStorageDir.toString() + File.separator + SharedData.cpuRealFileName);

		     
		     
		     if (sdLogsStorageDir.exists()) {

		    	 PrintWriter pw;

	             try {
	            
	            	 
	                 
	                 FileOutputStream fos = new FileOutputStream(file, true);
	                 pw = new PrintWriter(fos);
	                 pw.print(dataToAppend);
	                 pw.flush();
	                 pw.close();
	                 
	                 if(activityContext != null)
	                	 ((BindingActivity) activityContext).showToast("CPU Real-time Usage data stored in text file");

	                 
	     	    	Log.i(TAG, "MFCC CPU real-time usage data appended : " + dataToAppend);

	             } catch (IOException e) {
	                 e.printStackTrace();
	             }
	         }
		    
		     


		}
		
		
		/**
		 * Takes the final compiled list of cepstral features and stores them in a csv file on the sdcard.
		 * Each frame is represented by its MFCCs (without energy). 
		 * 
		 * @param arrayList : list of windows
		 */
		public void appendToCsv(LinkedList<ArrayList<double[]>> featureList) {
			
			

			PrintWriter csvWriter;
			try
			{
			    
				File csvFile = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_CSV + File.separator + SharedData.csvFileName);
				
				csvWriter = new  PrintWriter(new FileWriter(csvFile,true));
				
				
				for (ArrayList<double[]> window : featureList) {
					
					for (double[] newline : window) {
						
						int colCount = 1;
						for (double element : newline) {
							
							if(colCount == newline.length)
								csvWriter.print(element); //not to add comma when its last column
							else
								csvWriter.print(element + ",");
							
							++colCount;
						}
						
						
						csvWriter.print("\r\n");
					}
				}
			
			
				csvWriter.close();
				
		    	Log.i(TAG, "MFCC audio2csv() data dumped");
		    	

		    	

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		
		/**
		 * This is only to be used as input for VAD module
		 * 
		 * @param featureList
		 */
		/*public void updateCsvForVad(LinkedList<ArrayList<double[]>> featureList) {
			
			

			PrintWriter csvWriter;
			try
			{
			    
				
				//storage/emulated/0/Thesis/VoiceRecognizerSP/CSV/20MfccFeatures_1.csv
				File csvFile = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_CSV + File.separator + SharedData.vadCsvFileName);
				
				csvWriter = new  PrintWriter(new FileWriter(csvFile,false)); //not appending
				
				for (ArrayList<double[]> window : featureList) {
					
					for (double[] newline : window) {
					
						int colCount = 1; 
						int tempColCount = 0, deltaColCount = 0;
						double temp[] = new double[2];
						boolean tempBool = true;
						
						for (double element : newline) {
							
							temp[tempColCount++] = element;
							
							
							//handling filling of delta list of 18 columns from 2nd element on wards
							if(colCount >= 2) {
								
								if(tempBool) { 
									deltaList[deltaColCount] = temp[1] - temp[0];
									tempBool = false;
									tempColCount = 0;

								}
								else {
									deltaList[deltaColCount] = temp[0] - temp[1];
									tempBool = true;
								}
								
								++deltaColCount;
								
							}
							
							
							//csvWriter.print(element + ",");
							csvWriter.print(doublePrecision.format(element) + ",");
							
							++colCount;
						}
						
						deltaColCount = 1;
						for(double delta : deltaList) {
							
							if(deltaColCount == deltaList.length) {
								//csvWriter.print(delta + "," + delta + ",1"); //not to add comma when its last column
								//csvWriter.print(doublePrecision.format(delta) + "," + doublePrecision.format(delta-1) + ",1");
								
								csvWriter.print(doublePrecision.format(delta) +  ",1");//but this last 1s is a dummy 36th column just to match training dataset

							}
							else {
								//csvWriter.print(delta + ",");
								csvWriter.print(doublePrecision.format(delta) + ",");
							}
							
							++deltaColCount;
							
						}
						
						csvWriter.print("\r\n");
					}
				}
			

				csvWriter.close();
				
		    	Log.i(TAG, "MFCC VAD audio2csv() data dumped");
		    	

		    	

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}*/
		
		/*private void appendDelta(LinkedList<ArrayList<double[]>> featureList) {
			
			for (ArrayList<double[]> window : featureList) {
				
				for (double[] newline : window) {
				
					int colCount = 1; 
					int tempColCount = 0, deltaColCount = 0;
					double temp[] = new double[2];
					boolean tempBool = true;
					
					for (double element : newline) {
						
						temp[tempColCount++] = element;
						
						
						//handling filling of delta list of 18 columns from 2nd element on wards
						if(colCount >= 2) {
							
							if(tempBool) { 
								deltaList[deltaColCount] = temp[1] - temp[0];
								tempBool = false;
								tempColCount = 0;

							}
							else {
								deltaList[deltaColCount] = temp[0] - temp[1];
								tempBool = true;
							}
							
							++deltaColCount;
							
						}
								
						
						++colCount;
					}
				}
			}
			
		}*/
		

}