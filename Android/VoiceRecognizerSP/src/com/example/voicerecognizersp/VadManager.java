package com.example.voicerecognizersp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.w3c.dom.Element;

import weka.classifiers.pmml.consumer.NeuralNetwork;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
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
	//private static Runnable mainRunnable = null;
	

	private static AssetManager assManager = null;
	private static Kryo kryo = null;
	private static FastRandomForest rf = null;
	//private static NeuralNetwork nn = null;
	
	//Loading CSV input
	private static Instances testData = null;
	private ArrayList<Attribute> atts;
	static final int size = 36;
	private static double[] finalMfccVadList;
	private static double[] lastMfccVadList = null;

	
	public VadManager(Context context) {
		
		//Initializations
        execService = Executors.newSingleThreadExecutor();
        
		assManager = context.getAssets();
		kryo = new Kryo();

		
			
		init();
		
	}
	
	private void init() {
		
		loadModelFromAssets();
		
		vadInit();
	}
	
	public int executeRfVad(double[] frameData) {
		
		return predictifyVadOnFrame(frameData);
		
	}

	
	public void stopRfVad() {
		
		execService.shutdown();
		
		
	}
	
	//original one
	/*private void predictifyRf() {
		
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
				//csvData = loader.getDataSet();
				
				csvData = new Instances("vad", , capacity);
				
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
			
	}*/
	
	//modified one which works with vad csv file
	/*private void predictifyRf() {
		
		try {
				
				///Loading CSV input
		    	Instances csvData = null;
				InputStream is = null;
				InputStream csvIs = null;
				
				File vadCsvFile = new File(SharedData.SD_PATH + SharedData.SD_FOLDER_PATH_CSV + File.separator + SharedData.vadCsvFileName);
				
				is = new FileInputStream(vadCsvFile);
				csvIs = new BufferedInputStream(is);
				
			    //csvData = new Instances(new BufferedReader(new FileReader(vadCsvFile)));
			
			    CSVLoader loader = new CSVLoader();
				
				txtMessage = "Loading CSV...";
			    Log.v(TAG, txtMessage);
			    
				loader.setSource(csvIs);
				csvData = loader.getDataSet();
				
				//csvData = new Instances("vad", frameData, frameData.length);
				
				////class index starts from 0 //numAttributes() - 1 was original. so now with -1 its like 0 -> 35
				csvData.setClassIndex(csvData.numAttributes() - 1); 
		        Log.i(TAG, "csvData numAttributes : " + String.valueOf(csvData.numAttributes()));


				Log.v(TAG, "CSV loaded.");
				
		    	
		    	//Prediction
				
				String[] options = new String[2];
		        options[0]="-R";
		        
		        //originally 37 when there were 37 columns including last one as the output. Now is 36 with 36 # of columns
		        options[1]="35";  //36 total columns
		        
		        Instances newData = null;
		        newData = new Instances(csvData);
		        
		        
		        Log.i(TAG, "Running prediction/testing...");
		        
		        //Applying filter after converting to Nominal
				NumericToNominal num2nom = new NumericToNominal();

				num2nom.setOptions(options);
			    //num2nom.setInputFormat(csvData);
				num2nom.setInputFormat(newData);
			    newData = Filter.useFilter(newData, num2nom);
	
				int numInstances = newData.numInstances();
				Log.i(TAG, "numInstances : " + newData.numInstances());
		        Log.i(TAG, "newData numAttributes : " + newData.numAttributes());


				//only for informational purposes (attribute metadata)
				rf.setM_Info(new Instances(newData, 0));
				
				int truePos = 0, trueNeg = 0, falsePos = 0, falseNeg = 0;
				
				for(int instIdx = 0; instIdx < numInstances; instIdx++) {
					
					Instance currInst = newData.instance(instIdx);
					Log.i(TAG, "currInst : " + instIdx);

					double score = 0;
					try {
						score = rf.classifyInstance(currInst);
						Log.i(TAG, "score : " + score);

					} 
					catch (ArrayIndexOutOfBoundsException e) {
						e.printStackTrace();
					}
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					//Log.i(TAG, "newData ClassAttribute.value : " + newData.classAttribute().value());
						
					//code changed because predValue was coming blank so ParstInt was giving error
					String predValue = newData.classAttribute().value((int) score); 
					int pred = 0;
					if(!predValue.equals(""))
						pred = Integer.parseInt(predValue);
					
					String labelValue = newData.classAttribute().value((int) currInst.classValue()); 
					int label = 0;
					if(!labelValue.equals(""))
						label = Integer.parseInt(labelValue);
					
					
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
				if(is != null) is.close();
                if(csvIs != null) csvIs.close();
                if(csvData != null) csvData.delete();
                if(newData != null) newData.delete();

				
			}
			
		
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	} */
	
	private void vadInit() {

		finalMfccVadList = new double[size];

		atts = new ArrayList<Attribute>(size);

	 	for(int i=0; i<size; i++) {
	 		atts.add(new Attribute("content"+i));
	 	
	 	}
	 	
    
	}
	
	/**
	 * It takes extracted features, calculate delta values for next frame and previous frame.  
	 * The first 18 columns would be the new frame and the next 18 columns are the difference of values i.e delta
	 * @param newline
	 * @return if everything works fine then true otherwise false
	 */
	
	private boolean getMfccWithDelta(double[] newline) {
		
		int colCount = 0; 
		int deltaColCount = newline.length;
		
		if(lastMfccVadList != null) {
			
			if(lastMfccVadList.length == newline.length) {
				
				//where main computation happens
				
				for (double element : newline) {
										
					finalMfccVadList[colCount] = element;//for populating the first 18 elements
					
					//handling filling of delta list of other 18 columns as different of values from both frames
					finalMfccVadList[deltaColCount] = finalMfccVadList[colCount] - lastMfccVadList[colCount] ; //frame2 - frame1

					++deltaColCount;
					++colCount;
				}
				
				
					
				return true;
			}
			else {
				
				//if length doesn't match
				
				Log.i(TAG, "length of both frames doesn't match");
				
				return false;
			}
			
			
		}
		else {
			
			//for first time
			lastMfccVadList = newline;
			
			return false;

		}
		
		
		
		
		
	}
	
	
	

	private int predictifyVadOnFrame(double[] frameData) {
		
		int output = 0;
		
		try {
				
			    testData = new Instances("TestInstances",atts,0);
		        //Log.i(TAG, "csvData before adding instance : " + testData);
		        //Log.i(TAG, "----------------------------------------------");
		    
			    if(getMfccWithDelta(frameData))
			    	testData.add(new DenseInstance(1.0, finalMfccVadList));
			    else
			    	return output;
			    
			    
		        //Log.i(TAG, "csvData after adding instance : " + testData);
		        //Log.i(TAG, "----------------------------------------------");

		        
				////class index starts from 0 //numAttributes() - 1 was original. so now with -1 its like 0 -> 35
			    testData.setClassIndex(testData.numAttributes() - 1);
		        
			    //Log.i(TAG, "csvData numAttributes : " + testData.numAttributes());
				//Log.i(TAG, "numInstances : " + testData.numInstances());


				//Log.v(TAG, "CSV loaded.");
				
		    	
		    	//Prediction
				
				String[] options = new String[2];
		        options[0]="-R";
		        
		        //Now 36 # of columns
		        options[1]="35";

		        //Log.i(TAG, "Running prediction/testing...");
		        
		        //Applying filter after converting to Nominal
				NumericToNominal num2nom = new NumericToNominal();

				num2nom.setOptions(options);
				num2nom.setInputFormat(testData);
			    testData = Filter.useFilter(testData, num2nom);
	
				
				//Log.i(TAG, "numInstances : " + testData.numInstances());
		        //Log.i(TAG, "newData numAttributes : " + testData.numAttributes());


				//only for informational purposes (attribute metadata)
				rf.setM_Info(new Instances(testData, 0));
			    
				
				int instIdx = 0;
				
				if(!testData.isEmpty()) {
				
						Instance currInst = testData.instance(instIdx);
					//Log.i(TAG, "currInst : " + instIdx);
	
					double score = 0;
					try {
						
						score = rf.classifyInstance(currInst);
						
						//Log.i(TAG, "classifier score : " + score);
	
					} 
					catch (ArrayIndexOutOfBoundsException e) {
						e.printStackTrace();
					}
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	
					//Log.i(TAG, "newData ClassAttribute.value : " + testData.classAttribute().value());
		
					//code changed because predValue was coming blank so ParstInt was giving error
					String predValue = testData.classAttribute().value((int) score); 
					int pred = 0;
					if(!predValue.equals(""))
						pred = Integer.parseInt(predValue);
					
					//compiling data for confusion matrix
					/*
					int truePos = 0, trueNeg = 0, falsePos = 0, falseNeg = 0;
	
					String actualValue = testData.classAttribute().value((int) currInst.classValue()); 
					int actual = 0;
					if(!actualValue.equals(""))
						actual = Integer.parseInt(actualValue);
					
					if (pred == 1)
						if (actual == 1)
							truePos++;
						else
							falsePos++;
					else
						if (actual == 1)
							falseNeg++;
						else
							trueNeg++;
					
	
					//Log.v(TAG, "Confusion matrix:");
					
					Log.v(TAG, "\tTP: " + truePos);
					Log.v(TAG, "\tTN: " + trueNeg);
					Log.v(TAG, "\tFP: " + falsePos);
					Log.v(TAG, "\tFN: " + falseNeg);
					 
					 */
				    
					//Releaseing
					
	                output = pred;
	                //Log.v(TAG, "prediction : " + pred);


					
				}
	
	            
				
				if(testData != null) testData.delete();
			        
				
    			
			}
			
		
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		return output;
			
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
