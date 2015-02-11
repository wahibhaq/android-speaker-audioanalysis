package com.example.tarsosaudioproject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import android.support.v4.content.LocalBroadcastManager;

public class RecordingMfccService extends Service {
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    
    // Random number generator
    private final Random mGenerator = new Random();
    private String TAG = "MFCCService";
    

    
    //Tarsos Parameters//
    private AudioDispatcher dispatcher = null;
	final double endTime = 20.0;
	
	static int mfccIndex = 0;
	ArrayList<float[]> mfccList;
	
	LocalBroadcastManager broadcaster;
    static final public String COPA_RESULT = "com.example.tarsosaudioproject.RecordingMfccService.REQUEST_PROCESSED";
    static final public String COPA_MESSAGE = "UINotification";

    Handler handler;
	String uiMessage = "";

	//MFCC attributes
	final int samplesPerFrame = 512;
	final int sampleRate = 16000;
	final int amountOfCepstrumCoef = 19; //actually 18 but energy column would be discarded
	int amountOfMelFilters = 30; 
	float lowerFilterFreq = 133.3334f;
	float upperFilterFreq = ((float)sampleRate)/2f;
    
    public RecordingMfccService() {
        Log.d(TAG, "constructor done");

    }
    
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate called");
        
        //handler= new Handler();
        broadcaster = LocalBroadcastManager.getInstance(this);
        

    }
    
    /*
    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }
    */
    
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        RecordingMfccService getService() {
            Log.d(TAG, "getService done");

            // Return this instance of LocalService so clients can call public methods
            return RecordingMfccService.this;
        }
    }

    
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind done");
        
        //initilizeMic
        
        return mBinder;
    }
    
    
    
   
    /*
    @Override
    public void onDestroy() {
    	
    	Log.i(TAG, "Destroying Service ..");
    	
    	if(dispatcher != null)
    	{
	    	dispatcher.stop();
	    	dispatcher = null;
    	}
    }
    */
    
    /**
     * methods for handling TarsosDSP
     */
    
    public void initDispatcher()
    {
        Log.d(TAG, "initDispatcher done");
		mfccList = new ArrayList<float[]>();

    	//sampleRate, audioBufferSize, int bufferOverlap 
		//Florian suggested to use 16kHz as sample rate 
		dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(16000, 1024 ,0); //(22050,1024,0);
		
    }
    
    public boolean isDispatcherNull()
    {
    	if(dispatcher == null)
    		return true;
    	else
    		return false;
    }
    
    public void stopDispatcher()
    {
    	dispatcher.stop();
		dispatcher = null;

    }
    
  //after getting feedback from owner Joren Six in email
  	public void startMfccExtraction()
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
  				
  				//fetchng MFCC array and removing the 0th index because its energy coefficient and florian asked to discard
  				float[] mfccOutput = mfccObj.getMFCC();
  				mfccOutput = Arrays.copyOfRange(mfccOutput, 1, mfccOutput.length);
  	
  				//Storing in global arraylist so that i can easily transform it into csv
  				mfccList.add(mfccOutput);
  				Log.i("MFCC", String.valueOf(Arrays.toString(mfccOutput)));
  				
  				
  				return true;
  			}
  		});
  		

  		//its better to use thread vs asynctask here. ref : http://stackoverflow.com/a/18480297/1016544
  		new Thread(dispatcher, "Audio Dispatcher").start();

  		

  	}
  	
  	public ArrayList<float[]> getMfccList()
  	{
  		return mfccList;
  	}
  	
  	
  	public void startPitchDetection()
	{
        Log.d(TAG, "startPitchDetection");

		//algorithm, sampleRate, bufferSize, handler
		dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN, 16000, 1024, new PitchDetectionHandler() {
			
			@Override
			public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
				
				//-1 means no sound 
				final float pitchInHz = pitchDetectionResult.getPitch();
				//Log.i("Pitch", String.valueOf(pitchInHz));
				
				if(pitchInHz == -1)
		    		sendResult("Silent");
		    	else
		    		sendResult("Speaking");
				
				
				//call showPitchOnUI(pitchInHz) 
				/*runOnUiThread(new Runnable() {
				     @Override
				     public void run() {
				    	
				    	 
				    	 
				    	if(pitchInHz == -1)
				    		uiMessage = "Silent";
				    	else
				    		uiMessage = "Speaking";
				    }
				});
				
				*/
				
			}
		}));
		

		
	}
  	

	public void sendResult(String message) {
	    Intent intent = new Intent(COPA_RESULT);
	    
	    if(message != null)
	        intent.putExtra(COPA_MESSAGE, message);
	    
	    broadcaster.sendBroadcast(intent);
	}
    
    /** method for clients */
    public int getRandomNumber() {
      return methodTwo();
    }
    
 // Methods used by the binding client components
    
    public boolean methodOne() {
        // Some code...
    	return mGenerator.nextBoolean();
    }
     
    public int methodTwo() {
        // Some code...
    	return mGenerator.nextInt(100);
    }
    
    
    
}