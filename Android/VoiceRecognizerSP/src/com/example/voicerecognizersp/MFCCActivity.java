package com.example.voicerecognizersp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Trace;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.javagmm.FFT;
import com.example.javagmm.FrequencyProperties;
import com.example.javagmm.MFCC;
import com.example.javagmm.Window;

/**
 * VoiceRecognizer
 * A speech processor for the Android plattform.
 * 
 * @author Florian Schulze, schulze@in.tum.de, 2013
 * 
 *
 * contains code taken from:
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 */

/**
 * Copy of MainActivity which was already running and tested for MFCC extraction and storing in CSV file.
 * Reason to create a separate activity and copying is to apply optimizing technique. 
 * 
 * This also contains Integration of FFT module from Superpowered SDK and systrace code to evaluate
 * performance of FFT function from Superpowered and Cooley-Tukey
 * 
 * @author Wahib-Ul-Haq
 *
 */

public class MFCCActivity extends Activity {

	private static String APP_NAME = "VoiceRecognizerSP";
	private TextView txtMessage;

	private static int RECORDER_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
	private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static int RECORDER_SAMPLERATE = 16000; //16Hz frequency 

	//private static int WINDOWS_TO_RECORD = 4;

	private static int FFT_SIZE = 512; //512 = default;
	//8000samples/s divided by 256samples/frame -> 32ms/frame (31.25ms)
	private static int FRAME_SIZE_IN_SAMPLES = 512; //512 = default value;

	//32ms/frame times 64frames/window = 2s/window
	private static int WINDOW_SIZE_IN_FRAMES = 64;

	private static int MFCCS_VALUE = 19; //MFCCs count 
	private static int NUMBER_OF_FINAL_FEATURES = MFCCS_VALUE - 1; //discard energy
	private static int MEL_BANDS = 20; //use FB-20
	private static double[] FREQ_BANDEDGES = {50,250,500,1000,2000};

	private int bufferSize = 0;

	private static int[] freqBandIdx = null;
	public double[] featureBuffer = null;

	private Thread recordingThread = null;
	private FFT featureFFT = null; //not using now and instead using FFT from Superpowered Sdk
	private MFCC featureMFCC = null;
	private Window featureWin = null;
	private AudioRecord audioRecorder = null;

	private boolean isRecording = false;
	
	private static int DROP_FIRST_X_WINDOWS = 1;
	
	//variabled added later by Wahib
	final double windowsToRead = 10;//2.5; //audio file duration in seconds
	public static final File SD_PATH = Environment.getExternalStorageDirectory();
	public static final String SD_FOLDER_PATH = "/Thesis/VoiceRecognizerSP";
	public static final String SD_FOLDER_PATH_CSV = SD_FOLDER_PATH + "/CSV"; 

	static Handler repeatRecordHandler = null;
	static Runnable repeatRecordRunnable = null;
	Thread repeatRecordThread = null;
	
	//private final int RECORDING_REPEAT_CYCLE = 10000; //1000 = 10 seconds
	static int cycleCount = 0;
	//final int maxCycleCount = 100; //use it to set total time to run; total time (sec) = maxCycleCount * RECORDING_REPEAT_CYCLE
	final String csvFileName = "20MfccFeatures.csv"; //"20MfccFeatures_";

	final static String TAG = "VoiceRecognizerSP"; //Voice Recognizer with Superpowered functionality

	ArrayList<LinkedList<ArrayList<double[]>>> mfccFinalList;

	private static final String appProcessName = "com.example.voicerecognizersp";
	
	FileOperations fileOprObj;
	static volatile FileOperations instance = null;
	
	/////////////////Superpowered /////////
	
	public static final int logSize = 8; //9; //2^9 = 512 = FFTSize
	public static final boolean ifReal = true;  //We are using FFT-in-Place 
	public static final boolean ifForward = false;
	
	public static String fftType = "FFT_CT"; //FFT_CT : Cooley-Tukey and other option is FFT_SP : Superpowered

	/*
	 * These two functions are taken from Superpowered SDK
	 */
	//http://superpowered.com/docs/_superpowered_f_f_t_8h.html 
    private native void onFFTPrepare(int logSize, boolean ifReal);
    private native void onFFTReal(float[] real, float[] imag, int logSize, boolean ifForward);

    static {
		Log.i(TAG, "Superpowered lib loaded successfully");
        System.loadLibrary("SuperpoweredExample");
    }
    
    ///////////////////////////////////////////


