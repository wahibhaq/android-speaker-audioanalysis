package edu.sinica.citi.mac.android.actclassification;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import edu.sinica.citi.mac.FileOperationMethods;
import edu.sinica.citi.mac.MathMethods;

public class ActClassificationActivity extends Activity{
	public static String TAG = "ActClassification";
	private Context m_context = this;	
	public static final File SD_PATH = Environment.getExternalStorageDirectory();

	/* For libsvm parameters */
	private int m_svmType;
	private int m_kernelType;
	private double m_degree;
	private double m_gamma;
	private double m_coef0;
	private double m_cost;
	private double m_nu;
	private double m_epsilonSVR;
	private int m_cacheSize;
	private double m_epsilon;
	private int m_shrinking;
	private int m_isProb;
	private double m_weightCsvc;
	private int m_nFold;
	private String m_fileTrain;  
	private String m_fileOutModel;
	/* The other member variables */
	private int m_nClass;
	private boolean m_isNormalized;
	private boolean m_isOfflineTrain;
	private String m_fileTrMean;
	private String m_fileTrSTD;
	private double[] m_arrayTrMean;
	private double[] m_arrayTrSTD;
	// set the settings above in the train() at just once.

	/*
	 * Load the native libraries (written in C/C++, compiled by NDK)
	 */
	private native int trainClassifierNative(int svmType, int kernelType, double degree, 
			double gamma, double coef0, double cost, double nu, double epsilonSVR, int cacheSize, 
			double epsilon, int shrinking, int isProb, double weightCsvc, int nFold,
			String trainingFile, String modelFile);

	private native int trainClassifierNative(int svmType, int kernelType, double degree, 
			double gamma, double coef0, double cost, double nu, double epsilonSVR, int cacheSize, 
			double epsilon, int shrinking, int isProb, double weightCsvc, int nFold,
			double[][] arrayXtr, int[] arrayYtr, int[][] arrayIdx, String modelFile);

	private native int doClassificationNative(double values[][], int indices[][],
			int isProb, String modelFile, int labels[], double probs[][]);

	static {
		try {
			System.loadLibrary("LibsvmAndroid"); //libLibsvmAndroid.so
		} catch (UnsatisfiedLinkError ule) {
			Log.e(TAG, "Hey, could not load native library LibsvmAndroid.");
		}
	}

	private void setDefault() {
		// set the default values according to the original function.
		m_svmType = 0;
		m_kernelType = 2;
		m_degree = 3;
		m_gamma = 0; // 1/num_features, whatever
		m_coef0 = 0;
		m_cost = 1;
		m_nu = 0.5;
		m_epsilonSVR = 0.1;
		m_cacheSize = 100;
		m_epsilon = 0.001;
		m_shrinking = 1;
		m_isProb = 1;
		m_weightCsvc = 1;
		m_nFold = 0; // disable
		m_fileTrain = SD_PATH + "/libsvm_data/training.libsvm";
		m_fileOutModel = SD_PATH + "/libsvm_data/modelFile";
		m_nClass = 2;   	
		m_isNormalized = false; // use the given data directly
		m_isOfflineTrain = false; //train the model on the phone
		m_fileTrMean = SD_PATH + "/libsvm_data/mean_tr.csv";
		m_fileTrSTD = SD_PATH + "/libsvm_data/std_tr.csv";
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_classification);

