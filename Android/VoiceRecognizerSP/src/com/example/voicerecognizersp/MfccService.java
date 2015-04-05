package com.example.voicerecognizersp;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Trace;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.javagmm.FFT;
import com.example.javagmm.FrequencyProperties;
import com.example.javagmm.MFCC;
import com.example.javagmm.Window;

/**
 * The background service which performs the main audio recording, feature extraction and monitoring
 * operations. Service also shows notification in the status bar and the current mode. Service is 
 * binded with Activity {@link BindingActivity}
 * 
 * @author Wahib-Ul-Haq 
 * Mar 22, 2015
 *
 */
public class MfccService extends Service {

	private boolean mStop = false; 
	
	//why volatime ? http://tutorials.jenkov.com/java-concurrency/volatile.html
	private volatile Thread backgroundThread = null;
	private volatile ExecutorService recordingExecService = null;
	
	private volatile ScheduledThreadPoolExecutor scheduleExec = null;
	

	
	///////////Service parameters/////////////
		
	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();
	
	
	LocalBroadcastManager broadcaster;
	static final public String COPA_RESULT = "com.example.voicerecognizersp.MfccService.REQUEST_PROCESSED";
	static final public String COPA_MESSAGE = "UINotification";
			
	 
	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private static final int NOTIFICATION_ID = 123;
    private NotificationManager notifManager;
    private static String notificationConText = "idle mode";
    
	final static String TAG = "MfccService"; 

	
	///////////////////////
	
//////////Voice Recognizer specific ///////////
	
	private static final int RECORDER_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static final int RECORDER_SAMPLERATE = 16000; //16Hz frequency 
	
	//private static int WINDOWS_TO_RECORD = 4;
	
	private static final int FFT_SIZE = 512; //512 = default;
	//8000samples/s divided by 256samples/frame -> 32ms/frame (31.25ms)
	//now its 16000 samples/s divided by 512 samples.frame but still -> 32ms/frame (31.25ms)
	private static final int FRAME_SIZE_IN_SAMPLES = 512; //512 = default value;
	
	//32ms/frame times 64frames/window = 2s/window
	private static final int WINDOW_SIZE_IN_FRAMES = 64;
	
	private static final int MFCCS_VALUE = 19; //MFCCs count 
	private static final int NUMBER_OF_FINAL_FEATURES = MFCCS_VALUE - 1; //discard energy
	private static final int MEL_BANDS = 20; //use FB-20
	private static final double[] FREQ_BANDEDGES = {50,250,500,1000,2000};
	
	private int bufferSize = 0;
	
	private static int[] freqBandIdx = null;
	public double[] featureBuffer = null;
	
	private FFT featureFFT = null; //not using now and instead using FFT from Superpowered Sdk
	private MFCC featureMFCC = null;
	private Window featureWin = null;
	private AudioRecord audioRecorder = null;
	
	public static boolean isRecording = false;
	
	private static final int DROP_FIRST_X_WINDOWS = 1;
	
	//////////////////variabled added later by Wahib//////////////
	private static final double windowsToRead = 2.5; //10;//10 = 20 seconds //2.5 = 5 seconds; //audio file duration in seconds
	private static final int OVERLAP_SIZE_IN_SAMPLES = 160;
	private static final int BUFFER_ITERATIONS_COUNT = 4;
	
	private static Runnable repeatRecordRunnable = null;//the one which actually repeats every cycle
	private static Runnable delayTask = null;
		
	//private final int RECORDING_REPEAT_CYCLE = 10000; //1000 = 10 seconds
	static int cycleCount = 0;
	//final int maxCycleCount = 100; //use it to set total time to run; total time (sec) = maxCycleCount * RECORDING_REPEAT_CYCLE
		
	MonitoringData monitorOprObj;
    FileOperations fileOprObj;
    
    VadManager vadOprObj;
    DescriptiveStatistics vadPredictionList;
	DecimalFormat doublePrecision = new DecimalFormat("#0.0");

