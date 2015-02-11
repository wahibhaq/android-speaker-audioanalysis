package tum.laser.voicerecognizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;

import tum.laser.javagmm.FFT;
import tum.laser.javagmm.FrequencyProperties;
import tum.laser.javagmm.MFCC;
import tum.laser.javagmm.PointList;
import tum.laser.javagmm.Window;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
public class MainActivity extends Activity {

	private static String APP_NAME = "VoiceRecognizer";

	private static int RECORDER_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
	private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static int RECORDER_SAMPLERATE = 16000; //16Hz frequency 

	private static int WINDOWS_TO_RECORD = 4;

	private static int FFT_SIZE = 512;
	//8000samples/s divided by 256samples/frame -> 32ms/frame (31.25ms)
	private static int FRAME_SIZE_IN_SAMPLES = 512;

	//32ms/frame times 64frames/window = 2s/window
	private static int WINDOW_SIZE_IN_FRAMES = 64;

	private static int MFCCS_VALUE = 20; //MFCCs count 
	private static int NUMBER_OF_FINAL_FEATURES = MFCCS_VALUE - 1; //discard energy
	private static int MEL_BANDS = 20; //use FB-20
	private static double[] FREQ_BANDEDGES = {50,250,500,1000,2000};

	private int bufferSize = 0;

	private static int[] freqBandIdx = null;
	public double[] featureBuffer = null;

	private Thread recordingThread = null;
	private FFT featureFFT = null;
	private MFCC featureMFCC = null;
	private Window featureWin = null;
	private AudioRecord audioRecorder = null;

	private boolean isRecording = false;
	
	private static int DROP_FIRST_X_WINDOWS = 1;
	
	//variabled added later by Wahib
	final double windowsToRead = 2.5; //5 sec audio file duration
	public static final File SD_PATH = Environment.getExternalStorageDirectory();
	public static final String SD_FOLDER_PATH_CSV = "/Thesis/VoiceRecognizer/CSV"; 
	public static final String SD_FOLDER_PATH_LOGS = "/Thesis/VoiceRecognizer/Logs"; 

	static Handler repeatRecordHandler = null;
	static Runnable repeatRecordRunnable = null;
	private final int RECORDING_REPEAT_CYCLE = 30000; //30 seconds
	static int cycleCount = 0;
	final int maxCycleCount = 250; //20 * 30 sec = 10 mins
	final String csvFileName = "20MfccFeatures_";
	final String batteryFileName = "battery_data.txt";
	final String TAG = "VoiceRecognizer";


	//TODO do this properly
	//GaussianMixture gmmFlo = null; //florian asked to comment
	//GaussianMixture gmmOther = null;
	//ImageView recognition = null;
	//ImageView admission = null;


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
		
		
		audioRecorder.startRecording();
		recordingThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				processAudioStream();
				
				//Switch s = (Switch) findViewById(R.id.switchAudio);
				