		Button btnTrain = (Button) findViewById(R.id.btnTrainLibsvm);
		btnTrain.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				train_libsvm();	    				
			}

		});

		Button btnTrainCsv = (Button) findViewById(R.id.btnTrainCsv);
		btnTrainCsv.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				train_csv();
			}

		});
		
		Button btnTrainArray = (Button) findViewById(R.id.btnTrainArray);
		btnTrainArray.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				train_array();
			}

		});
		
		Button btnPredictAcc = (Button) findViewById(R.id.btnPredictAcc);
		btnPredictAcc.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				predict_accuracy();
			}
		});

		Button btnPredictLabel = (Button) findViewById(R.id.btnPredictLabel);
		btnPredictLabel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				predict_label();
			}

		});
		
		Button btnRunExp = (Button) findViewById(R.id.btnExp);
		btnRunExp.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				runExp();
			}

		});



	}

	/**
	 * Table of parameter content (mostly copied from the official documents) <br><br>
	 * 
	 *    <B>
	 *    int trainClassifierNative(int svmType, int kernelType, float degree,
	 *		float gamma, float coef0, float cost, float nu,float epsilonSVR, int cacheSize, 
	 *		float epsilon, int shrinking, int isProb, float weightCsvc, int nFold,
	 * 		String trainingFile, String modelFile);
	 *    </B><br><br>
	 * 
	 * 
	 *	    parameters:<br>
	 *   
	 *  svmType: set type of SVM (default 0)<Table>
	 *		<li>0 -- C-SVC		(multi-class classification)</li>
	 *		<li>1 -- nu-SVC		(multi-class classification)</li>
	 *		<li>2 -- one-class SVM</li>
	 *		<li>3 -- epsilon-SVR	(regression)</li>
	 *		<li>4 -- nu-SVR		(regression)</li> </Table>
	 *		<br>
	 *	kernelType : set type of kernel function (default 2) <Table>
	 *		<li>0 -- linear: u'*v</li>
	 *		<li>1 -- polynomial: (gamma*u'*v + coef0)^degree</li>
	 *		<li>2 -- radial basis function: exp(-gamma*|u-v|^2)</li>
	 *		<li>3 -- sigmoid: tanh(gamma*u'*v + coef0)</li>
	 *		<li>4 -- precomputed kernel (kernel values in training_set_file)</li> </Table>
	 *	degree : set degree in kernel function (default 3) <br>
	 *	gamma : set gamma in kernel function (default 0.0 here)<br>
	 *	coef0 : set coef0 in kernel function (default 0)<br>
	 *	cost : set the parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1)<br>
	 *	nu: set the parameter nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)<br>
	 *	epsilonSVR: set the epsilon in loss function of epsilon-SVR (default 0.1)<br>
	 *	cachesize: set cache memory size in MB (default 100)<br>
	 *	epsilon: set tolerance of termination criterion (default 0.001)<br>
	 *	shrinking: whether to use the shrinking heuristics, 0 or 1 (default 1)<br>
	 *	probability_estimates: whether to train a SVC or SVR model for probability estimates, 0 or 1 (default 0)<br>
	 *	weight: set the parameter C of class i to weight*C, for C-SVC (default 1)<br>
	 *	nFold: n-fold cross validation mode (default disable)<br><br>
	 *
	 *  trainingFile: the path of train set in libsvm format<br>
	 *  modelFile: the path of trained model file<br>
	 *
	 */
	private void train_libsvm() {
		setDefault();

		// linear kernel, c = 1, use probability 
		m_svmType = 0;
		m_kernelType = 0;
		m_cost = 1;
		m_isProb = 1;
		m_nClass = 2; 
		m_nFold = 0;
		m_fileTrain = SD_PATH + "/libsvm_data/training.libsvm";
		m_fileOutModel = SD_PATH + "/libsvm_data/model_linear_libsvm";
		String fileTr = m_fileTrain; 

		// Load the data into arrays
		List<Integer> listY = new ArrayList<Integer>();
		List<List<Double>> listX = new ArrayList<List<Double>>();
		List<List<Integer>> listIndex = new ArrayList<List<Integer>>();

		FileOperationMethods.getListXYI(m_fileTrain, listY, listX, listIndex);
		int[] arrayYtr = MathMethods.convertList2array(listY);
		double[][] arrayXtr = MathMethods.convertList2TwoDarray(listX);
		int[][] arrayIdxTr = MathMethods.convertList2TwoDarray(listIndex);

		// normalization
		if(m_isNormalized) {
			arrayXtr = MathMethods.getFullMatrix(arrayXtr, arrayIdxTr);
			if(m_isOfflineTrain) { // load them from given files
				m_arrayTrMean = FileOperationMethods.csvread(m_fileTrMean, ',')[0];
				m_arrayTrSTD  = FileOperationMethods.csvread(m_fileTrSTD, ',')[0];
			}
			else { // compute from the feature and output them
				m_arrayTrMean = MathMethods.getMatrixMean(arrayXtr);
				m_arrayTrSTD = MathMethods.getMatrixSTD(arrayXtr);
				// code reuse. don't want to implement another csvwrite for single record
				double[][] matTemp = new double[1][m_arrayTrMean.length];
				matTemp[0] = m_arrayTrMean;
				FileOperationMethods.csvwrite(m_fileTrMean, matTemp, ',');
				matTemp[0] = m_arrayTrSTD;
				FileOperationMethods.csvwrite(m_fileTrSTD, matTemp, ',');
			}
			arrayXtr = MathMethods.getNormalizeZscore(arrayXtr, arrayXtr);

			// write into another file
			fileTr = m_fileOutModel + "_norm";
			FileOperationMethods.writeData2libsvm(arrayYtr, arrayXtr, fileTr);
		}

		// Call the native svm jni
		if (trainClassifierNative(m_svmType, m_kernelType, m_degree, m_gamma, m_coef0, m_cost,
				m_nu, m_epsilonSVR, m_cacheSize, m_epsilon, m_shrinking, m_isProb, m_weightCsvc, m_nFold,
				fileTr, m_fileOutModel) == -1) {
			Toast.makeText(m_context, "Something wrong when training the model", Toast.LENGTH_SHORT).show();
			// delete the normalized file, which it just a temporary file
			File fr = new File(fileTr);
			fr.delete();
			
		}
		else {
			Toast.makeText(m_context, "Training done!", Toast.LENGTH_SHORT).show();
		}

	}

	
	/*
	 * Train and model using csv file
	 */
	void train_csv() {
		setDefault();

		// linear kernel, c = 1, use probability 
		m_svmType = 0;
		m_kernelType = 0;
		m_cost = 1;
		m_isProb = 1;
		m_nClass = 2; 
		m_nFold = 0;
		m_fileTrain = SD_PATH + "/libsvm_data/Xtr.csv";
		m_fileOutModel = SD_PATH + "/libsvm_data/model_linear_csv";
		String fileYtr = SD_PATH + "/libsvm_data/Ytr.txt";

		/* Train */
		double[][] arrayXtr = FileOperationMethods.csvread(m_fileTrain, ',');
		int[][] arrayIdxTr = MathMethods.getIdxArray(arrayXtr);
		int[] arrayYtr = FileOperationMethods.loadListFile(fileYtr);

		/* Normalization */
		if(m_isNormalized) {
			arrayXtr = MathMethods.getFullMatrix(arrayXtr, arrayIdxTr);
			if(m_isOfflineTrain) { // load them from given files		
				m_arrayTrMean = FileOperationMethods.csvread(m_fileTrMean, ',')[0];
				m_arrayTrSTD  = FileOperationMethods.csvread(m_fileTrSTD, ',')[0];
			}
			else { // compute them from the feature
				m_arrayTrMean = MathMethods.getMatrixMean(arrayXtr);
				m_arrayTrSTD = MathMethods.getMatrixSTD(arrayXtr);
				// code reuse. don't want to implement another csvwrite for single record
				double[][] matTemp = new double[1][m_arrayTrMean.length];
				matTemp[0] = m_arrayTrMean;
				FileOperationMethods.csvwrite(m_fileTrMean, matTemp, ',');
				matTemp[0] = m_arrayTrSTD;
				FileOperationMethods.csvwrite(m_fileTrSTD, matTemp, ',');
			}
			arrayXtr = MathMethods.getNormalizeZscore(arrayXtr, arrayXtr);
		}

		/* Make to matrix sparse to save memory */
		arrayIdxTr = MathMethods.getSparseIndex(arrayXtr);
		arrayXtr = MathMethods.getSparseMatrix(arrayXtr, arrayIdxTr);

		/* Call the native svm jni */
		if (trainClassifierNative(m_svmType, m_kernelType, m_degree, m_gamma, m_coef0, m_cost,
				m_nu, m_epsilonSVR, m_cacheSize, m_epsilon, m_shrinking, m_isProb, m_weightCsvc, m_nFold,
				arrayXtr, arrayYtr, arrayIdxTr, m_fileOutModel) == -1) {
			Toast.makeText(m_context, "Something wrong when training the model", Toast.LENGTH_SHORT).show();
		}
		else {
			Toast.makeText(m_context, "Training done!", Toast.LENGTH_SHORT).show();
		}
	}

	/*
	 * Train the model using embedded array
	 */
	void train_array() {
		setDefault();

		// linear kernel, c = 1, use probability 
		m_svmType = 0;
		m_kernelType = 0;
		m_cost = 1;
		m_isProb = 1;
		m_nClass = 2; 
		m_nFold = 0;
		m_fileTrain = " ";//whatever
		m_fileOutModel = SD_PATH + "/libsvm_data/model_linear_array";

		/* Exactly the same in /libsvm_data/training.libsvm */
		int [] arrayYtr = {+1, -1, +1, -1, -1, -1, +1, +1, +1, +1,
				-1, -1, -1, +1, -1, -1, +1, +1, -1, -1,
				+1, -1, -1, -1, -1, -1, -1, -1, +1, -1};

		double[][] arrayXtr = {{0.708333,1,1,-0.320755,-0.105023,-1,1,-0.419847,-1,-0.225806,1,-1},
				{0.583333,-1,0.333333,-0.603774,1,-1,1,0.358779,-1,-0.483871,-1,1},
				{0.166667,1,-0.333333,-0.433962,-0.383562,-1,-1,0.0687023,-1,-0.903226,-1,-1,1},
				{0.458333,1,1,-0.358491,-0.374429,-1,-1,-0.480916,1,-0.935484,-0.333333,1},
				{0.875,-1,-0.333333,-0.509434,-0.347032,-1,1,-0.236641,1,-0.935484,-1,-0.333333,-1},
				{0.5,1,1,-0.509434,-0.767123,-1,-1,0.0534351,-1,-0.870968,-1,-1,1},
				{0.125,1,0.333333,-0.320755,-0.406393,1,1,0.0839695,1,-0.806452,-0.333333,0.5},
				{0.25,1,1,-0.698113,-0.484018,-1,1,0.0839695,1,-0.612903,-0.333333,1},
				{0.291667,1,1,-0.132075,-0.237443,-1,1,0.51145,-1,-0.612903,0.333333,1},
				{0.416667,-1,1,0.0566038,0.283105,-1,1,0.267176,-1,0.290323,1,1},
				{0.25,1,1,-0.226415,-0.506849,-1,-1,0.374046,-1,-0.83871,-1,1},
				{1,1,-0.0943396,-0.543379,-1,1,-0.389313,1,-1,-1,-1,1},
				{-0.375,1,0.333333,-0.132075,-0.502283,-1,1,0.664122,-1,-1,-1,-1,-1},
				{0.333333,1,-1,-0.245283,-0.506849,-1,-1,0.129771,-1,-0.16129,0.333333,-1},
				{0.166667,-1,1,-0.358491,-0.191781,-1,1,0.343511,-1,-1,-1,-0.333333,-1},
				{0.75,-1,1,-0.660377,-0.894977,-1,-1,-0.175573,-1,-0.483871,-1,-1},
				{-0.291667,1,1,-0.132075,-0.155251,-1,-1,-0.251908,1,-0.419355,0.333333,1},
				{1,1,-0.132075,-0.648402,1,1,0.282443,1,1,-1,1},
				{0.458333,1,-1,-0.698113,-0.611872,-1,1,0.114504,1,-0.419355,-1,-1},
				{-0.541667,1,-1,-0.132075,-0.666667,-1,-1,0.633588,1,-0.548387,-1,-1,1},
				{0.583333,1,1,-0.509434,-0.52968,-1,1,-0.114504,1,-0.16129,0.333333,1},
				{-0.208333,1,-0.333333,-0.320755,-0.456621,-1,1,0.664122,-1,-0.935484,-1,-1},
				{-0.416667,1,1,-0.603774,-0.191781,-1,-1,0.679389,-1,-0.612903,-1,-1},
				{-0.25,1,1,-0.660377,-0.643836,-1,-1,0.0992366,-1,-0.967742,-1,-1,-1},
				{0.0416667,-1,-0.333333,-0.283019,-0.260274,1,1,0.343511,1,-1,-1,-0.333333,-1},
				{-0.208333,-1,0.333333,-0.320755,-0.319635,-1,-1,0.0381679,-1,-0.935484,-1,-1,-1},
				{-0.291667,-1,1,-0.169811,-0.465753,-1,1,0.236641,1,-1,-1,-1},
				{-0.0833333,-1,0.333333,-0.509434,-0.228311,-1,1,0.312977,-1,-0.806452,-1,-1,-1},
				{0.208333,1,0.333333,-0.660377,-0.525114,-1,1,0.435115,-1,-0.193548,-0.333333,1},
				{0.75,-1,0.333333,-0.698113,-0.365297,1,1,-0.0992366,-1,-1,-1,-0.333333,-1},	
		};

		int[][] arrayIdxTr = {{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{2,3,4,5,6,7,8,9,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
				{1,2,3,4,5,6,7,8,9,10,12,13},
				{1,2,3,4,5,6,7,8,9,10,11,12,13},
		};
		// Features are sparse already
		/* Normalization */
		if(m_isNormalized) {
			arrayXtr = MathMethods.getFullMatrix(arrayXtr, arrayIdxTr);
			if(m_isOfflineTrain) { // load them from given files
				m_arrayTrMean = FileOperationMethods.csvread(m_fileTrMean, ',')[0];
				m_arrayTrSTD  = FileOperationMethods.csvread(m_fileTrSTD, ',')[0];
			}
			else { // compute them from the feature
				m_arrayTrMean = MathMethods.getMatrixMean(arrayXtr);
				m_arrayTrSTD = MathMethods.getMatrixSTD(arrayXtr);
			}
			arrayXtr = MathMethods.getNormalizeZscore(arrayXtr, arrayXtr);
		}


		/* Call the native svm jni */
		if (trainClassifierNative(m_svmType, m_kernelType, m_degree, m_gamma, m_coef0, m_cost,
				m_nu, m_epsilonSVR, m_cacheSize, m_epsilon, m_shrinking, m_isProb, m_weightCsvc, m_nFold,
				arrayXtr, arrayYtr, arrayIdxTr, m_fileOutModel) == -1) {
			Toast.makeText(m_context, "Something wrong when training the model", Toast.LENGTH_SHORT).show();
		}
		else {
			Toast.makeText(m_context, "Training done!", Toast.LENGTH_SHORT).show();
		}
	}
	/**
	 *  Load the test data and predict the label using the trained model <br><br>
	 * 
	 *   <B>
	 *   int doClassificationNative(double values[][], int indices[][],
	 *		int isProb, String modelFile, int labels[], double probs[]);
	 *	 </B><br><br>
	 *
	 *	 values: feature (X) matrix <br>
	 *   indices: index matrix for whose item is non-zero <br>
	 *   isProb: use probability estimate or not (default 0) <br>
	 *   modelFile: the path of trained model <br>
	 *   labels: label (Y) array <br>
	 *   probs: probability estimate matrix (supports multi-label) <br>
	 *   
	 *   This version accepts the libsvm file with label, for experiment validation
	 */
	private void predict_accuracy() {
		String fileTest = SD_PATH + "/libsvm_data/test_label.libsvm";		

		// Load the features into arrays
		List<Integer> listY = new ArrayList<Integer>();
		List<List<Double>> listX = new ArrayList<List<Double>>();
		List<List<Integer>> listIndex = new ArrayList<List<Integer>>();

		FileOperationMethods.getListXYI(fileTest, listY, listX, listIndex);
		int[] arrayYte = MathMethods.convertList2array(listY);
		double[][] arrayXte = MathMethods.convertList2TwoDarray(listX);
		int[][] arrayIdxTe = MathMethods.convertList2TwoDarray(listIndex);

		// normalization
		if(m_isNormalized) {
			arrayXte = MathMethods.getFullMatrix(arrayXte, arrayIdxTe);
			arrayXte = MathMethods.getNormalizeZscore(arrayXte, m_arrayTrMean, m_arrayTrSTD);		
		}

		// prediction
		int[] arrayPredLabel = new int[arrayXte.length];
		double[][] arrayPredScore = new double[arrayXte.length][m_nClass];

		doClassificationNative(arrayXte, arrayIdxTe, m_isProb, m_fileOutModel, arrayPredLabel, arrayPredScore);				
		/* Compute the performance */
		float accuracy = MathMethods.getAccuracy(arrayYte, arrayPredLabel);
		Toast.makeText(m_context, "Accuracy:" + Float.toString(accuracy), Toast.LENGTH_LONG).show();
	}


	/**
	 *  Load the test data and predict the label using the trained model <br><br>
	 * 
	 *   <B>
	 *   int doClassificationNative(double values[][], int indices[][],
	 *		int isProb, String modelFile, int labels[], double probs[]);
	 *	 </B><br><br>
	 *
	 *	 values: feature (X) matrix <br>
	 *   indices: index matrix for whose item is non-zero <br>
	 *   isProb: use probability estimate or not (default 0) <br>
	 *   modelFile: the path of trained model <br>
	 *   labels: label (Y) array <br>
	 *   probs: probability estimate matrix (supports multi-label) <br>
	 *   
	 *   This version accepts the libsvm file without label
	 */
	private void predict_label() {
		String fileTest = SD_PATH + "/libsvm_data/Xte.libsvm";		
		// Load the features into arrays
		List<List<Double>> listX = new ArrayList<List<Double>>();
		List<List<Integer>> listIndex = new ArrayList<List<Integer>>();

		FileOperationMethods.getListXYI(fileTest, null, listX, listIndex);

		double[][] arrayXte = MathMethods.convertList2TwoDarray(listX);
		int[][] arrayIdxTe = MathMethods.convertList2TwoDarray(listIndex);

		// normalization
		if(m_isNormalized) {
			arrayXte = MathMethods.getFullMatrix(arrayXte, arrayIdxTe);
			arrayXte = MathMethods.getNormalizeZscore(arrayXte, m_arrayTrMean, m_arrayTrSTD);		
		}

		// prediction
		int[] arrayPredLabel = new int[arrayXte.length];
		double[][] arrayPredScore = new double[arrayXte.length][m_nClass];

		doClassificationNative(arrayXte, arrayIdxTe, m_isProb, m_fileOutModel, arrayPredLabel, arrayPredScore);		

		// Pops the predicted label
		String s = "";
		for(int label: arrayPredLabel) {
			s += label + ",";
		}
		Toast.makeText(m_context, "predicted labels: " + s, Toast.LENGTH_LONG).show();
	}
	
	/*
	 * Illustrate a simple classification experiment.
	 * 
	 * Load both training and testing data in csv files, and show accuracy on the screen
	 * Off-line training: train the model on the phone rather than load the pre-trained one
	 * Normalize the data using the mean and STD of training data
	 */
	void runExp() {
		setDefault();
		// linear kernel, c = 1, use probability 
		m_svmType = 0;
		m_kernelType = 0;
		m_cost = 1;
		m_isProb = 1;
		m_nClass = 2; 
		m_nFold = 0;
		m_fileOutModel = SD_PATH + "/libsvm_data/model_linear_csv";
		m_fileTrain = SD_PATH + "/libsvm_data/Xtr.csv";
		String fileYtr = SD_PATH + "/libsvm_data/Ytr.txt";
		
		String fileXte = SD_PATH + "/libsvm_data/Xte.csv";
		String fileYte = SD_PATH + "/libsvm_data/Yte.txt";
		
		/* Load data */
		double[][] arrayXtr = FileOperationMethods.csvread(m_fileTrain, ',');
		int[] arrayYtr = FileOperationMethods.loadListFile(fileYtr);
		int[][] arrayIdxTr = MathMethods.getIdxArray(arrayXtr);
		
		double[][] arrayXte = FileOperationMethods.csvread(fileXte, ',');
		int[] arrayYte = FileOperationMethods.loadListFile(fileYte);
		int[][] arrayIdxTe = MathMethods.getIdxArray(arrayXte);		
		
		/* Normalization */
		if(m_isNormalized) {
			arrayXtr = MathMethods.getFullMatrix(arrayXtr, arrayIdxTr);
			if(m_isOfflineTrain) { // load them from given files
				m_arrayTrMean = FileOperationMethods.csvread(m_fileTrMean, ',')[0];
				m_arrayTrSTD  = FileOperationMethods.csvread(m_fileTrSTD, ',')[0];
			}
			else { // compute them from the feature
				m_arrayTrMean = MathMethods.getMatrixMean(arrayXtr);
				m_arrayTrSTD = MathMethods.getMatrixSTD(arrayXtr);
				// code reuse. don't want to implement another csvwrite for single record
				double[][] matTemp = new double[1][m_arrayTrMean.length];
				matTemp[0] = m_arrayTrMean;
				FileOperationMethods.csvwrite(m_fileTrMean, matTemp, ',');
				matTemp[0] = m_arrayTrSTD;
				FileOperationMethods.csvwrite(m_fileTrSTD, matTemp, ',');
			}
			arrayXtr = MathMethods.getNormalizeZscore(arrayXtr, arrayXtr);
			arrayXte = MathMethods.getNormalizeZscore(arrayXte, m_arrayTrMean, m_arrayTrSTD);
		}
		
		/* Sparsify again (just in case) */
		arrayIdxTr = MathMethods.getSparseIndex(arrayXtr);
		arrayXtr = MathMethods.getSparseMatrix(arrayXtr, arrayIdxTr);
		
		arrayIdxTe = MathMethods.getSparseIndex(arrayXte);
		arrayXte = MathMethods.getSparseMatrix(arrayXte, arrayIdxTe);	
		
		/* Call native library to train the model */
		if (trainClassifierNative(m_svmType, m_kernelType, m_degree, m_gamma, m_coef0, m_cost,
				m_nu, m_epsilonSVR, m_cacheSize, m_epsilon, m_shrinking, m_isProb, m_weightCsvc, m_nFold,
				arrayXtr, arrayYtr, arrayIdxTr, m_fileOutModel) == -1) {
			Toast.makeText(m_context, "Something wrong when training the model", Toast.LENGTH_SHORT).show();
		}
		else {
			Toast.makeText(m_context, "Training done!", Toast.LENGTH_SHORT).show();
		}
		
		/* Prediction */
		int[] arrayPredLabel = new int[arrayXte.length];
		double[][] arrayPredScore = new double[arrayXte.length][m_nClass];
		
		doClassificationNative(arrayXte, arrayIdxTe, m_isProb, m_fileOutModel, arrayPredLabel, arrayPredScore);	
		
		/* Evaluation */
		float accuracy = MathMethods.getAccuracy(arrayYte, arrayPredLabel);
		Toast.makeText(m_context, "Accuracy:" + Float.toString(accuracy), Toast.LENGTH_LONG).show();
	}
}