	/**
	 * Start recording audio in a separate thread.
	 * If the switch in our main view is checked, the audio will be saved in
	 * a file {@link #storeAudioStream()}. Otherwise we process the stream
	 * immediately {@link #processAudioStream()}.
	 */
	protected void recordAudioFromMic() {
		
		synchronized (this) {
			if (isRecording)
				return;
			else
				isRecording = true;
		}
		
		bufferSize = AudioRecord.getMinBufferSize(
				RECORDER_SAMPLERATE,
				RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING);

		bufferSize = Math.max(bufferSize, RECORDER_SAMPLERATE*2);
		
		audioRecorder = new AudioRecord(
				RECORDER_SOURCE,
				RECORDER_SAMPLERATE,
				RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING,
				bufferSize);
		
		if(fftType.equals("FFT_SP"))
			onFFTPrepare(logSize, ifReal);
		
		audioRecorder.startRecording();
		recordingThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				processAudioStream();
				
				
			}
		}, APP_NAME + "_Thread");
		
		recordingThread.start();
	}


	/**
	 * Stop recording. Kills off any ongoing recording.
	 */
	protected void stopRecording() {
		
		synchronized (this) {
			if (!isRecording)
				return;
			else
			{
				
				
				if(audioRecorder != null)
				{
					isRecording = false;

					Toast.makeText(getApplicationContext(), "Stopped recording. Resetting...", Toast.LENGTH_SHORT).show();
					txtMessage.setText("Stopped recording. Resetting...");


					audioRecorder.stop();
					audioRecorder.release();
					audioRecorder = null;
					
					cycleCount=0;
					
					
					recordingThread.interrupt();
					recordingThread = null;
					
					
					
					repeatRecordHandler.removeCallbacks(repeatRecordRunnable);
					
					
			    	Log.i(TAG, "MFCC stopRecording() released !");
			    	monitorBatteryUsage("End  ");
			    	monitorCpuAndMemoryUsage("End  ");
			    	
			    	audioFeatures2csv(mfccFinalList);
				}
				else
				{
					isRecording = false;
					Toast.makeText(getApplicationContext(), "Recording already stopped !", Toast.LENGTH_SHORT).show();
					txtMessage.setText("Recording already in stopped state !");

				}

			}
		}

		
		
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopRecording();
	}
	
	/**
	 * singleton method to ensure FileOperations instance remains unique so that constructor is not called more than once
	 * 
	 * @param obj
	 * @return
	 */
	//http://howtodoinjava.com/2012/10/22/singleton-design-pattern-in-java/
	public static FileOperations getInstance(MFCCActivity obj) {
        if (instance == null) {
            synchronized (FileOperations.class) {
                //instance = new FileOperations(obj);
            }
        }
        return instance;
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		repeatRecordHandler = new Handler();

		mfccFinalList = new ArrayList<LinkedList<ArrayList<double[]>>>();
		
		//fileOprObj = new FileOperations(this); //also created dir if not exist
		fileOprObj = getInstance(this);
		
		txtMessage = (TextView) findViewById(R.id.txtMessage);
		
		if(fftType.equals("FFT_CT"))
			txtMessage.setText("MFCC with Cooley-Tukey");
		else if(fftType.equals("FFT_SP"))
			txtMessage.setText("MFCC with Superpowered");

		final Button buttonStart = (Button) findViewById(R.id.buttonStartRec);
		buttonStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				txtMessage.setText("Recording .. ");
				handleRecordingAudio();
				
				
			}
		});

		final Button buttonStop = (Button) findViewById(R.id.buttonStopRec);
		buttonStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				isRecording = true; //for force stopping only
				stopRecording();

			}
		});
		
		
		
	}
	
	private void handleRecordingAudio()
	{


		repeatRecordRunnable = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				++cycleCount;
	        	Log.i(TAG, "MFCC cycle #" + cycleCount);
	        	
	        	if(cycleCount == 1)
	        	{
	        		monitorBatteryUsage("Start");
	        		monitorCpuAndMemoryUsage("Start");
	        		
	        		//includes first time initialization
	        		recordAudioFromMic();
	        	}
	        	else
	        	{
		        	recordingThread = new Thread(new Runnable()
		    		{
		    			@Override
		    			public void run()
		    			{

		    				isRecording = true;
		    				processAudioStream();
		    			}
		    		}, APP_NAME + "_Thread");
		    		
		    		recordingThread.start();
		        	
	        	}
			
	        	
			}
		};
		
		repeatRecordHandler.post(repeatRecordRunnable);
		//repeatRecordHandler.postDelayed(repeatRecordRunnable, RECORDING_REPEAT_CYCLE); //this is to add delay in the beginning which is not required
	}

	
	//TODO: drop frames without speech -> speakersense
	//TODO: cepstral mean normalization -> overview paper
	//TODO: augment features with derivatives -> overview paper
	//TODO: buffer in-between frames vs drop; drop whole window vs frames

	/**
	 * Reads from {@link #audioRecorder} and processes the stream.
	 * For that, it cuts the stream into chunks (frames) which are
	 * processed individually in
	 * {@link #processAudioFrame(int, short[], double[], double[])}
	 * to obtain feature vectors.
	 * Features of multiple frames are then bundled to feature windows
	 * which represent speech utterances.
	 * Before we add a frame to a window we run admission checks.
	 * Frames that are not admitted simply get dropped. Therefore,
	 * a window contains sequential but not necessarily consecutive
	 * frames.
	 * 
	 * v1.0
	 */
	private void processAudioStream()
	{

		short dataFrame16bit[] = new short[FRAME_SIZE_IN_SAMPLES];

		//initialize general processing data
		featureWin = new Window(FRAME_SIZE_IN_SAMPLES); //smoothing window, nothing to do with frame window
		
		if(fftType.equals("FFT_CT"))
			featureFFT = new FFT(FFT_SIZE);
		
		featureMFCC = new MFCC(FFT_SIZE, MFCCS_VALUE, MEL_BANDS, RECORDER_SAMPLERATE);

		freqBandIdx = new int[FREQ_BANDEDGES.length];
		for (int i = 0; i < FREQ_BANDEDGES.length; i ++)
		{
			freqBandIdx[i] = Math.round((float)FREQ_BANDEDGES[i]*((float)FFT_SIZE/(float)RECORDER_SAMPLERATE));
		}

		//lists of windows
		LinkedList<ArrayList<double[]>> featureCepstrums = new LinkedList<ArrayList<double[]>>();

		//windows: list of frames
		ArrayList<double[]> cepstrumWindow = new ArrayList<double[]>(WINDOW_SIZE_IN_FRAMES);

		int readAudioSamples = 0;
		int currentIteration = 0;

		
		showToast("Starting to record...");
    	Log.i(TAG, "MFCC processAudioStream() Staring to Record !");
		//monitorCpuUsage();

    	//analysing and extracting MFCC features in a 30 sec frame. WINDOW_SIZE_IN_FRAMES here refers to 2 sec
		while (currentIteration < windowsToRead * WINDOW_SIZE_IN_FRAMES) //960 = 15*64 -> 15*2s=30s
		{

			// read() kann entweder mind. buffer_size/2 zeichen zur�ckliefern
			// (wir brauchen viel weniger) oder blockiert:
			// http://stackoverflow.com/questions/15804903/android-dev-audiorecord-without-blocking-or-threads
			
			try 
			{
				synchronized (this) {
					if (isRecording)
					{
						readAudioSamples = audioRecorder.read(dataFrame16bit, 0, FRAME_SIZE_IN_SAMPLES);
						//monitorCpuUsage();
					}
					else
					{
						Log.i(TAG, "ProcessAudioStream is killed early");

						//we only get here in case the user kills us off early
						//remove last window as it's incomplete
						if (cepstrumWindow.size() > 0 && cepstrumWindow.size() < WINDOW_SIZE_IN_FRAMES)
						{
							if(!featureCepstrums.isEmpty())
								featureCepstrums.removeLast();
						}
						
				    	mfccFinalList.add(featureCepstrums);
				    	
						audioFeatures2csv(mfccFinalList);
						return;
					}
				}
			}
			catch (Exception e) {
				
				e.printStackTrace();
			}
			

			if (readAudioSamples == 0)
				return;
			
			if (!isFrameAdmittedByRMS(dataFrame16bit))
				continue;

			FrequencyProperties freq = processAudioFrame(readAudioSamples, dataFrame16bit, true);
			
			if (freq == null)
				continue;
			
			//combine WINDOW_SIZE_IN_FRAMES frames to a window
			//2s with a window size of 64
			if (cepstrumWindow.size() == WINDOW_SIZE_IN_FRAMES){
				cepstrumWindow = new ArrayList<double[]>(WINDOW_SIZE_IN_FRAMES);
				featureCepstrums.add(cepstrumWindow);

				
			}
			
			currentIteration++;
			
		
			// Add MFCCs of this frame to our window
			cepstrumWindow.add(freq.getFeatureCepstrum());
			
			//monitorCpuUsage();

	    	Log.i(TAG, "MFCC recording cycle : " + currentIteration);

		}

		showToast("Done recording.");
    	Log.i(TAG, "MFCC processAudioStream() Done Recording !");
		//monitorCpuUsage();

    	mfccFinalList.add(featureCepstrums);
    	
    	repeatCycle();

		return;
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

	
	/**
	 * Takes an audio frame and processes it in the frequency domain:
	 * 1. Applies a Hamming smoothing window
	 * 2. Computes the FFT
	 * 3. Computes the Power Spectral Density for each frequency band
	 * 4. Computes the MFC coefficients
	 * 
	 * @param samples Number of samples in this frame (should be static)
	 * @param dataFrame16bit Array of samples
	 * @param dropIfBad Whether to drop the frame if the spectral entropy is below a threshold
	 * @return FrequencyProperties object containing FFT & MFCC coefs, PSDs;  null if dropped
	 * 
	 * based on code of Funf's AudioFeaturesProbe class
	 * 
	 * Funf: Open Sensing Framework
	 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
	 * Acknowledgments: Alan Gardner
	 * Contact: nadav@media.mit.edu
	 * 
	 * v1.0
	 */
	public FrequencyProperties processAudioFrame(int samples, short dataFrame16bit[], boolean dropIfBad) {
		double fftBufferR[] = new double[FFT_SIZE];
		double fftBufferI[] = new double[FFT_SIZE];
	
		double[] featureCepstrum = new double[MFCCS_VALUE-1];
		


		// Frequency analysis
		Arrays.fill(fftBufferR, 0);
		Arrays.fill(fftBufferI, 0);

		// Convert audio buffer to doubles
		for (int i = 0; i < samples; i++)
		{
			fftBufferR[i] = dataFrame16bit[i];
		}

		// In-place windowing
		featureWin.applyWindow(fftBufferR);

		
		
		Trace.beginSection("ProcessFFT");
		try {
		        Trace.beginSection("Processing onFFTReal");
		        try {
		            // code for superpowered onFFTReal task...
		        	
		        	if(fftType.equals("FFT_SP"))
		        	{
		        		// In-place FFT - this is what we have to use as instructed by Florian
		        		onFFTReal(convertDoublesToFloats(fftBufferR), convertDoublesToFloats(fftBufferI), logSize, ifForward);
		        	}
		        	else if(fftType.equals("FFT_CT"))
		        		featureFFT.fft(fftBufferR, fftBufferI);


		        } finally {
		            Trace.endSection(); // ends "Processing Jane"
		        }
		 } 
		finally {
		        Trace.endSection(); // ends "ProcessPeople"
		 }

		//featureFFT.fft(fftBufferR, fftBufferI);
		//onFFTReal(convertDoublesToFloats(fftBufferR), convertDoublesToFloats(fftBufferI), logSize, ifForward);
		
		
		
		FrequencyProperties freq = new FrequencyProperties();
		
		freq.setFftImag(fftBufferI);
		freq.setFftReal(fftBufferR);
		freq.setFeatureCepstrum(featureCepstrum);

		// Get MFCCs
		double[] featureCepstrumTemp = featureMFCC.cepstrum(fftBufferR, fftBufferI);

		// copy MFCCs
		for(int i = 1; i < featureCepstrumTemp.length; i++) {
			//only keep energy-independent features, drop first coefficient
			featureCepstrum[i-1] = featureCepstrumTemp[i];
		}
		
		return freq;
	}

	public static float[] convertDoublesToFloats(double[] input)
	{
	    if (input == null)
	    {
	        return null; // Or throw an exception - your choice
	    }
	    
	    float[] output = new float[input.length];
	    
	    for (int i = 0; i < input.length; i++)
	    {
	        output[i] = (float) input[i];
	    }
	    return output;
	}
	
	public double getRMS(short[] buffer) {
		double rms = 0;
		for (int i = 0; i < buffer.length; i++) {
			rms += buffer[i] * buffer[i];
		}
		return Math.sqrt(rms / buffer.length);
	}


	/**
	 * 
	 * @param frame Array of features
	 * @return returns whether the frame is to be admitted
	 */
	public boolean isFrameAdmittedByRMS(short[] buffer) {
		//if (getRMS(buffer) < 0.5)
		//	return false;
		return true;
	}

	
	
	/**
	 * Reads from {@link #audioRecorder} and saves the stream to the sdcard.
	 * 
	 * based on code snippets taken from from
	 * http://eurodev.blogspot.de/2009/09/raw-audio-manipulation-in-android.html
	 */
/*	private void storeAudioStream() {
		File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioStream.pcm");
		int suffix = 0;

		while (file.exists())
			file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioStream_copy" + (suffix++) + ".pcm");
			//file.delete();

		// Create the new file.
		try {
			file.createNewFile();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to create " + file.toString());
		}

		try {
			OutputStream os = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(os);
			DataOutputStream dos = new DataOutputStream(bos);

			short dataFrame16bit[] = new short[FRAME_SIZE_IN_SAMPLES];
			int readAudioSamples = 0;
			int currentIteration = 0;

			//String editText = ((EditText) findViewById(R.id.editTextSamples)).getText().toString();
			//int windowsToRead = (editText != null && !editText.isEmpty()) ?
					//Integer.parseInt(editText) : WINDOWS_TO_RECORD;
			
			

			showToast("Starting to record...");

			while (currentIteration < windowsToRead * WINDOW_SIZE_IN_FRAMES) //960 = 15*64 -> 15*2s=30s
			{
				synchronized (this) {
					if (isRecording)
						readAudioSamples = audioRecorder.read(dataFrame16bit, 0, FRAME_SIZE_IN_SAMPLES);
					else {
						//we only get here in case the user kills us off early
						dos.close();
						return;
					}
				}

				currentIteration++;

				if (readAudioSamples == 0)
					return;

				if (currentIteration > WINDOW_SIZE_IN_FRAMES * DROP_FIRST_X_WINDOWS)
					for (int i = 0; i < readAudioSamples; i++)
						dos.writeShort(dataFrame16bit[i]);
				
			}
			showToast("Done recording.");
			dos.close();
		} catch (Throwable t) {
			Log.e(APP_NAME,"Recording Failed");
		}
	}
	*/


	/**
	 * Takes the final compiled list of cepstral features and stores them in a csv file on the sdcard.
	 * Each frame is represented by its MFCCs (without energy). 
	 * 
	 * @param csvInput
	 */
	public void audioFeatures2csv(ArrayList<LinkedList<ArrayList<double[]>>> csvInput) {
		
		

		PrintWriter csvWriter;
		try
		{
		     
			//storage/emulated/0/Thesis/VoiceRecognizerSP/CSV/20MfccFeatures_1.csv
			File file = new File(SD_PATH + SD_FOLDER_PATH_CSV + File.separator + csvFileName);
			
			if(!file.exists())
				file = new File(SD_PATH + SD_FOLDER_PATH_CSV + File.separator + csvFileName);
			
			csvWriter = new  PrintWriter(new FileWriter(file,true));
			
			for(LinkedList<ArrayList<double[]>> linkedList : csvInput) {
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
			
			showToast("Features stored in csv file !");
			
			
          // code runs in a thread
          runOnUiThread(new Runnable() {
        	@Override
          	public void run() {
        	  	txtMessage.setText("Features successfully stored in csv file !");
          	}
          });
          
			
	    	Log.i(TAG, "MFCC audio2csv() done");
	    	
			//monitorCpuUsage();

	    	

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	private void repeatCycle()
	{
    		isRecording = false;//to skip synchronized if condition
    		
    		repeatRecordHandler.post(repeatRecordRunnable);
    		//repeatRecordHandler.postDelayed(repeatRecordRunnable, RECORDING_REPEAT_CYCLE);
    		
	    	Log.i(TAG, "MFCC ------------------------");

	    	Log.i(TAG, "MFCC repeatCycle() new repeat initiated ..");

    	
	}
	
	
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
	    	 if(isRecording)
	    		 Toast.makeText(this, "Switch not possible because recording is running !", Toast.LENGTH_SHORT).show();
	    	 else
	    	 {
	    		 if(fftType.equals("FFT_CT"))
	    		 {
	    			 fftType = "FFT_SP";
	    			 txtMessage.setText("MFCC with Superpowered");
	    			 
	    			 monitorBatteryUsage("AddExperiment");
	    			 monitorCpuAndMemoryUsage("AddExperiment");

	    		 }
	    		 else if(fftType.equals("FFT_SP"))
	    		 {
	    			 fftType = "FFT_CT";
	    			 txtMessage.setText("MFCC with Cooley-Tukey");
	    			 
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


	
}