		///////////
	
    public MfccService() {
        Log.i(TAG, "constructor done");

    }
	
		
	/////////////////Superpowered specifc/////////
		
	public static final int logSize = 8; //9; //2^9 = 512 = FFTSize
	public static final boolean ifReal = true;  //We are using FFT-in-Place 
	public static final boolean ifForward = false;
		
	public static String fftType = "FFT_SP"; //FFT_CT : Cooley-Tukey and other option is FFT_SP : Superpowered
	
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
	
	 @Override
    public void onCreate() {
        Log.i(TAG, "onCreate called");

        init();
        
        //registerCallSmsReceiver();

              
    }
	 
	/* private void registerCallSmsReceiver() {
		 
		 IntentFilter filter = new IntentFilter();
		 filter.addAction("android.provider.Telephony.SMS_RECEIVED");
		 //filter.addAction(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		 //filter.addAction("your_action_strings"); //further more
		 //filter.addAction("your_action_strings"); //further more

		 filter.setPriority(Integer.MAX_VALUE);
		 
		 registerReceiver(receiver, filter);
	 }*/
	 
	 /**
	  * Handles of all necessary initializations
	  */
	 private boolean init() {

		 
        //broadcaster = LocalBroadcastManager.getInstance(this);

        fileOprObj = new FileOperations();
        monitorOprObj = new MonitoringData(this, fileOprObj);
        
        //loads Vad model as well in the constructor 
        vadOprObj = new VadManager(getApplicationContext());
        vadPredictionList = new DescriptiveStatistics();
        
       
        notifManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        synchronized (this) {
			if (backgroundThread == null) {
				
		        backgroundThread = new Thread(new Runnable() {
					//For the resource intensive tasks
		        	
					@Override
					public void run() {
						// TODO Auto-generated method stub
				        Log.i(TAG, "onCreate thread running..");
				        handleRecordingAudio();
				        updateNotification("extraction mode");
					}
				});
			}
        }
        
        Log.i(TAG, "init()");
        
        recordingExecService = Executors.newCachedThreadPool();
        
        //scheduleExec = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
        //long period
        
        return true;


	 }
	 
	 
	 /**
     * Show a notification while this service is running and run as foreground so
     * that OS knows that Activity depends on service and service is not a candidate
     * to be killed
     */
    //http://stackoverflow.com/a/28144499/1016544
    private void runAsForeground() {

    	startForeground(NOTIFICATION_ID, getMyNotification());//"normal mode"));
	}
    
	
	private Notification getMyNotification(){
		
	        // The PendingIntent to launch our activity if the user selects
	        // this notification
	    	Intent notificationIntent = new Intent(this, BindingActivity.class);
	        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,  notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);//Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);//
	
