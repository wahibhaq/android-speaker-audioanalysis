package com.example.voicerecognizersp;

import java.io.File;

import android.os.Environment;

/**
 * This class maintains all memors which remain mostly constant and shared within all classes 
 * 
 * @author Wahib-Ul-Haq
 *
 */
public class SharedData {
	
	public SharedData() {
		
	}
	
	public static String fftType = "FFT_SP"; //FFT_CT : Cooley-Tukey and other option is FFT_SP : Superpowered
    public static final String appProcessName = "com.example.voicerecognizersp";
	public final static String APP_NAME = "VoiceRecognizerSP";

		 
	public static final File SD_PATH = Environment.getExternalStorageDirectory();
	public static final String SD_FOLDER_PATH_PARENT = "/Thesis";
	public static String SD_FOLDER_PATH = "/Thesis/VoiceRecognizer";
	public static String SD_FOLDER_PATH_LOGS = SD_FOLDER_PATH + "/Logs"; 
	public static String SD_FOLDER_PATH_CSV = SD_FOLDER_PATH + "/CSV"; 

	public static final String csvFileName = "voicerecognizer_mfcc.csv"; //"20MfccFeatures_";
	public static final String batteryFileName = "battery_data.txt";
	public static final String memcpuFileName = "memcpu_data.txt";
	public static final String cpuRealFileName = "cpu_real_usage_data.txt";

	 
	 
}
