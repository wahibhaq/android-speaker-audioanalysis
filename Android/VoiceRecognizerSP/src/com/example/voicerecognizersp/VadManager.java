package com.example.voicerecognizersp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.tum.classifiertest.FastRandomForest;

public class VadManager {
	

	static final String TAG = "VadManager";
	private String txtMessage = "";

	private volatile ExecutorService execService = null;
	private static Runnable mainRunnable = null;

	

	private static AssetManager assManager = null;
	private static Kryo kryo = null;
	private static FastRandomForest rf = null;
	
	//private static Context appContext;
	
	public VadManager(Context context) {
		
		//Initializations
        execService = Executors.newSingleThreadExecutor();
        
		assManager = context.getAssets();
		kryo = new Kryo();
		
		mainRunnable = new Runnable() {
			
			@Override
			public void run() {
				
			    Log.i(TAG, "Main Code is running...");

			    try {
			    	predictifyRf();
				} 
				catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
			
		init();
		
	}
	
	private void init() {
		
		loadModelFromAssets();
	}
	
	public void executeRfVad() {
		
		execService.submit(mainRunnable);

	}
	
	public void stopRfVad() {
		
		execService.shutdown();
		
		
	}
	

	private void predictifyRf() {
		
		try {
				
				///Loading CSV input
		    	Instances csvData = null;
				FileInputStream fis = null;
				InputStream csvIs = null;
				
				File vadCsvFile = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_CSV + File.separator + SharedData.vadCsvFileName);
				
				fis = new FileInputStream(vadCsvFile);
				csvIs = new BufferedInputStream(fis);
			
			    CSVLoader loader = new CSVLoader();
				
				txtMessage = "Loading CSV...";
			    Log.v(TAG, txtMessage);
			    
				loader.setSource(csvIs);
				csvData = loader.getDataSet();
				csvData.setClassIndex(csvData.numAttributes() - 1); //class index starts from 0 
		       // Log.i(TAG, "csvData numAttributes : " + String.valueOf(csvData.numAttributes()));


				Log.v(TAG, "CSV loaded.");
				
		    	
		    	//Prediction
				NumericToNominal num2nom = new NumericToNominal();
				
				String[] options = new String[2];
		        options[0]="-R";
		        options[1]="19";
		        
		        Instances newData = null;
		        
		        
		        Log.i(TAG, "Running prediction/testing...");
  
	
				num2nom.setOptions(options);
			    num2nom.setInputFormat(csvData);
			    newData = Filter.useFilter(csvData, num2nom);
	
				int numInstances = newData.numInstances();
				//Log.i(TAG, "numInstances : " + newData.numInstances());
		        //Log.i(TAG, "newData numAttributes : " + newData.numAttributes());


				//only for informational purposes (attribute metadata)
				rf.setM_Info(new Instances(newData, 0));
				
				int truePos = 0, trueNeg = 0, falsePos = 0, falseNeg = 0;
				
				for(int instIdx = 0; instIdx < numInstances; instIdx++) {
					
					Instance currInst = newData.instance(instIdx);
					//Log.i(TAG, "currInst : " + instIdx);

					double score = 0;
					try {
						score = rf.classifyInstance(currInst);
					} 
					catch (ArrayIndexOutOfBoundsException e) {
						//e.printStackTrace();
					}
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					int pred = Integer.parseInt(newData.classAttribute().value((int) score));
					int label = Integer.parseInt(newData.classAttribute().value((int) currInst.classValue()));
					
					if (pred == 1)
						if (label == 1)
							truePos++;
						else
							falsePos++;
					else
						if (label == 1)
							falseNeg++;
						else
							trueNeg++;
					
				
					
					
				}
				
				Log.v(TAG, "Confusion matrix:");
				
				Log.v(TAG, "\tTP: " + truePos);
				Log.v(TAG, "\tTN: " + trueNeg);
				Log.v(TAG, "\tFP: " + falsePos);
				Log.v(TAG, "\tFN: " + falseNeg);
			    
				//Releaseing
				if(fis != null) fis.close();
                if(csvIs != null) csvIs.close();
                if(csvData != null) csvData.delete();
                if(newData != null) newData.delete();

				
			}
			
		
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
	}
	
	
	private boolean loadModelFromAssets() {
		
	//Loading Model (bound to be done on main UI thread, otherwise doesnt work
		InputStream is = null;
		Input input = null;
		InputStream model = null;
		
		try {
			
			is = assManager.open("fastrf.model");
			model = new BufferedInputStream(is);

			input = new Input(model);
			rf = kryo.readObject(input, FastRandomForest.class);
		
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 	
	    finally {
            try{
            	if(input != null) input.close();

                if(is != null) is.close();
                if(model != null) model.close();
            } catch (Exception ex){
                 
            }
        }
  
	    
	    txtMessage = "Random Forest loaded.";
	    Log.v(TAG, txtMessage);

	    
	    if(rf != null)
	    	return true;
	    else
	    	return false;
	}
	
	

}
