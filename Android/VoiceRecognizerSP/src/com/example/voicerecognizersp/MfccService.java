package com.example.voicerecognizersp;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Trace;
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
	//private volatile ExecutorService recordingExecService = null;
	
	private volatile ScheduledExecutorService scheduleExecService = null;
	private static volatile ScheduledFuture<?> futureForSchedule = null;
	private static volatile Future<?> futureForSubmit = null;
	
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
	
	 //used because for idle mode window is 6s instead of 2s
	private static int WINDOW_SIZE_IN_FRAMES_FACTOR = 3;
	
	//32ms/frame times 64frames/window = 2s/window
	private static int WINDOW_SIZE_IN_FRAMES = 64 * WINDOW_SIZE_IN_FRAMES_FACTOR; 
	
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
	
	//2.5; //10;//10 = 20 seconds //2.5 = 5 seconds; //audio file duration in seconds
	//windowsToRead = 3 means 6 sec while loop.
	//for vad, stick to 1 so that one while loop == 1 window
	private static final double windowsToRead = 1; 
	
	private static final int OVERLAP_SIZE_IN_SAMPLES = 160;
	private static final int BUFFER_ITERATIONS_COUNT = 4;
	
	private static Runnable repeatRecordRunnable = null;//the one which actually repeats every cycle
	private static Runnable delayTask = null;
		
	static int cycleCount = 0;
	
	//Data monitoring specific
	private MonitoringData monitorOprObj;
    private FileOperations fileOprObj;
    
    //Vad specifc
    private VadManager vadOprObj;
    private DescriptiveStatistics vadPredictionList;
	private DecimalFormat doublePrecision = new DecimalFormat("#0.0");
	private static double windowScore = 0;
	private static int windowCount = 0;
	
	//Probing specific
	private static String mode = "idle";
	
	private static float speechPrediction = 0;
	private static float speakerPrediction = 0;
	
	private static boolean isIdle = true;
	private static boolean isSpeech = false;
	private static boolean isSpeaker = false;
	
	private static int windowAsSpeechCount = 0;
	private static CircularFifoBuffer speechWindowBuffer = null;
	
	//Simulation specific
	private Simulation simulateOprObj;
	
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
        
        vadOprObj = new VadManager(getApplicationContext());        //loads Vad model as well in the constructor 
        vadPredictionList = new DescriptiveStatistics();
		speechWindowBuffer = new CircularFifoBuffer(12);

        
        simulateOprObj = new Simulation();
        simulateOprObj.scenario2();//Setting Simulation Scenario
       
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
        
        //recordingExecService = Executors.newCachedThreadPool();     
        scheduleExecService = Executors.newScheduledThreadPool(5);
        
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
        	initStartAudioFromMic();
        	
        	

        	backgroundThread.start();
        }
    	
    }
    
    private void clean() {

    	//Cleaning threads and order matters.
    	

		//recordingExecService.shutdown();
    	scheduleExecService.shutdownNow();//shutdownNow() tries to kill the currently running tasks immediately

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
	protected void initStartAudioFromMic() {
		
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
		
		futureForSchedule = scheduleExecService.scheduleWithFixedDelay(repeatRecordRunnable, SharedData.IDLE_INITIAL_DELAY, SharedData.IDLE_WAIT_DURATION, TimeUnit.SECONDS);
		
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
		double[] frameFeatures = null;
		
    	Log.i(TAG, "Mfcc processAudioStream() Staring to Record !");
    	
    	
    	//analysing and extracting MFCC features in a 5 sec frame. WINDOW_SIZE_IN_FRAMES here refers to 2 sec
    	 //160 = 2.5 * 64 -> 2.5 * 2 sec => 5 sec 'while loop' with 2 windows running  
    	//64 = 1 * 64 -> 1 * 2 sec = 2 sec 'while loop' with 1 window running
    	//Long probe in Idle mode : 192 = 1 * (64*3) -> 1 * (2*3) sec = 6 sec 'while loop' with 1 long window of 6 sec running
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
			//if (cepstrumWindow.size() == WINDOW_SIZE_IN_FRAMES){
			if (cepstrumWindow.size() == WINDOW_SIZE_IN_FRAMES - 1){
				
				//Add this window to main collection of windows i.e featureCepstrumss
				cepstrumWindow = new ArrayList<double[]>(WINDOW_SIZE_IN_FRAMES);
				featureCepstrums.add(cepstrumWindow);
		    	
				//the function which considers current state, windowScore, windowCount and decides when to switch
				computeProbeSwitching();


			}
			
			currentIteration++;

			//TODO : change the count to bring to half
			if(currentIteration == WINDOW_SIZE_IN_FRAMES * 5) {
				monitorOprObj.dumpRealtimeCpuValues();//dumping at half for better evaluation
			}

	    	//Log.i(TAG, "MFCC iteration # : " + currentIteration);
			
			
			frameFeatures = freq.getFeatureCepstrum();

			// Add MFCCs of this frame to our window
			cepstrumWindow.add(frameFeatures);
		
			if(frameFeatures.length > 0 && !Double.isNaN(frameFeatures[0])) { //this is to skip initial values which have NaN values
				
				vadPredictionList.addValue(vadOprObj.executeRfVad(frameFeatures));
				
		    	//Log.i(TAG, "VAD output per Frame: " + vadOprObj.executeRfVad(freq.getFeatureCepstrum()));

			}

			
			freq = null;
			
	    	

		}//end of one window

    	Log.i(TAG, "MFCC processAudioStream() Done Recording !");

    	//add CSV data of all windows in this cycle
    	fileOprObj.appendToCsv(featureCepstrums);
    	

		

    	
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

	

	
	private void repeatCycle() {
    	
		//initiate repeat 
		futureForSubmit = scheduleExecService.submit(repeatRecordRunnable);

		
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
	
	
	
	//////////////////////Probing Manager//////////////////
	
	/**
	 * Calculates if the windowScore is good enough/enough probability to be considered as speech 
	 * @param avgScore from all frames in this particular window
	 */
	private void checkIfWindowIsSpeech(double avgScore) {
		

		if(avgScore >= SharedData.MIN_WINDOW_SCORE_IF_SPEECH) {
			
			speechWindowBuffer.add(1);
			//++windowAsSpeechCount;
		}
		else {
			speechWindowBuffer.add(0);
		}
		
		//just for testing
	    //Random randomGenerator = new Random();
		//speechWindowBuffer.add(randomGenerator.nextInt(1));
			

		
	}
	
	/**
	 * considers all the most recent scores of windows. score is either 1 (speech exists) or 0 (speech doesn't exist)
	 * @return count of windows which are considered true speech
	 */
	private int getCountOfTrueSpeech() {
		
		int countTrueSpeech = 0;
		for(Object element : speechWindowBuffer) {
			
			if((Integer)element == 1) {
				++countTrueSpeech;
			}
			
		}
		
		return countTrueSpeech;
	}
	
	private boolean shouldSwitchFromIdleToSpeech() {
				
		
		//if(windowAsSpeechCount > SharedData.MIN_WINDOW_COUNT_SWITCH_TO_SPEECH)
		/*if(getCountOfTrueSpeech() > SharedData.MIN_WINDOW_COUNT_SWITCH_TO_SPEECH)
			return true;
		else
			return false;*/
		
			
		
		return Simulation.SWITCH_FORWARD_FROM_IDLE_TO_SPEECH; //true; just for testing
	}
	
	
	
	private boolean shouldSwitchFromSpeechToSpeaker() {
		
		/*
		//if(windowAsSpeechCount >= SharedData.MIN_WINDOW_COUNT_SWITCH_TO_SPEAKER) {
		if(getCountOfTrueSpeech() >= SharedData.MIN_WINDOW_COUNT_SWITCH_TO_SPEAKER) {
			
			return true;
		}
		else {
			
			return false;
		}*/
		
		//Log.i(TAG, "Probing : shouldSpeechToSpeaker countOfTrueSpeech : " + getCountOfTrueSpeech());

		
		return Simulation.SWITCH_FORWARD_FROM_SPEECH_TO_SPEAKER; //true;//only for testing
		
	}
	
	private boolean shouldSwitchBackFromSpeechToIdle() {
		
		
		/*
		//if(windowAsSpeechCount < SharedData.THRESHOLD_WINDOW_COUNT_SWITCH_BACK_TO_IDLE) {
		if(getCountOfTrueSpeech() < SharedData.THRESHOLD_WINDOW_COUNT_SWITCH_BACK_TO_IDLE) {
			
			return true;
		}
		else {
			
			return false;
		}*/
		
		//Log.i(TAG, "Probing : shouldSpeechToIdle countOfTrueSpeech : " + getCountOfTrueSpeech());

		
		return Simulation.SWITCH_BACKWARD_FROM_SPEECH_TO_IDLE; //true; //only for testing
		
	}
	
	private void remainInSpeechMode() {
		
		windowCount = 0;
		//windowAsSpeechCount = 0;
		cycleCount = 0;
		
		repeatCycle();
		
		
	}
	
	private boolean shouldSwitchBackFromSpeakerToSpeech(){
		
		/*
		//if(windowAsSpeechCount < SharedData.THRESHOLD_WINDOW_COUNT_SWITCH_BACK_TO_SPEECH) {
		if( getCountOfTrueSpeech() < SharedData.THRESHOLD_WINDOW_COUNT_SWITCH_BACK_TO_SPEECH) {
			return true;
		}
		else {
			return false;
		}*/
		
		return Simulation.SWITCH_BACKWARD_FROM_SPEAKER_TO_SPEECH; //true; //only for testing
	}
	

	private void switchFromIdleToSpeech() {
		
		isSpeech = true;
		isIdle = false;
		isSpeaker = false;
		
		//resetting WINDOW_SIZE_IN_FRAMES
		WINDOW_SIZE_IN_FRAMES_FACTOR = 1;
		WINDOW_SIZE_IN_FRAMES = 64 * WINDOW_SIZE_IN_FRAMES_FACTOR; 

		windowCount = 0;
		//windowAsSpeechCount = 0;
		cycleCount = 0;
				
		if(futureForSchedule.cancel(true)) //cancels already running probe in idle mode and avoid going in waiting 
			futureForSubmit = scheduleExecService.submit(repeatRecordRunnable);//initiate probe for regular 2s windows 
		
	}
	
	private void switchFromSpeechToIdle() {
		
		isIdle = true;
		isSpeech = false;
		isSpeaker = false;
		
		//resetting WINDOW_SIZE_IN_FRAMES
		WINDOW_SIZE_IN_FRAMES_FACTOR = 3;
		WINDOW_SIZE_IN_FRAMES = 64 * WINDOW_SIZE_IN_FRAMES_FACTOR;  
		
		windowCount = 0;
		//windowAsSpeechCount = 0;
		cycleCount = 0;

		
		if(futureForSubmit.cancel(true))//cancels previous running idle mode task
			futureForSchedule = scheduleExecService.scheduleWithFixedDelay(repeatRecordRunnable, SharedData.IDLE_INITIAL_DELAY, SharedData.IDLE_WAIT_DURATION, TimeUnit.SECONDS);
		
		
	}
	
	public void switchFromSpeechToSpeaker() {
		
		isSpeaker = true;
		isSpeech = true;
		isIdle = false;
		
		//windowAsSpeechCount = 0;
		windowCount = 0;
		cycleCount = 0;
		
		if(futureForSubmit.cancel(true))
			futureForSubmit = scheduleExecService.submit(repeatRecordRunnable);//initiate probe for regular 2s windows

	}
		
	private void addWaitingDelayInSpeech() {
		
		futureForSchedule = scheduleExecService.schedule(repeatRecordRunnable, SharedData.SPEECH_WAIT_DURATION, TimeUnit.SECONDS);
		
	}
	
	private void switchBackFromSpeakerToSpeech() {
		
		isSpeech = true;
		isIdle = false;
		isSpeaker = false;
		
		//resetting WINDOW_SIZE_IN_FRAMES
		WINDOW_SIZE_IN_FRAMES_FACTOR = 1;
		WINDOW_SIZE_IN_FRAMES = 64 * WINDOW_SIZE_IN_FRAMES_FACTOR;  

		windowCount = 0;
		//windowAsSpeechCount = 0;
		cycleCount = 0;
		
		if(futureForSubmit.cancel(true))
			futureForSubmit = scheduleExecService.submit(repeatRecordRunnable);//initiate probe for regular 2s windows
	}
	
	
	
	
	
	
	private boolean isIdle() {
		
		if(isIdle == true && isSpeech == false && isSpeaker == false)
			return true;
		else
			return false;
	}
	
	private boolean isSpeech() {
	
		if(isIdle == false && isSpeech == true && isSpeaker == false)
			return true;
		else
			return false;
	}
	
	private boolean isSpeaker() {
		
		if(isIdle == false && isSpeech == true && isSpeaker == true)
			return true;
		else
			return false;
		
	}
	
	/**
	 * Chec if switching of mode needs to be done on the basis of windowCount, windowScore and/or on the basis of scenario
	 * in {@link Simulation}.
	 */
	private void computeProbeSwitching() {
		
		
		//calculates average of all outputs of frames to give score for 1 window
		windowScore = Double.valueOf(doublePrecision.format(vadPredictionList.getMean()));
		Log.i(TAG, "Window Score : " + windowScore);
		
		++windowCount;//counter used to detect when to evaluate for switching
		
		//checks from most recent windowScore entries and increments counter for true speech instances
		checkIfWindowIsSpeech(windowScore); 
		
		
		if(isIdle()) {
			//checking here coz in Idle mode, 1 window is 1 probing cycle
			//comes here after 6 sec of execution 
			
			Log.i(TAG, "Probing : Idle mode & windowCount : " + windowCount);

			if(shouldSwitchFromIdleToSpeech()) {
				
				switchFromIdleToSpeech();
				Log.i(TAG, "Probing : ---> Switching from Idle to Speech now");


			}
			else {
				
				//no need to change the probe state coz alredy in idle mode
				Log.i(TAG, "Probing : Waiting time in idle mode ..");
				

				
			}
		}
		else if(isSpeech()) {
			//Running Speech Mode
			Log.i(TAG, "Probing : Speech mode & windowCount : " + windowCount);

			
			if(windowCount == 6) {
				
				//to check from out of 6 windows
				Log.i(TAG, "Probing : Speech mode & checking out of 6 windows");

				if(shouldSwitchFromSpeechToSpeaker()) {
					switchFromSpeechToSpeaker();
					Log.i(TAG, "Probing : --> Switching from Speech To Speaker");

				}
				else {
					Log.i(TAG, "Probing : Add waiting delay in Speech mode");
					addWaitingDelayInSpeech();

				}
			}
			else if(windowCount == 12) {
				
				//to check from out of 12 windows
				Log.i(TAG, "Probing : Speech mode & checking out of 12 windows");

				if(shouldSwitchBackFromSpeechToIdle()) {
					switchFromSpeechToIdle();
					Log.i(TAG, "Probing : --> Switching back from Speech To Idle");

				}
				else {
					remainInSpeechMode();
				}
			}
			else {
				Log.i(TAG, "Probing : Speech mode and running ..");
				repeatCycle();
			}
				
		}
		else if(isSpeaker()) {
			//Running Speaker mode
			Log.i(TAG, "Probing : Speaker mode & windowCount : " + windowCount);
			
			if(windowCount == 30) {
				
				//to check from out of 30 windows
				Log.i(TAG, "Probing : Speaker mode & checking out of 30 windows");
				
				if(shouldSwitchBackFromSpeakerToSpeech()) {
					switchBackFromSpeakerToSpeech();
					Log.i(TAG, "Probing : --> Switching back from Speaker To Speech");

				}
				else {
					Log.i(TAG, "Probing : Continuing with Speaker mode ..");
					windowCount = 0;
					repeatCycle();

				}

			}
			else {
				Log.i(TAG, "Probing : Speaker mode and running ..");
				repeatCycle();
			}
			
			
		}
	}
	
	

    

}//class ends here