	        return new Notification.Builder(this)
	                .setContentTitle(SharedData.APP_NAME)
	                .setContentText(notificationConText)
	                .setSmallIcon(R.drawable.ic_launcher)
	                .setContentIntent(pendingIntent).build(); 
	}
	/**
	this is the method that can be called to update the Notification
	*/
	private void updateNotification(String text) {
		
					notificationConText = text;
	                Notification notification = getMyNotification();
	
	                notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	                notifManager.notify(NOTIFICATION_ID, notification);
	}
	 

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	
        
    	int rc = super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "onStartCommand Received start id " + startId + ": " + intent + "rc : " + rc );
        
        runAsForeground();

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
        //return START_REDELIVER_INTENT;
        
        //return Service.START_REDELIVER_INTENT;

    }

    @Override
    public void onDestroy() {
    	
    	super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        
        clean();
        
       // unregisterReceiver(receiver);
        
        notifManager.cancel(NOTIFICATION_ID);
        Log.i(TAG, "Cencelling notification");
        
        stopForeground(true);

    }
	
	
	/**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
    	
        MfccService getService() {
            Log.d(TAG, "getService done");

            // Return this instance of LocalService so clients can call public methods
            return MfccService.this;
            
            
        }
    }

    
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind called");
        

        return mBinder;
        //return null;
    }
   
    
    //http://stackoverflow.com/a/9092277/1016544
    /*private final BroadcastReceiver receiver = new BroadcastReceiver() {
    	   @Override
    	   public void onReceive(Context context, Intent intent) {
 	    	  Log.i(TAG, "SMS onReceive()");

    	      String action = intent.getAction();
    	      if(action.equals("android.provider.Telephony.SMS_RECEIVED")){
    	        //action for sms received
    	    	 
    	    	  Log.i(TAG, "SMS Received");
    	    	  
    	      }
    	      else if(action.equals(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED)){
    	           //action for phone state changed
    	    	  
    	    	  Log.i(TAG, "Phone State Changed");

    	      }     
    	   }
    	}; */
    
    /////////////////////MFCC Operations to run in bacground////////////////

    
    /**
     * FFT types are two : Cooley Turkey (CT) and Super Powered (SP)
     * @param type
     */
    public void setFFTType(String type)
    {
    	fftType = type;
    }
    
    
    public void startMfccExtraction()
    {
    	
        if(init())
        {
        	recordAudioFromMic();

        	backgroundThread.start();
        }
    	
    }
    
    private void clean() {

    	//Cleaning threads and order matters.
    	

		recordingExecService.shutdown();		

    	if(audioRecorder != null) {
			audioRecorder.stop();
			audioRecorder.release();
			
			if(audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
				audioRecorder = null;
			
    	}

        if(backgroundThread != null) {
	        Thread dummy = backgroundThread;
	        backgroundThread = null;
	        dummy.interrupt();
    	}
        
        

    	Log.i(TAG, "clean() released !");
    	

    }
    
       
    
    
    public boolean isRecording()
    {
    	return isRecording;
    }
	/**
	 * Start recording audio in a separate thread.
	 * If the switch in our main view is checked, the audio will be saved in
	 * a file {@link #storeAudioStream()}. Otherwise we process the stream
	 * immediately {@link #processAudioStream()}.
	 */
	protected void recordAudioFromMic() {
		
		synchronized (this) {
			if (isRecording())
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
		
		
		
	}


	/**
	 * Stop recording. Kills off any ongoing recording.
	 */
	protected void stopRecording() {
		
		synchronized (this) {
			if (!isRecording())
				return;
			else
			{
				
				
				if(audioRecorder != null)
				{
					isRecording = false;

					//sendMsgToActivity("Stopped recording & Resetting");
			    	updateNotification("idle mode");

					clean();

					
					cycleCount=0;

			    	
				}
				else
				{
					isRecording = false;
					
			    	Log.i(TAG, "already in stopped state !");


				}

			}
		}

		
		
	}
	
	/*private void handleProbeDelay() {
		
		delayTask = new Runnable(){
            @Override
            public void run() {
                try{
                    System.out.println("\t delayTask Execution Time: " + fmt.format(new Date()));
                    Thread.sleep(10 * 1000);
                    System.out.println("\t delayTask End Time: "
                            + fmt.format(new Date()));
                }catch(Exception e){
                     
                }
            }
        };
	}*/
	
	private void handleRecordingAudio() {


		repeatRecordRunnable = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				// Moves the current Thread into the background. 
				//This approach reduces resource competition between the Runnable object's thread and the UI thread. 
		        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		        
		        
				++cycleCount;
	        	Log.i(TAG, "handleaudio - cycle #" + cycleCount);
	        	
	        	isRecording = true;
        		processAudioStream();
			
	        	
			}
		};
		
		recordingExecService.submit(repeatRecordRunnable);
		
	}
	


	/*@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		
		Log.i(TAG, "onTripMemory Level : " + level);
		
	}*/
	
	
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
	 * Updated by Wahib on 03/03/2015
	 * Decoupling of audioRecord.read() and sending to processAudioFrame().
	 * CircularFifoBuffer is introduced which maintins the buffer and when filled then
	 * only sent for processing. Reason for doing this was to introduce overlapping of
	 * samples for better efficiency.
	 * 
	 * v1.1
	 */
	private void processAudioStream()
	{
		
		///newly added ///
		
		short dataFrame16bit_one[] = new short[OVERLAP_SIZE_IN_SAMPLES]; //160
		short dataFrame16bit_two[] = new short[OVERLAP_SIZE_IN_SAMPLES]; //160
		short dataFrame16bit_three[] = new short[OVERLAP_SIZE_IN_SAMPLES]; //160
		short dataFrame16bit_four[] = new short[OVERLAP_SIZE_IN_SAMPLES]; //160

		
		
		int dataFrameCount = 0;
		
		//https://commons.apache.org/proper/commons-collections/javadocs/api-3.2.1/org/apache/commons/collections/buffer/CircularFifoBuffer.html
		CircularFifoBuffer buffer = new CircularFifoBuffer(BUFFER_ITERATIONS_COUNT);
		///////////////
		
		
		
		//initialize general processing data
		featureWin = new Window(FRAME_SIZE_IN_SAMPLES); //smoothing window, nothing to do with frame window
		
		//choising FFT technology
		if(fftType.equals("FFT_CT"))
			featureFFT = new FFT(FFT_SIZE);
		
		featureMFCC = new MFCC(FFT_SIZE, MFCCS_VALUE, MEL_BANDS, RECORDER_SAMPLERATE);

		freqBandIdx = new int[FREQ_BANDEDGES.length];
		int freqBandLen = FREQ_BANDEDGES.length;
		for (int i = 0; i < freqBandLen; i ++)
		{
			freqBandIdx[i] = Math.round((float)FREQ_BANDEDGES[i]*((float)FFT_SIZE/(float)RECORDER_SAMPLERATE));
		}

		//lists of windows
		LinkedList<ArrayList<double[]>> featureCepstrums = new LinkedList<ArrayList<double[]>>();

		//windows: list of frames
		ArrayList<double[]> cepstrumWindow = new ArrayList<double[]>(WINDOW_SIZE_IN_FRAMES);

		int readAudioSamples = 0;
		int currentIteration = 0;
		
		
		FrequencyProperties freq = null;
		
    	Log.i(TAG, "Mfcc processAudioStream() Staring to Record !");
    	
    	
    	//analysing and extracting MFCC features in a 5 sec frame. WINDOW_SIZE_IN_FRAMES here refers to 2 sec
    	 //160 = 2.5 * 64 -> 2.5 * 2 sec => 5 sec 'while loop' with 2 windows running  
		while (currentIteration < windowsToRead * WINDOW_SIZE_IN_FRAMES)
		{
			
			// read() kann entweder mind. buffer_size/2 zeichen zur�ckliefern
			// (wir brauchen viel weniger) oder blockiert:
			// http://stackoverflow.com/questions/15804903/android-dev-audiorecord-without-blocking-or-threads
			
			try 
			{
				synchronized (this) {
					if (isRecording())
					{
						if(audioRecorder != null) {						
						
								//to keep a check when buffer is full and needs to be send to process
							++dataFrameCount;
							
							//the only reason to have 4 different dataFrame arrays is that buffer uses pass by reference so
							//data was not actually changed in buffer even if dataFrame16bit content was changed 
							if(dataFrameCount == 1)
							{
								readAudioSamples = audioRecorder.read(dataFrame16bit_one, 0, OVERLAP_SIZE_IN_SAMPLES);
								buffer.add(dataFrame16bit_one);
							}
							else if(dataFrameCount == 2)
							{
								readAudioSamples = audioRecorder.read(dataFrame16bit_two, 0, OVERLAP_SIZE_IN_SAMPLES);
								buffer.add(dataFrame16bit_two);
							}
							else if(dataFrameCount== 3)
							{
								readAudioSamples = audioRecorder.read(dataFrame16bit_three, 0, OVERLAP_SIZE_IN_SAMPLES);
								buffer.add(dataFrame16bit_three);
							}
							else if(dataFrameCount == 4)
							{
								readAudioSamples = audioRecorder.read(dataFrame16bit_four, 0, OVERLAP_SIZE_IN_SAMPLES);
								buffer.add(dataFrame16bit_four);
							}
							
							//Log.i(TAG, "MFCC circular buffer : " + dataFrameCount);
						}
						
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
										    	
				    	
						return;
					}
				}
			}
			catch (Exception e) {
				
				e.printStackTrace();
			}

			if (readAudioSamples == 0)
				return;
			
			Object bufferArray[] = buffer.toArray();
			if(!isFrameAdmittedByRMS((short[])bufferArray[dataFrameCount-1]))
				continue; //skips rest of the code in loop

			//means 512 samples count is achieved and now its ready for processing
			if(dataFrameCount == BUFFER_ITERATIONS_COUNT )
			{
				freq = processAudioFrame(FRAME_SIZE_IN_SAMPLES, buffer, true);
				
				dataFrameCount = 0;//resetting count
				
			}
			
			if (freq == null)
				continue; //skips rest of the code in loop
			
			//combine WINDOW_SIZE_IN_FRAMES frames to a window
			//2s with a window size of 64
			if (cepstrumWindow.size() == WINDOW_SIZE_IN_FRAMES){
				
				//Add this window to main collection of windows i.e featureCepstrumss
				cepstrumWindow = new ArrayList<double[]>(WINDOW_SIZE_IN_FRAMES);
				featureCepstrums.add(cepstrumWindow);
		    	
				//Log.i(TAG, "MFCC currentInteration# when adding window to list : " + currentIteration);
				
				//TODO : decide what to do with vad when window is completed
				//Double.valueOf(doublePrecision.format(vadPredictionList.getMean()));

			}
			
			currentIteration++;

		
			// Add MFCCs of this frame to our window
			cepstrumWindow.add(freq.getFeatureCepstrum());
			
			if(currentIteration == WINDOW_SIZE_IN_FRAMES * 5) {
				monitorOprObj.dumpRealtimeCpuValues();//dumping at half for better evaluation
			}

	    	//Log.i(TAG, "MFCC iteration # : " + currentIteration);
			
			//TODO : call vad function here and save output because this is where a frame gets complete. 
			//vadPredictionList.add(vadOprObj.executeRfVad());
			
			double[] frameFeatures = freq.getFeatureCepstrum();
			//int vadPredPerFrame = 0;
			
			if(!Double.isNaN(frameFeatures[0])) { //this is to skip initial values which have NaN values
				//vadPredPerFrame = vadOprObj.executeRfVad(freq.getFeatureCepstrum());
				
				vadPredictionList.addValue(vadOprObj.executeRfVad(freq.getFeatureCepstrum()));
				
		    	//Log.i(TAG, "VAD output : " + vadPredPerFrame);

			}

			
			freq = null;
			
			//break;//toremove
	    	

		}//end of one window

    	Log.i(TAG, "MFCC processAudioStream() Done Recording !");

    	//add CSV data of all windows in this cycle
    	fileOprObj.appendToCsv(featureCepstrums);
    	
    	repeatCycle();
		

    	
    	///clearing up the most populated arrays/lists
    	featureCepstrums.clear();
    	featureCepstrums = null;
    	cepstrumWindow.clear();
    	cepstrumWindow = null;

		return;
	}
	
	



	/**
	 * Takes an audio frame and processes it in the frequency domain:
	 * 1. Applies a Hamming smoothing window
	 * 2. Computes the FFT
	 * 3. Computes the Power Spectral Density for each frequency band
	 * 4. Computes the MFCC coefficients
	 * 
	 * updated by Wahib on 03/03/2015
	 * Accessing CircularBufferFifo and filling the main data array with contents
	 * of buffer so that it can be used like before
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
	 * v1.1
	 */
	public FrequencyProperties processAudioFrame(int samples, CircularFifoBuffer buffer, boolean dropIfBad) {
		
		double fftBufferR[] = new double[FFT_SIZE];
		double fftBufferI[] = new double[FFT_SIZE];
	
		double[] featureCepstrum = new double[NUMBER_OF_FINAL_FEATURES];
		
		short dataFrame16bit[] = new short[samples];
		
		////////newly added///////
		Object bufferArray[] = buffer.toArray();
		int count = 0;
		short[] temp;
		
		for(int i=0; i<buffer.size(); i++)
		{
			if(i != buffer.size() - 1)
			{
				//except last one
				temp = (short[]) bufferArray[i];
			}
			else
			{
				//last one
				int maxForLast = FRAME_SIZE_IN_SAMPLES - (3 * OVERLAP_SIZE_IN_SAMPLES) ; 
				temp = Arrays.copyOfRange((short[]) bufferArray[i], 0, maxForLast);

			}
				
			
			
			System.arraycopy(temp, 0, dataFrame16bit, count, temp.length);
			
			count += OVERLAP_SIZE_IN_SAMPLES;
			
		}
		///////////////////
		
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

		
		if(getAndroidVersion() >= 18)
		{
			//Android 4.3 (API level 18) or higher
			
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
		}
		else
		{
			//Android 4.2 (API level 17) or lower
			if(fftType.equals("FFT_SP"))
        	{
        		// In-place FFT - this is what we have to use as instructed by Florian
        		onFFTReal(convertDoublesToFloats(fftBufferR), convertDoublesToFloats(fftBufferI), logSize, ifForward);
        	}
        	else if(fftType.equals("FFT_CT"))
        		featureFFT.fft(fftBufferR, fftBufferI);

		}

		
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

	
	/**
	 * Fetches OS version of the phone and returns the sdk version
	 * @return
	 */
	public int getAndroidVersion() {
	    //String release = Build.VERSION.RELEASE;
	    int sdkVersion = Build.VERSION.SDK_INT;
	    //return "Android SDK: " + sdkVersion + " (" + release +")";
	    return sdkVersion;
	}

	public static float[] convertDoublesToFloats(double[] input)
	{
	    if (input == null)
	    {
	        return null; // Or throw an exception - your choice
	    }
	    
	    float[] output = new float[input.length];
	    
	    int inputLen = input.length; 
	    for (int i = 0; i < inputLen; i++)
	    {
	        output[i] = (float) input[i];
	    }
	    return output;
	}
	
	public double getRMS(short[] buffer) {
		double rms = 0;
	    int bufferLen = buffer.length;
	    
		for (int i = 0; i < bufferLen; i++) {
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

	

	
	private void repeatCycle()
	{
    	
		//TODO : compute average of vad function output here because this is where cycle gets complete
		//Double.valueOf(doublePrecision.format(vadPredictionList.getMean()));

		
		//initiate repeat 
		recordingExecService.submit(repeatRecordRunnable);


		
    	Log.i(TAG, "------------------------");

    	Log.i(TAG, "repeatCycle() new repeat initiated ..");

    	//Log.i(TAG, "MFCC repeatCycle() cpu usage value : " + monitorOprObj.getCpuUsage());
    	
    	monitorOprObj.dumpRealtimeCpuValues();
    	



	}
	

	
	
	/*public void sendMsgToActivity(String message) {
	    Intent broadcastIntent = new Intent();
	    broadcastIntent.setAction(BindingActivity.ResponseReceiver.LOCAL_ACTION);
	    
	    if(message != null)
	        broadcastIntent.putExtra(COPA_MESSAGE, message);
	    
	    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
	    localBroadcastManager.sendBroadcast(broadcastIntent);
	}*/
	
    

    

}//class ends here
