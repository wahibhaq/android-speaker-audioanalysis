package edu.sinica.citi.mac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MathMethods {

	public MathMethods() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Convert the 1-D list to array
	 * 
	 * @param listInput 1-D List in int
	 * @return 1-D array in int
	 */
	public static int[] convertList2array(List<Integer> listInput) {
		int nRow = listInput.size();
		int[] arrayOut = new int[nRow];
		
		for(int row = 0; row < nRow; row++) {
			arrayOut[row] = listInput.get(row);
		}
		return arrayOut;
		
	}
	
	/**
	 * Convert the 1-D list to array in string
	 * 
	 * @param listInput 1-D List in string
	 * @return 1-D array in string
	 */
	public static String[] convertList2arrayString(List<String> listInput) {
		int nRow = listInput.size();
		String[] arrayOut = new String[nRow];
		
		for(int row = 0; row < nRow; row++) {
			arrayOut[row] = listInput.get(row);
		}
		return arrayOut;
		
	}
	
	
	/**
	 * Convert the 2-D list to array (might be different in the second dimension)
	 * 
	 * @param listInput 2-D double list
	 * @return 2-D array 
	 */
	public static double[][] convertList2TwoDarray(List<List<Double>> listInput) {
		int nRow = listInput.size();
		double[][] arrayOut = new double[nRow][];
		

		for(int row = 0; row < nRow; row++) {
			List<Double> record = listInput.get(row);
			int nCol = record.size();
			double[] rowIdx = new double[nCol];
			for(int i = 0; i < nCol; i++) {
				rowIdx[i] = record.get(i);
			}
			arrayOut[row] = rowIdx;
		}
		return arrayOut;
	}
	
	/**
	 * Convert the 2-D list to array (might be different in the second dimension)
	 * 
	 * @param listInput 2-D int list
	 * @return 2-D array 
	 */
	public static int[][] convertList2TwoDarray(List<List<Integer>> listInput) {
		int nRow = listInput.size();
		int[][] arrayOut = new int[nRow][];
		

		for(int row = 0; row < nRow; row++) {
			List<Integer> record = listInput.get(row);
			int nCol = record.size();
			int[] rowIdx = new int[nCol];
			for(int i = 0; i < nCol; i++) {
				rowIdx[i] = record.get(i);
			}
			arrayOut[row] = rowIdx;
		}
		
		return arrayOut;
	}
	
	
	/**
	 * Use the mean and STD of source data to normalize the target data,
	 * the normalized data will be zero mean, and will STD equals 1 (row major)
	 * 
	 * The equation is: M* = (M-mean) / std
	 * 
	 * @param matrixSource 2-D feature array in row major, whose mean and std will be used
	 * @param matrixTarget 2-D feature array in row major
	 * 
	 * @return the normalized matrix
	 */
	public static float[][] getNormalizeZscore(float[][] matrixSource, float[][] matrixTarget) {
		/* get mean and std vector (for every column) */
		float[] arrayMean = getMatrixMean(matrixSource);
		float[] arraySTD = getMatrixSTD(matrixSource);
		
		float[][] matNormalized = matrixTarget.clone();
		for(int col = 0; col < matNormalized[0].length; col++) {
			float meanSource = arrayMean[col];
			float stdSource  = arraySTD[col];
			for(int row = 0; row < matNormalized.length; row++) {
				matNormalized[row][col] = (matNormalized[row][col] - meanSource) / stdSource;
			}
		}
		
		return matNormalized;	
 	}
	
	/**
	 * Use the mean and STD of source data to normalize the target data,
	 * the normalized data will be zero mean, and will STD equals 1 (row major)
	 * 
	 * The equation is: M* = (M-mean) / std
	 * 
	 * @param matrixSource 2-D feature array in row major, whose mean and std will be used
	 * @param matrixTarget 2-D feature array in row major
	 * 
	 * @return the normalized matrix
	 */
	public static double[][] getNormalizeZscore(double[][] matrixSource, double[][] matrixTarget) {
		/* get mean and std vector (for every column) */
		double[] arrayMean = getMatrixMean(matrixSource);
		double[] arraySTD = getMatrixSTD(matrixSource);
		
		double[][] matNormalized = matrixTarget.clone();
		for(int col = 0; col < matNormalized[0].length; col++) {
			double meanSource = arrayMean[col];
			double stdSource  = arraySTD[col];
			for(int row = 0; row < matNormalized.length; row++) {
				matNormalized[row][col] = (double)(matNormalized[row][col] - meanSource) / (double)stdSource;
			}

		}
		
		return matNormalized;		
	}
	
	/**
	 * Use the mean and STD of source data to normalize the target data,
	 * the normalized data will be zero mean, and will STD equals 1 (row major)
	 * 
	 * The equation is: M* = (M-mean) / std
	 * 
	 * @param matrixTarget 2-D feature array in row major
	 * @param arrayMean 1-D mean array 
	 * @param arraySTD 1-D STD array
	 * 
	 * @return the normalized matrix
	 */
	public static double[][] getNormalizeZscore(double[][] matrixTarget, double[] arrayMean, double[] arraySTD) {	
		double[][] matNormalized = matrixTarget.clone();
		for(int col = 0; col < matNormalized[0].length; col++) {
			double meanSource = arrayMean[col];
			double stdSource  = arraySTD[col];
			for(int row = 0; row < matNormalized.length; row++) {
				matNormalized[row][col] = (double)(matNormalized[row][col] - meanSource) / (double)stdSource;
			}
		}
		
		return matNormalized;		
	}
	
	/**
	 * Return the arithmetic mean of the 1-D array 
	 * 
	 * @param arrayData 1-D array in double
	 * @return the mean value in double
	 */
	public static double getMean(double[] arrayData) {
		double mean = 0;
		double sum = 0;
		
		for(double d: arrayData) {
			sum += d;
		}
		
		mean = sum / (double)arrayData.length;
		return mean;
	}
	
	/**
	 * Return the arithmetic mean of the 1-D array 
	 * 
	 * @param arrayData 1-D array in float
	 * @return the mean value in float
	 */
	public static float getMean(float[] arrayData) {
		float mean = 0;
		float sum = 0;
		
		for(float d: arrayData) {
			sum += d;
		}
		
		mean = sum / arrayData.length;
		return mean;
	}
	
	/**
	 * Return the standard deviation, which is the square root of its variance
	 * 
	 * @param arrayData 1-D array in double
	 * @return standard deviation
	 */
	public static double getSTD(double[] arrayData) {
		double variance = getVariance(arrayData);
		double std = Math.sqrt(variance);
		
		/* 
		 * in case the std = 0, which might result the divided-by-zero error 
		 * when computing using this in normalization.
		 * 
		 * add a small epsilon to it to prevent this.
		 */	
		std += 1E-14;
		return std;
	}
	
	/**
	 * Return the standard deviation, which is the square root of its variance
	 * 
	 * @param arrayData: 1-D array in float
	 * @return standard deviation
	 */
	public static float getSTD(float[] arrayData) {
		float variance = getVariance(arrayData);
		float std = (float) Math.sqrt(variance);
		
		/* 
		 * in case the std = 0, which might result the divided-by-zero error 
		 * when computing using this in normalization.
		 * 
		 * add a small epsilon to it to prevent this.
		 */	
		std += 1E-14;
		return std;
	}
	
	/**
	 * Return the variance of array
	 * 
	 * @param arrayData 1-D array in double
	 * @return variance
	 * 
	 * Note: the sum is divided by <B> N-1 </B> rather than N
	 */
	public static double getVariance(double[] arrayData) {
		double mean = getMean(arrayData);
		double sum = 0;
		
		for(double d: arrayData) {
			sum += ((d-mean) * (d-mean));
		}		
		
		double variance = sum / (arrayData.length-1);
		
		return variance;	
	}
	
	/**
	 * Return the variance of array
	 * 
	 * @param arrayData 1-D array in double
	 * @return variance
	 * 
	 * Note: the sum is divided by <B> N-1 </B> rather than N
	 */
	public static float getVariance(float[] arrayData) {
		float mean = getMean(arrayData);
		float sum = 0;
		
		for(float d: arrayData) {
			sum += ((d-mean) * (d-mean));
		}		
		
		float variance = sum / (arrayData.length-1);
		
		return variance;	
	}
	
	/**
	 * Return the arithmetic mean vector of the 2-D matrix (row major)
	 * 
	 * @param matrixData 2-D array in double (row major)
	 * @return the mean vector in double (for each column)
	 */
	public static double[] getMatrixMean(double[][] matrixData) {
		int nCol = matrixData[0].length;
		
		double[] arrayMean = new double[nCol];
		double matTrans[][] = getTransposeMatrix(matrixData);
		// the row of the transpose matrix = the column of the matrix
		for(int row = 0; row < matTrans.length; row++) {
			arrayMean[row] = getMean(matTrans[row]);
		}
		

		return arrayMean;
	}
	
	/**
	 * Return the arithmetic mean vector of the 2-D matrix (row major)
	 * 
	 * @param matrixData 2-D array in float (row major)
	 * @return the mean vector in float (for each column)
	 */
	public static float[] getMatrixMean(float[][] matrixData) {
		int nCol = matrixData[0].length;
		
		float[] arrayMean = new float[nCol];
		float matTrans[][] = getTransposeMatrix(matrixData);
		// the row of the transpose matrix = the column of the matrix
		for(int row = 0; row < matTrans.length; row++) {
			arrayMean[row] = getMean(matTrans[row]);
		}
		return arrayMean;
	}
	
	/**
	 * Return the standard deviation vector of the 2-D matrix (row major)
	 * 
	 * @param matrixData 2-D array in double (row major)
	 * @return the standard deviation vector in double (for each column)
	 */
	public static double[] getMatrixSTD(double[][] matrixData) {
		int nCol = matrixData[0].length;
		
		double[] arraySTD = new double[nCol];
		double matTrans[][] = getTransposeMatrix(matrixData);
		// the row of the transpose matrix = the column of the matrix
		for(int row = 0; row < matTrans.length; row++) {
			arraySTD[row] = getSTD(matTrans[row]);
			
		}
		return arraySTD;
	}
	
	/**
	 * Return the standard deviation vector of the 2-D matrix (row major)
	 * 
	 * @param matrixData 2-D array in double (row major)
	 * @return the standard deviation vector in float (for each column)
	 */
	public static float[] getMatrixSTD(float[][] matrixData) {
		int nCol = matrixData[0].length;
		
		float[] arraySTD = new float[nCol];
		float matTrans[][] = getTransposeMatrix(matrixData);
		// the row of the transpose matrix = the column of the matrix
		for(int row = 0; row < matTrans.length; row++) {
			arraySTD[row] = getSTD(matTrans[row]);
			
		}
		return arraySTD;
	}
	
	/**
	 * Given a 2-D matrix (n-by-m) in double, returns the transpose matrix (m-by-n)
	 * 
	 * @param matrixInput 2-D array in double
	 * @return transpose matrix of the input
	 */
	public static double[][] getTransposeMatrix(double[][] matrixInput) {
		int nRow = matrixInput.length;
		int nCol = matrixInput[0].length;
		
		double[][] matrixOutput = new double[nCol][nRow];
		for(int row = 0; row < nRow; row++) {
			for(int col = 0; col < nCol; col++) {
				matrixOutput[col][row] = matrixInput[row][col];
			}
		}
		return matrixOutput;
	}


	/**
	 * Given a 2-D matrix (n-by-m) in float, returns the transpose matrix (m-by-n)
	 * 
	 * @param matrixInput 2-D array in float
	 * @return transpose matrix of the input
	 */
	public static float[][] getTransposeMatrix(float[][] matrixInput) {
		int nRow = matrixInput.length;
		int nCol = matrixInput[0].length;
		
		float[][] matrixOutput = new float[nCol][nRow];
		for(int row = 0; row < nRow; row++) {
			for(int col = 0; col < nCol; col++) {
				matrixOutput[col][row] = matrixInput[row][col];
			}
		}
		return matrixOutput;
	}
	
	/**
	 * Given the 2-D array with variant length in the second dimension, returns the largest dimension
	 * 
	 * @param matrixInput 2-D array in double with variant length in the second dimension
	 * @return number of the largest dimension
	 */
	public static int getMaxDimension(double[][] matrixInput) {
		int maxDim = 1;
		for(double[] row : matrixInput) {
			int dim = row.length;
			if(dim > maxDim) {
				maxDim = dim;
			}
		}
		return maxDim;
	}
	
	/**
	 * Given the 2-D array with variant length in the second dimension, returns the largest dimension
	 * 
	 * @param matrixInput 2-D array in int with variant length in the second dimension
	 * @return number of the largest dimension
	 */
	public static int getMaxDimension(int[][] matrixInput) {
		int maxDim = 1;
		for(int[] row : matrixInput) {
			int dim = row.length;
			if(dim > maxDim) {
				maxDim = dim;
			}
		}
		return maxDim;
	}
	
	/**
	 * Given a full matrix, returns an index matrix for it
	 * 
	 * @param matrixInput 2-D array in double (full matrix, can include zeros)
	 * @return index array 
	 */
	public static int[][] getIdxArray(double[][] matrixInput) {
		int nRow = matrixInput.length;
		int nCol = matrixInput[0].length;
		int[][] arrayOut = new int[nRow][nCol];
		for(int row = 0; row < nRow; row++) {
			for(int col = 0; col < nCol; col++) {
				arrayOut[row][col] = col;
			}
		}
		return arrayOut;
	}

	/**
	 * Given an array and item in the same type, check whether the array contains the item
	 * (not for primitive types)
	 * 
	 * @param array search domain in any type (not for primitive type)
	 * @param item target in any type
	 * @return false if not contains
	 */
	public static <T> boolean contains(T[] array, T item) {
		boolean result = false;
		
		for(T element : array) {
			if(element==item || element != null && element.equals(item)) {
				result = true;
			}
		}
		
		return result;
	}
	
	/**
	 * Given an array and item both in int, check whether the item is in the array
	 * 
	 * @param array search domain in int
	 * @param item target item in int
	 * @return false if the item is not in the array
	 * 
	 */
	public static boolean contains(int[] array, int item) {
		HashSet<Integer> set = new HashSet<Integer> ();
		for(int i = 0; i < array.length; i++) {
			set.add(array[i]);
		}
		return set.contains(item);
		
	}
	
	/**
	 * Given a the sparse matrix and index array, return a condense one 
	 * 
	 * @param matrixSparse 2-D array in double, whose length of the second dimension is variant
	 * @param matrixIndex 2-D array in int, which indicates the index of non-zero items in the sparse matrix
	 * @return full matrix in 2-D double array
	 */
	public static double[][] getFullMatrix(double[][] matrixSparse, int[][] matrixIndex) {
		int nRow = matrixSparse.length;
		int nCol = getMaxDimension(matrixSparse);	
		double[][] matrixDense = new double[nRow][nCol];
		for(int row = 0; row < nRow; row++) {
			// note: libsvm index starts from 1 rather than 0
			int[] arrayIndex = matrixIndex[row];
			for(int col = 0; col < nCol; col++) {
				int colLibsvm = col + 1;
				boolean isNonZero = contains(arrayIndex, colLibsvm);
				double item = 0;
				if(isNonZero) {					
					int idx = Arrays.binarySearch(arrayIndex, colLibsvm);
					item = matrixSparse[row][idx];
				}
				matrixDense[row][col] = item;
			}
		}
		
		return matrixDense;
	}
	
	/**
	 * Given a full matrix, which may contain zeros, return the index of non-zero items
	 * 
	 * @param matrixFull 2-D full matrix in double,  
	 * @return non-zero index array 
	 */
	public static int[][] getSparseIndex(double[][] matrixFull) {
		List<List<Integer>> listSparseIndex = new ArrayList<List<Integer>>();
		int nRow = matrixFull.length;
		int nCol = getMaxDimension(matrixFull);	
		for(int row = 0; row < nRow; row++) {
			List<Integer> listIdxRow = new ArrayList<Integer>();
			
			double item;
			for(int col = 0; col < nCol; col++) {
				item = matrixFull[row][col];
				if(item != 0.0) {
					// note: libsvm index starts from 1 rather than 0
					listIdxRow.add(col+1);
				}
				
			}
			listSparseIndex.add(listIdxRow);
		}
		
		return convertList2TwoDarray(listSparseIndex);
	}
	
	/**
	 * Given the full matrix and sparse (only for non-zero items) index array, prune the zeros
	 * 
	 * @param matrixFull 2-D array in double, may contains zeros
	 * @param matrixSparseIndex 2-D array in int, which indicates the index of non-zero items in the full matrix
	 * @return sparse version of the matrix
	 */
	public static double[][] getSparseMatrix(double[][] matrixFull, int[][] matrixSparseIndex) {
		List<List<Double>> listSparse = new ArrayList<List<Double>>();
		
		int nRow = matrixFull.length;
		for(int row = 0; row < nRow; row++) {
			int[] indexRow = matrixSparseIndex[row];		
			List<Double> listRow = new ArrayList<Double>();
			
			int col;
			for(int idxLibsvm : indexRow) {
				col = idxLibsvm - 1;
				listRow.add(matrixFull[row][col]);
			}
			listSparse.add(listRow);
		}
		
		return convertList2TwoDarray(listSparse);
	}

    /**
     * Given the arrays of ground truth (answer) and predicted label, 
     * compute the accuracy.
     * 
     * @param groundTruth 1-D label array in int
     * @param predictedLabel 1-D predicted label array in int
     * @return accuracy in float
     */
    public static float getAccuracy(int[] groundTruth, int[] predictedLabel) {
    	float accuracy = 0;
    	int nRow = groundTruth.length;
		int TP = 0;

//		System.out.println("Computing accuracy");
		for (int i = 0; i < nRow; i++) {
//			System.out.printf("GT:%d, Predicted:%d\n", groundTruth[i], predictedLabel[i]);
			if (groundTruth[i] == predictedLabel[i]) {
//				System.out.println("++");
				++TP;
			}
		}
		
		accuracy = (float) TP / (float)nRow;
//		System.out.printf("TP:%d, nRow:%d, accuracy:%.2f\n", TP, nRow, accuracy);
		return accuracy;
    }

}