				//To remove the delay at beginning
				/*try {
					
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
				
				/*if (s.isChecked())
				{
					storeAudioStream();
				}
				else
					processAudioStream();
					*/
				
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
				isRecording = false;
		}

		Toast.makeText(getApplicationContext(), "Stopped recording. Resetting...", Toast.LENGTH_SHORT).show();

		audioRecorder.stop();
		audioRecorder.release();
		audioRecorder = null;
		recordingThread = null;
		
		repeatRecordHandler.removeCallbacks(repeatRecordRunnable);
    	Log.i(TAG, "MFCC stopRecording() released !");
		
	}

	@Override
	protected void onStop() {
		super.onStop();
		//stopRecording();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		repeatRecordHandler = new Handler();


		final Button buttonStart = (Button) findViewById(R.id.buttonStartRec);
		buttonStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				repeatRecordingAudio();
				
				/*
				repeatRecordHandler.postDelayed(new Runnable() {
				//new Handler().postDelayed(new Runnable() {
			        @Override
			        public void run() {
			            
			        	++cycleCount;
			        	Log.i(TAG, "MFCC cycle #" + cycleCount);
			        	recordAudioFromMic();
			        	
			        	
			        	//if(cycleCount < 5)
			        	//	repeatRecordingCycle.postDelayed(this, RECORDING_REPEAT_CYCLE);
			        	
			        }
			        	
			    }, RECORDING_REPEAT_CYCLE); //30 seconds
				*/
				
				//recordAudioFromMic(); //default
			}
		});

		final Button buttonStop = (Button) findViewById(R.id.buttonStopRec);
		buttonStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				isRecording = true; //for force stopping only
				stopRecording();
			}
		});
		
		/*
		if (gmmFlo == null) {
			gmmFlo = GaussianMixture.readGMM(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +"gmmFlo");
			gmmOther = GaussianMixture.readGMM(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +"gmmOther");
	
			Toast.makeText(getApplicationContext(), "GMMs loaded", Toast.LENGTH_SHORT).show();
		}
		
		
		recognition = (ImageView) findViewById(R.id.recognitionView);
		admission = (ImageView) findViewById(R.id.admissionView);
		*/
		
	}
	
	
	private void repeatRecordingAudio()
	{


		repeatRecordRunnable = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				++cycleCount;
	        	Log.i(TAG, "MFCC cycle #" + cycleCount);
	        	
	        	if(cycleCount == 1)
	        	{
	        		monitorBatteryUsage();
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
		
		repeatRecordHandler.postDelayed(repeatRecordRunnable, RECORDING_REPEAT_CYCLE);
	}
	
	//http://m2catalyst.com/tutorial-finding-cpu-usage-for-individual-android-apps/ 
	private void monitorCpuUsage()
	{
		ArrayList<String> list = new ArrayList<String>();
		
		Process p = null;
		try {
			
			p = Runtime.getRuntime().exec(new String[] { "sh", "-c", "top -n 1 | grep tum.laser.voicerecognizer" });
			
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
		
		String cpuUsageValue = "";
		
		//while(line != null)
		if(line != null)
		{
			Log.i(TAG, "MFCC output of top command : " + i + " : " + line);
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
		
		
	}
	
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
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
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
		featureFFT = new FFT(FFT_SIZE);
		featureMFCC = new MFCC(FFT_SIZE, MFCCS_VALUE, MEL_BANDS, RECORDER_SAMPLERATE);

		freqBandIdx = new int[FREQ_BANDEDGES.length];
		for (int i = 0; i < FREQ_BANDEDGES.length; i ++)
		{
			freqBandIdx[i] = Math.round((float)FREQ_BANDEDGES[i]*((float)FFT_SIZE/(float)RECORDER_SAMPLERATE));
		}

		//lists of windows
		LinkedList<ArrayList<double[]>> featureCepstrums = new LinkedList<ArrayList<double[]>>();
		LinkedList<ArrayList<double[]>> psdsAcrossFrequencyBands = new LinkedList<ArrayList<double[]>>();

		//windows: list of frames
		ArrayList<double[]> cepstrumWindow = new ArrayList<double[]>(WINDOW_SIZE_IN_FRAMES);
		ArrayList<double[]> psdWindow = new ArrayList<double[]>(WINDOW_SIZE_IN_FRAMES);

		int readAudioSamples = 0;
		int currentIteration = 0;

		
		//I have defined it as static final variable
		//String editText = ((EditText) findViewById(R.id.editTextSamples)).getText().toString();
		//int windowsToRead = (editText != null && !editText.isEmpty()) ? Integer.parseInt(editText) : WINDOWS_TO_RECORD;

		showToast("Starting to record...");
    	Log.i(TAG, "MFCC processAudioStream() Staring to Record !");
		//monitorCpuUsage();


		while (currentIteration < windowsToRead * WINDOW_SIZE_IN_FRAMES) //960 = 15*64 -> 15*2s=30s
		{

			// read() kann entweder mind. buffer_size/2 zeichen zur�ckliefern
			// (wir brauchen viel weniger) oder blockiert:
			// http://stackoverflow.com/questions/15804903/android-dev-audiorecord-without-blocking-or-threads
			synchronized (this) {
				if (isRecording)
				{
					readAudioSamples = audioRecorder.read(dataFrame16bit, 0, FRAME_SIZE_IN_SAMPLES);
					//monitorCpuUsage();
				}
				else
				{
					//we only get here in case the user kills us off early
					//remove last window as it's incomplete
					if (cepstrumWindow.size() > 0 && cepstrumWindow.size() < WINDOW_SIZE_IN_FRAMES)
					{
						featureCepstrums.removeLast();
					}
					   
					audioFeatures2csv(featureCepstrums, csvFileName + cycleCount + ".csv");
					return;
				}
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

				psdWindow = new ArrayList<double[]>(WINDOW_SIZE_IN_FRAMES);
				psdsAcrossFrequencyBands.add(psdWindow);
			}
			
			currentIteration++;
			
			// Add PSDs of this frame to our window
			psdWindow.add(freq.getPsdAcrossFrequencyBands());
			// Add MFCCs of this frame to our window
			cepstrumWindow.add(freq.getFeatureCepstrum());
			
			//if (cepstrumWindow.size() == WINDOW_SIZE_IN_FRAMES) //classify last window
			//	classifyWindow(cepstrumWindow);
			
			//monitorCpuUsage();

		}

		showToast("Done recording.");
    	Log.i(TAG, "MFCC processAudioStream() Done Recording !");
		//monitorCpuUsage();

		/*for (ArrayList<double[]> windows : featureCepstrums) {

		}*/

		
       audioFeatures2csv(featureCepstrums, csvFileName + cycleCount + ".csv");
    	

		return;
	}

	/*
	private void classifyWindow(ArrayList<double[]> cepstrumWindow) {
		PointList pl = new PointList(MFCCS_VALUE - 1);

		for (double[] frame : cepstrumWindow) {
			pl.add(frame);
		}

		double pFlo = gmmFlo.getLogLikelihood(pl);
		double pOther = gmmOther.getLogLikelihood(pl);

		//TODO
		if (pFlo > pOther)
			setRecognitionView(R.drawable.green_light);
		else
			setRecognitionView(R.drawable.red_light);
	}


	private void setRecognitionView(final int image) {
		runOnUiThread(new Runnable() 
		{                
			@Override
			public void run() 
			{
				recognition.setImageResource(image);
			}
		});
	}

	private void setAdmissionView(final int image) {
		runOnUiThread(new Runnable() 
		{                
			@Override
			public void run() 
			{
				recognition.setImageResource(image);
			}
		});
	}
	*/

	private void showToast(final String text) {
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
		
		double[] psdAcrossFrequencyBands = new double[FREQ_BANDEDGES.length - 1];
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

		// In-place FFT
		featureFFT.fft(fftBufferR, fftBufferI);
		
		if (dropIfBad && !isFrameAdmittedBySpectralEntropy(fftBufferR, fftBufferI))
			return null;

		// Get PSD across frequency band ranges
		for (int i = 0; i < (FREQ_BANDEDGES.length - 1); i ++)
		{
			int j = freqBandIdx[i];
			int k = freqBandIdx[i+1];
			double accum = 0;

			for (int h = j; h < k; h ++)
				accum += fftBufferR[h]*fftBufferR[h] + fftBufferI[h]*fftBufferI[h];

			psdAcrossFrequencyBands[i] = accum/((double)(k - j));
		}
		
		FrequencyProperties freq = new FrequencyProperties();
		
		freq.setFftImag(fftBufferI);
		freq.setFftReal(fftBufferR);
		freq.setFeatureCepstrum(featureCepstrum);
		freq.setPsdAcrossFrequencyBands(psdAcrossFrequencyBands);

		// Get MFCCs
		double[] featureCepstrumTemp = featureMFCC.cepstrum(fftBufferR, fftBufferI);

		// copy MFCCs
		for(int i = 1; i < featureCepstrumTemp.length; i++) {
			//only keep energy-independent features, drop first coefficient
			featureCepstrum[i-1] = featureCepstrumTemp[i];
		}
		
		return freq;
	}


	public double getRMS(short[] buffer) {
		double rms = 0;
		for (int i = 0; i < buffer.length; i++) {
			rms += buffer[i] * buffer[i];
		}
		return Math.sqrt(rms / buffer.length);
	}


	//TODO entropy for individual subbands
	//http://www.ee.uwa.edu.au/~roberto/research/theses/tr05-01.pdf
	//TODO double overflow? BigDecimal ok?

	/**
	 * Spectral entropy is calculated as
	 *
	 * 1) Normalize the power spectrum:
	 * Q(f)=P(f)/sum(P(f))  (where P(f) is the power spectrum)
	 * 
	 * 2) Transform with the Shannon function:
	 * H(f)=Q(f)[log(1/Q(f))]
	 * 
	 * 3) Spectral entropy:
	 * E=sum(H(f))/log(Nf)  (where Nf is the number of frequency components.
	 * 
	 * http://www.scielo.br/scielo.php?pid=S0034-70942004000300013&script=sci_arttext&tlng=en
	 * 
	 * @param fftReal
	 * @param fftImag
	 * @return
	 * 
	 * v1.1 Fixed "Non-terminating decimal expansion" error in divide()
	 *      by providing desired precision and rounding mode
	 */
	public static double getSpectralEntropy(double[] fftReal, double[] fftImag) {
		BigDecimal intensities[] = new BigDecimal[fftReal.length];
		BigDecimal sumOfIntensities = new BigDecimal(0d);
		double entropy = 0;
		
		try {
			//iterate over frequencies, compute intensities and overall sum of intensities
			for (int f = 0; f < fftReal.length; f++) {
				intensities[f] = new BigDecimal(fftReal[f]*fftReal[f] + fftImag[f]*fftImag[f]);
				sumOfIntensities = sumOfIntensities.add(intensities[f]);
			}
	
			for (BigDecimal intensity : intensities) {
				//normalize so that sum(p)=1 in order to get PMF
				BigDecimal p = intensity.divide(sumOfIntensities, 10, RoundingMode.HALF_UP);
				//only consider frequencies with p(f)>0
				if (p.compareTo(BigDecimal.ZERO) != 0) {
					entropy += p.doubleValue() * Math.log(p.doubleValue()) / Math.log(2);
				}
			}
			entropy *= -1;
			//optional: normalize
			entropy /= fftReal.length;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return entropy;
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

	public boolean isFrameAdmittedBySpectralEntropy(double[] fftReal, double[] fftImag) {
		//if (getSpectralEntropy(fftReal,fftImag) > 0.5)
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
	 * Takes a list of windows of frames and stores them in a csv file on the sdcard.
	 * Each frame is represented by its MFCCs (without energy). Legacy-ish because
	 * we now store raw audio on the sdcard and do the processing elsewhere.
	 * 
	 * @param featuresByFrameByWindow Lists of MFCCs
	 * @param filename Name of the csv file
	 */
	public void audioFeatures2csv(LinkedList<ArrayList<double[]>> featuresByFrameByWindow, String filename) {
		PointList pl = new PointList(NUMBER_OF_FINAL_FEATURES);

		PrintWriter csvWriter;
		try
		{
			//File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename);
			
			//storage/emulated/0/Thesis/VoiceRecognizer/CSV/20MfccFeatures_1.csv
			File file = new File(SD_PATH + SD_FOLDER_PATH_CSV + File.separator + filename);
			
			
			if(!file.exists())
				file = new File(SD_PATH + SD_FOLDER_PATH_CSV + File.separator + filename);
				//file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename);
			
			csvWriter = new  PrintWriter(new FileWriter(file,true));

			for (ArrayList<double[]> window : featuresByFrameByWindow) {
				for (double[] featuresByFrame : window) {
					pl.add(featuresByFrame);
					for (double d : featuresByFrame) {
						csvWriter.print(d + ",");
					}
					csvWriter.print("\r\n");
				}
			}

			csvWriter.close();
			
			showToast("Features stored in csv file !");
	    	Log.i(TAG, "MFCC audio2csv() done");
			//monitorCpuUsage();

	    	repeatRecordCycle();



		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	private void repeatRecordCycle()
	{
		if(cycleCount < maxCycleCount)
    	{
    		
    		isRecording = false;//to skip synchronized if condition
    		
    		repeatRecordHandler.postDelayed(repeatRecordRunnable, RECORDING_REPEAT_CYCLE);

    		//monitorCpuUsage();
    		monitorBatteryUsage();

    		
	    	Log.i(TAG, "MFCC ------------------------");

	    	Log.i(TAG, "MFCC repeatRecordCycle() new repeat initiated ..");

    	}
	}
	
	//http://examples.javacodegeeks.com/core-java/io/fileoutputstream/append-output-to-file-with-fileoutputstream/
	private void appendFile(String filename, String dataToAppend)
	{
		
		 ///File file = new File(Environment.getExternalStorageDirectory(), filename);
		 File direct = new File(SD_PATH + SD_FOLDER_PATH_LOGS);
		 File file = new File(SD_PATH + SD_FOLDER_PATH_LOGS + File.separator + filename);


	     if(!direct.exists())
	     {
	         direct.mkdir();
	     }        
	     
	     if (file.exists()) {
             try {
            	 
            	 /*FileOutputStream fOut = openFileOutput(file, MODE_APPEND);
                 OutputStreamWriter osw = new OutputStreamWriter(fOut);
                 osw.write(dataToAppend);
                 osw.flush();
                 osw.close();
                 */
            	 
                 
                 FileOutputStream fos = new FileOutputStream(file, true);
                 PrintWriter pw = new PrintWriter(fos);
                 pw.println(dataToAppend);
                 pw.flush();
                 pw.close();
                 
                 /*
                 FileWriter fw = new FileWriter(file, true);
				 fw.append(dataToAppend);
				 fw.append("\r\n");
				 fw.close();
				 */
					
                 
                
                // fos.close();
                 
                 
                 showToast("Battery data stored in text file");
     	    	Log.i(TAG, "MFCC battery data appended : " + dataToAppend);

             } catch (IOException e) {
                 e.printStackTrace();
             }}
	     
	     
		 /*
		 FileOutputStream fos;
		 byte[] data = jsonData.getBytes();
		 
		 try {
		     fos = new FileOutputStream(file);
		     fos.write(data);
		     fos.flush();
		     fos.close();
		 } catch (FileNotFoundException e) {
				Log.w("TAG", "Error saving json file: " + e.getMessage());
				
			} catch (IOException e) {
				Log.w("TAG", "Error saving json file: " + e.getMessage());
				
			}
			
			*/
	


	}

}
