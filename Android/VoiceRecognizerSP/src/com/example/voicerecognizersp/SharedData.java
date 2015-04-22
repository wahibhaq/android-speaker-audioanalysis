package com.example.voicerecognizersp;

import java.io.File;

import android.os.Environment;

/**
 * This class maintains all parameters which remain constant and shared within all classes 
 * 
 * @author Wahib-Ul-Haq
 *
 */
public class SharedData {
	
	
	public static String fftType = "FFT_SP"; //FFT_CT : Cooley-Tukey and other option is FFT_SP : Superpowered
    public static final String appProcessName = "com.example.voicerecognizersp";
	public final static String APP_NAME = "VoiceRecognizerSP";

		 
	public static final File SD_PATH = Environment.getExternalStorageDirectory();
	public static final String SD_FOLDER_PATH_PARENT = "/Thesis";
	public static String SD_FOLDER_PATH = "/Thesis/VoiceRecognizer";
	public static String SD_FOLDER_PATH_LOGS = SD_FOLDER_PATH + "/Logs"; 
	public static String SD_FOLDER_PATH_CSV = SD_FOLDER_PATH + "/CSV"; 

	public static final String csvFileName = "voicerecognizer_mfcc.csv"; 
	public static final String batteryFileName = "battery_data.txt";
	public static final String memcpuFileName = "memcpu_data.txt";
	public static final String cpuRealFileName = "cpu_real_usage_data.txt";
	
	public static final String vadCsvFileName = "voicerecognizer_mfcc_vad.csv"; 
	
	//////////Probing specific/////

	public static final int IDLE_INITIAL_DELAY = 0;
	public static final int IDLE_PROBE_DURATION = 6;//sec
	public static final int IDLE_WAIT_DURATION = 54;//sec
	
	public static final int SPEECH_WAIT_DURATION = 18;
	
	public static final int WINDOW_DURATION = 2;
	
	public static final int MIN_WINDOW_COUNT_SWITCH_TO_SPEECH = 0;
	public static final double MIN_WINDOW_SCORE_IF_SPEECH = 0.5;
	public static final int MIN_WINDOW_COUNT_SWITCH_TO_SPEAKER = 2;
	public static final int THRESHOLD_WINDOW_COUNT_SWITCH_BACK_TO_SPEECH = 2;
	public static final int THRESHOLD_WINDOW_COUNT_SWITCH_BACK_TO_IDLE = 6;




	 
	 
}
