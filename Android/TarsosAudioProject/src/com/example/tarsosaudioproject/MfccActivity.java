/*
 * 
 * API : http://0110.be/releases/TarsosDSP/TarsosDSP-latest/TarsosDSP-latest-Documentation/
 */

package com.example.tarsosaudioproject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;


import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import com.opencsv.CSVWriter;



public class MfccActivity extends Activity {

	AudioDispatcher dispatcher = null;
	final double endTime = 20.0;
	
	static int mfccIndex = 0;
	ArrayList<float[]> mfccList;
	
	//MFCC attributes
	final int samplesPerFrame = 512;
	final int sampleRate = 16000;
	final int amountOfCepstrumCoef = 19; //actually 18 but energy column would be discarded
	int amountOfMelFilters = 30; 
	float lowerFilterFreq = 133.3334f;
	float upperFilterFreq = ((float)sampleRate)/2f;
	
	Button btnStartRecording, btnStopRecording;
	TextView txtMessage;
	
	final static String Thesis_Tarsos_CSV_PATH = "Thesis/Tarsos/CSV";
	final static String Thesis_Tarsos_Logs_PATH = "Thesis/Tarsos/Logs";
	final static String csvFileName = "tarsos_mfcc.csv";
	final String batteryFileName = "battery_data.txt";
	
	final static String TAG = "MFCC";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mfcc);
		
		mfccList = new ArrayList<float[]>();
				
		btnStartRecording = (Button)findViewById(R.id.btnStartRecording);
		btnStopRecording = (Button)findViewById(R.id.btnStopRecording);
		txtMessage = (TextView) findViewById(R.id.txtMessage);
		
			
		btnStartRecording.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				
				if(dispatcher == null)
				{
					txtMessage.setText("Recording & MFCCs !");


					//sampleRate, audioBufferSize, int bufferOverlap 
					//Florian suggested to use 16kHz as sample rate 
					dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(16000, 1024 ,0); //(22050,1024,0);
					
					monitorBatteryUsage();

					handleMfccExtraction();
					handlePitchDetection();

				}

			}
		});
		
		btnStopRecording.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(dispatcher != null)
				{

					dispatcher.stop();
					dispatcher = null;
					
					txtMessage.setText("Recording stopped !");

					monitorBatteryUsage();

					
						// do something long
					    Runnable runnable = new Runnable() {
					      @Override
					      public void run() {
					        
					           
								try {
									
									//writeToCSV(mfccList);
									audioFeatures2csv(mfccList);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							
								
					        }
					      
					    };
					    
					    new Thread(runnable).start();
					    

					
					
				}

			}
		});
		
		
	}
	
	
	//From Tarsos -> 29 columns
	/*[-10.253985, 8.859316, -3.5757537, 0.12670934, -2.4220033, 2.8631196, -2.0005736, 1.0401096,
	 *  -0.6216758, -0.91912335, -1.3374954, 1.0321274, -2.887241, 0.7139418, 0.33881032, -0.75257796,
	 *   -0.8945726, 2.1461666, -2.1955435, 2.3723443, -1.5888317, 1.673526, -0.94757396, 0.65513366, 
	 *   -1.056742, 1.8233569, 0.23140988, -0.4569312, 0.100550026]
	 */
	
	//From 200Mels
	/*
	 * -2.2717,0.19408,-1.3273,0.22624,0.55402,-0.72029,0.15448,1.0724,-0.074159,-1.2252,0.14226,-0.55678,0.69244,
	 * -0.45816,-0.10579,0.94975,0.30049,0.19124,3.8249,-1.3157,0.92723,0.77439,1.886,2.6084,2.7006,-1.6056,-0.2795,
	 * -0.48608,-1.2018,0.85924,2.071,-0.40512,-3.1347,0.023976,-1.6196,1.0791,0
	 */

	//after getting feedback from owner Joren Six in email
	private void handleMfccExtraction()
	{
	
				
		//MFCC( samplesPerFrame, sampleRate ) //typical samplesperframe are power of 2 & Samples per frame = (sample rate)/FPS
		//Florian suggested to use 16kHz as sample rate and 512 for frame size
		final MFCC mfccObj = new MFCC(samplesPerFrame, sampleRate, amountOfCepstrumCoef, amountOfMelFilters, lowerFilterFreq, upperFilterFreq ); //(1024,22050); 
		
		/*AudioProcessors are responsible for actual digital signal processing. AudioProcessors are meant to be chained 
		e.g. execute an effect and then play the sound. 
		The chain of audio processor can be interrupted by returning false in the process methods.
		*/ 
		dispatcher.addAudioProcessor( mfccObj);	
		//handlePitchDetection();
		dispatcher.addAudioProcessor(new AudioProcessor() {
			
			@Override
			public void processingFinished() {
				// TODO Auto-generated method stub
				//Notify the AudioProcessor that no more data is available and processing has finished
				
				
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				// TODO Auto-generated method stub
				//process the audio event. do the actual signal processing on an (optionally) overlapping buffer
				

				//float[] output = mfccObj.getMFCC();
				//Log.i("MFCC", String.valueOf(Arrays.toString(output)));

				//Log.i("MFCC", String.valueOf(Arrays.toString(mfccObj.getMFCC())));

				
				//fetchng MFCC array and removing the 0th index because its energy coefficient and florian asked to discard
				float[] mfccOutput = mfccObj.getMFCC();
				mfccOutput = Arrays.copyOfRange(mfccOutput, 1, mfccOutput.length);
				//Log.i("MFCC", String.valueOf(Arrays.toString(mfccOutput)));

				//Storing in global arraylist so that i can easily transform it into csv
				mfccList.add(mfccOutput);
				//Log.i("MFCC", String.valueOf(Arrays.toString(mfccList.get(mfccIndex++))));
				
				
				return true;
			}
		});
		

		//its better to use thread vs asynctask here. ref : http://stackoverflow.com/a/18480297/1016544
		new Thread(dispatcher, "Audio Dispatcher").start();

		

	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    
	    if(dispatcher != null)
	    	dispatcher.stop();
	}
	
	public void audioFeatures2csv(ArrayList<float[]> csvInput) throws IOException {

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
	//[3.998066, 6.3241587, -1.8274933, 4.3879685, -2.469697, 4.2041035, -2.6247728, 3.609442, -3.1616154, 3.322619,
	//-3.1956894, 3.3220851, -3.1546564, 3.3215897, -2.7536705, 3.0538366, -2.8969948, 2.9581883, -2.4636075, 2.7506678,
	//-2.3384945, 2.2498512, -1.8880569, 1.8920029, -1.2648101, 1.3696442, -0.8091972, 0.6737367, -0.2902786]
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
	}
	
	
	
	
	private void handlePitchDetection()
	{
		//commented because dispatcher is initiated in onCreate()
		//AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);
		
		
		
		//algorithm, sampleRate, bufferSize, handler
		dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN, 16000, 512, new PitchDetectionHandler() {
			
			@Override
			public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
				
				//-1 means no sound 
				final float pitchInHz = pitchDetectionResult.getPitch();
				//Log.i("Pitch", String.valueOf(pitchInHz));
				
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
		}));
		
		//commented because thread is already initiated in handleMFCCextraction()
		//new Thread(dispatcher,"Audio Dispatcher").start();
	}
	
	/*Code for fetching battery information and writing in CSV file*/
	
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

		

	
}
