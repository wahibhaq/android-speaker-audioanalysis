package edu.sinica.citi.mac;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileOperationMethods {

	public FileOperationMethods() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Given the file list (with any length), concatenate the content,
	 * and write the aggregated results into another file.
	 * 
	 * @param fileOut the output file
	 * @param filesInput the input files, supported with variable length of files
	 * @return false if something wrong
	 */
	public static boolean concatFiles(String fileOut, String... filesInput) {
		boolean isSuccessed = true;
		try {
			FileWriter fw = new FileWriter(fileOut);
			BufferedWriter bw = new BufferedWriter(fw);
			for(String file: filesInput) {

				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String item;

				while((item = br.readLine()) != null) {
					bw.write(item + "\n");
				}		
			}
			bw.close();
			fw.close();
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			isSuccessed = false;
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			isSuccessed = false;
		}
		return isSuccessed;
	}
	
	/**
	 * Given the path of libsvm file, split it into X (full matrix), Y, and Index list
	 * 
	 * @param fileLibsvm the path of libsvm file
	 * @param listY the 1-D label list in int (use null if there is not label column)
	 * @param listX the 2-D feature list in double
	 * @param listIndex the 2-D (non-zero) index list in int
	 * @return false if something is wrong
	 */
	public static boolean getListXYI(String fileLibsvm, List<Integer> listY, List<List<Double>> listX, List<List<Integer>> listIndex) {
		boolean result = true;
		
		//initialize the 2-D list (necessary)
		for (List<Double> S: listX){
			S = new ArrayList<Double>();
		}
		/*
		 * Load the raw data into lists
		 */
		try {
			FileReader fr = new FileReader(fileLibsvm);
			BufferedReader br = new BufferedReader(fr);
			
			String sRow;
			while((sRow = br.readLine())!= null){
				String[] listItem = sRow.split(" ");
				List<Double> record = new ArrayList<Double>();
				List<Integer> rowIdx = new ArrayList<Integer>();
			    int idxInitX = 0;
				if(listY != null) {
					// load Y
					String Y = listItem[0];
					idxInitX = 1;

					Y = Y.replace("+", "");
					listY.add(Integer.parseInt(Y));
				}

				// load X (the first is Y, not X)
				for (int i = idxInitX; i < listItem.length; i++){
					String dataPair = listItem[i];
					String index = dataPair.split(":")[0];
					String value = dataPair.split(":")[1];

					record.add(Double.parseDouble(value));
					rowIdx.add(Integer.parseInt(index));
				}
				
				listX.add(record);
				listIndex.add(rowIdx);
			}
			br.close();
			fr.close();		
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			result = false;
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			result = false;
			e.printStackTrace();
		}

		return result;		
	}
	
	/**
	 * Given the label and feature list, output a libsvm matrix in String,
	 * whose format is [label] [idx1]:[value1] [idx2]:[values2] ... [idxM]:[idxM]
	 * (for full matrix)
	 * 
	 * @param arrayY 1-D array in int for label
	 * @param arrayX 2-D array in float for feature
	 * @return libsvm String list for the libsvm format data
	 */
	public static String[] convertArray2libsvm(int[] arrayY, float[][] arrayX) {
		List<String> listRecord = new ArrayList<String>();
		// integrate Y and X
		for(int row = 0; row < arrayY.length; row++) {
			int Y = arrayY[row];
			float[] Xrow = arrayX[row];
			System.out.printf("length of X[%d]:%d\n", row, Xrow.length);
			String sRecord = Integer.toString(Y);//for concatenating Y and Xs 
			for(int col = 0; col < Xrow.length; col++) {
				String sIdx = Integer.toString(col+1);
				String sValue = Float.toString(Xrow[col]);
				if(!sValue.equals(0))
					sRecord = sRecord + " " + sIdx + ":" + sValue;
			}	
			listRecord.add(sRecord);
		}
		// convert to array
		String[] arrayOut = MathMethods.convertList2arrayString(listRecord);

		return arrayOut;
	}
	
	/**
	 * Given the label and feature list, output a libsvm matrix in String,
	 * whose format is [label] [idx1]:[value1] [idx2]:[values2] ... [idxM]:[idxM]
	 * (for full matrix)
	 * 
	 * @param arrayY 1-D array in int for label
	 * @param arrayX 2-D array in double for feature
	 * @return libsvm String list for the libsvm format data
	 */
	public static String[] convertArray2libsvm(int[] arrayY, double[][] arrayX) {
		List<String> listRecord = new ArrayList<String>();
		// integrate Y and X
		for(int row = 0; row < arrayY.length; row++) {
			int Y = arrayY[row];
			double[] Xrow = arrayX[row];
			String sRecord = Integer.toString(Y);//for concatenating Y and Xs 
			for(int col = 0; col < Xrow.length; col++) {
				String sIdx = Integer.toString(col+1);
				String sValue = Double.toString(Xrow[col]);
				if(!sValue.equals(0))
					sRecord = sRecord + " " + sIdx + ":" + sValue;
			}	
			listRecord.add(sRecord);
		}
		// convert to array
		String[] arrayOut = MathMethods.convertList2arrayString(listRecord);

		return arrayOut;
	}
	
	/**
	 * Given the label, feature, and index list, output a libsvm matrix in String,
	 * whose format is [label] [idx1]:[value1] [idx2]:[values2] ... [idxM]:[idxM]
	 * (for sparse matrix)
	 * 
	 * @param arrayY 1-D array in int for label
	 * @param arrayX 2-D array in double for feature
	 * @param arrayIndex 2-D array in int for the index of non-zero items in X
	 * @return libsvm String list for the libsvm format data
	 */
	public static String[] convertArray2libsvm(int[] arrayY, double[][] arrayX, int[][] arrayIndex) {
		List<String> listRecord = new ArrayList<String>();
		// integrate Y and X
		for(int row = 0; row < arrayY.length; row++) {
			int Y = arrayY[row];
			String sRecord = Integer.toString(Y);//for concatenating Y and Xs 
			for(int col = 0; col < arrayX[row].length; col++) {
				String sIdx = Integer.toString(arrayIndex[row][col]);
				String sValue = Double.toString(arrayX[row][col]);
				sRecord = sRecord + " " + sIdx + ":" + sValue;
			}	
			listRecord.add(sRecord);
		}
		// convert to array
		String[] arrayOut = MathMethods.convertList2arrayString(listRecord);

		return arrayOut;
	}
	
	/**
	 * Given the label and feature list, integrate them into libsvm format, and write them into a file
	 * 
	 * @param arrayY 1-D array in int for label
	 * @param arrayX 2-D array in float for feature (full matrix)
	 * @param fileOut the output path
	 */
	public static void writeData2libsvm(int[] arrayY, float[][] arrayX, String fileOut) {
		String[] arrayLibsvm = convertArray2libsvm(arrayY, arrayX);
		
		try {
			FileWriter fw = new FileWriter(fileOut);
			BufferedWriter bw = new BufferedWriter(fw);
						
			for(int row = 0; row < arrayLibsvm.length; row++) {
				bw.write(arrayLibsvm[row] + "\n");
			}
			
			bw.close();
			fw.close();
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	/**
	 * Given the label, feature, and index list, integrate them into libsvm format, and write them into a file
	 * (for full feature matrix)
	 * 
	 * @param arrayY 1-D array in int for label
	 * @param arrayX 2-D array in double for feature
	 * @param arrayIndex 2-D array in int for the index of non-zero items in X
	 * @param fileOut the output path
	 */
	public static void writeData2libsvm(int[] arrayY, double[][] arrayX, String fileOut) {		
		String[] arrayLibsvm = convertArray2libsvm(arrayY, arrayX);

		try {
			FileWriter fw = new FileWriter(fileOut);
			BufferedWriter bw = new BufferedWriter(fw);
						
			for(int row = 0; row < arrayLibsvm.length; row++) {
				bw.write(arrayLibsvm[row] + "\n");
			}
			
			bw.close();
			fw.close();
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/**
	 * Given the label, feature, and index list, integrate them into libsvm format, and write them into a file
	 * (for sparse feature matrix)
	 * 
	 * @param arrayY 1-D array in int for label
	 * @param arrayX 2-D array in double for feature
	 * @param arrayIndex 2-D array in int for the index of non-zero items in X
	 * @param fileOut the output path
	 */
	public static void writeData2libsvm(int[] arrayY, double[][] arrayX, int[][] arrayIndex, String fileOut) {		
		String[] arrayLibsvm = convertArray2libsvm(arrayY, arrayX, arrayIndex);

		try {
			FileWriter fw = new FileWriter(fileOut);
			BufferedWriter bw = new BufferedWriter(fw);
						
			for(int row = 0; row < arrayLibsvm.length; row++) {
				bw.write(arrayLibsvm[row] + "\n");
			}
			
			bw.close();
			fw.close();
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
//	/**
//	 * Given a file that put one item in one row, returns an array for the item list
//	 * 
//	 * @param fileInput item list file, whose content should be integer
//	 * @return the list array in int
//	 */
//	public static int[] loadListFile(String fileInput) {
//		List<Integer> listItem = new ArrayList();
//		try {
//			FileReader fr = new FileReader(fileInput);
//			BufferedReader br = new BufferedReader(fr);
//			
//			String item;
//			while((item = br.readLine()) != null) {
//				// remove the sign (+)
//				item = item.replace("+", "");
//				listItem.add(Integer.parseInt(item));
//			}
//		}
//		catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		return MathMethods.convertList2array(listItem);
//	}
	
//	/**
//	 * Given a file that put one item in one row, returns an array for the item list
//	 * 
//	 * @param fileInput item list file, whose content should be integer
//	 * @return the list array in double
//	 */
//	public static double[] loadListFile(String fileInput) {
//		List<Double> listItem = new ArrayList();
//		try {
//			FileReader fr = new FileReader(fileInput);
//			BufferedReader br = new BufferedReader(fr);
//			
//			String item;
//			while((item = br.readLine()) != null) {
//				// remove the sign (+)
//				item = item.replace("+", "");
//				listItem.add(Double.parseDouble(item));
//			}
//		}
//		catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		return MathMethods.convertList2array(listItem);
//	}
	
	/**
	 * Given a file that put one item in one row, returns an array for the item list
	 * 
	 * @param fileInput item list file, whose content should be integer
	 * @return the list array in int
	 */
	public static int[] loadListFile(String fileInput) {
		List<Integer> listItem = new ArrayList();
		try {
			FileReader fr = new FileReader(fileInput);
			BufferedReader br = new BufferedReader(fr);
			
			String item;
			while((item = br.readLine()) != null) {
				// remove the sign (+)
				item = item.replace("+", "");
				listItem.add(Integer.parseInt(item));
			}
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return MathMethods.convertList2array(listItem);
	}
	
	/**
	 * Given a csv file and the delimiter, returns a 2-D array in double
	 * (full matrix, all in double)
	 * 
	 * @param fileInput path of csv file
	 * @param delimiter delimiter char
	 * @return 2-D array in double (full matrix)
	 */
	public static double[][] csvread(String fileInput, char delimiter) {
		/* Load into 2-D list */
		List<List<Double>> matrixOut = new ArrayList<List<Double>>();
		try {
			FileReader fr = new FileReader(fileInput);
			BufferedReader br = new BufferedReader(fr);
			
			String sRow;
			while((sRow = br.readLine()) != null) {
				List<Double> arrayOut = new ArrayList<Double>();
				String[] arrayItem = sRow.split(Character.toString(delimiter));
				for(String item : arrayItem) {
					arrayOut.add(Double.parseDouble(item));
				}
				matrixOut.add(arrayOut);
			}
			br.close();
			fr.close();
			
		/* Convert to 2-D array */
		return MathMethods.convertList2TwoDarray(matrixOut);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Given the 2-D data array in double and the delimiter, write them into a csv file
	 * 
	 * @param fileOutput path of csv output file
	 * @param matrixInput 2-D array in double, should be a full matrix
	 * @param delimiter delimiter char
	 */
	public static void csvwrite(String fileOutput, double[][] matrixInput, char delimiter) {
		
		try {
			FileWriter fw = new FileWriter(fileOutput);
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(double[] arrayRow : matrixInput) {
				List<String> arrayOut = new ArrayList<String>();
				for(double item : arrayRow) {
					arrayOut.add(Double.toString(item));
				}
						
				// Remove the '[' ,']', and commas; then replace the default delimiter with the one we assign
				String sOut = arrayOut.toString().replaceAll("[\\s\\[\\]]", "").replaceAll("[,]", Character.toString(delimiter));
				//Write (don't forget the newline character!)
				fw.write(sOut + '\n');			
			}
			bw.close();
			fw.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Given the 2-D data array in float and the delimiter, write them into a csv file
	 * 
	 * @param fileOutput path of csv output file
	 * @param matrixInput 2-D array in float, should be a full matrix
	 * @param delimiter delimiter char
	 */
	public static void csvwrite(String fileOutput, float[][] matrixInput, char delimiter) {
		
		try {
			FileWriter fw = new FileWriter(fileOutput);
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(float[] arrayRow : matrixInput) {
				List<String> arrayOut = new ArrayList<String>();
				for(float item : arrayRow) {
					arrayOut.add(Double.toString(item));
				}
						
				// Remove the '[' ,']', and commas; then replace the default delimiter with the one we assign
				String sOut = arrayOut.toString().replaceAll("[\\s\\[\\]]", "").replaceAll("[,]", Character.toString(delimiter));
				//Write (don't forget the newline character!)
				fw.write(sOut + '\n');			
			}
			bw.close();
			fw.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Given the 2-D data array in int and the delimiter, write them into a csv file
	 * 
	 * @param fileOutput path of csv output file
	 * @param matrixInput 2-D array in int, should be a full matrix
	 * @param delimiter delimiter char
	 */
	public static void csvwrite(String fileOutput, int[][] matrixInput, char delimiter) {
		
		try {
			FileWriter fw = new FileWriter(fileOutput);
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(int[] arrayRow : matrixInput) {
				List<String> arrayOut = new ArrayList<String>();
				for(int item : arrayRow) {
					arrayOut.add(Integer.toString(item));
				}
						
				// Remove the '[' ,']', and commas; then replace the default delimiter with the one we assign
				String sOut = arrayOut.toString().replaceAll("[\\s\\[\\]]", "").replaceAll("[,]", Character.toString(delimiter));
				//Write (don't forget the newline character!)
				fw.write(sOut + '\n');			
			}
			bw.close();
			fw.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Write a record into csv file
	 * 
	 * @param fileOutput path of csv output file
	 * @param arrayInput record in a 1-D double array
	 * @param delimiter delimiter char
	 */
	public static void csvwrite(String fileOutput, double[] arrayInput, char delimiter) {
		double[][] matInput = new double[1][arrayInput.length];
		matInput[0] = arrayInput;
		csvwrite(fileOutput, matInput, delimiter);
	}
	
	/**
	 * Write a record into csv file
	 * 
	 * @param fileOutput path of csv output file
	 * @param arrayInput record in a 1-D float array
	 * @param delimiter delimiter char
	 */
	public static void csvwrite(String fileOutput, float[] arrayInput, char delimiter) {
		float[][] matInput = new float[1][arrayInput.length];
		matInput[0] = arrayInput;
		csvwrite(fileOutput, matInput, delimiter);
	}
	
	/**
	 * Write a record into csv file
	 * 
	 * @param fileOutput path of csv output file
	 * @param arrayInput record in a 1-D int array
	 * @param delimiter delimiter char
	 */
	public static void csvwrite(String fileOutput, int[] arrayInput, char delimiter) {
		int[][] matInput = new int[1][arrayInput.length];
		matInput[0] = arrayInput;
		csvwrite(fileOutput, matInput, delimiter);
	}
	
	/**
	 * Given the path of a directory and the extension, returns the files match your query in a File list
	 * <br>
	 * For example: 
	 * 		getFileList("./somedir/", "mp3");
	 * 
	 * @param directoryName path of the directory, can be multilevel
	 * @param fileExtension the extension you want (case insensitive)
	 * @return all the files in List
	 */
	public static List<File> getFileList(String directoryName, String fileExtension) {
		List<File> fileList = new ArrayList<File>();
		File fileDirectory = new File(directoryName);	
		getDirectoryContent(fileList, fileDirectory, fileExtension);
		
		return fileList;
	}
	
	/**
	 * Given the path of a directory and the extension list, returns the files match your query in a File list
	 * <br>
	 * For example: 
	 * 		getFileList("./somedir/", ["mp3", "wav"]);
	 * 
	 * @param directoryName path of the directory, can be multilevel
	 * @param arrayFileExt the extension you want (case insensitive)
	 * @return all the files in List
	 */
	public static List<File> getFileList(String directoryName, String[] arrayFileExt) {
		List<File> fileList = new ArrayList<File>();
		File fileDirectory = new File(directoryName);	
		getDirectoryContent(fileList, fileDirectory, arrayFileExt);
		
		return fileList;
	}
	
	/**
	 * Store a file in another directory with exactly the same structure, changing the extension,
	 * and returns the new file path
	 * 
	 * @param dirOut the path of new directory 
	 * @param fileInput the file path of your original file
	 * @param extNew extension (not '.' required)
	 * @return
	 */
	public static String setOutFilePath(String dirOut, String fileInput, String extNew) {	
		String[] token = fileInput.split("\\.");
		/* Split the path to replace the extension to extNew */
		StringBuffer songName = new StringBuffer();
		for(int i = 0;i<token.length-1;i++) {
			songName.append(token[i] + '.');
		}
		songName.append(extNew);
		
		/* Reuse token array for directory */
		token = fileInput.split("/");
		StringBuffer subDir = new StringBuffer();
		for (int i = 1;i<token.length-1;i++) {
			subDir.append(token[i] + "/");
		}
		File tmp = new File(dirOut+ "/" + subDir );

		if (!tmp.exists()) {
			tmp.mkdirs();
		}
		
		return dirOut + songName;
	}
	
	/**
	 * Make sure every directory is exist
	 */
	public static void checkDir(String strFile) {	
		int idx = Math.max(strFile.lastIndexOf("/"), strFile.lastIndexOf("\\"));
		File tmpDir = new File(strFile.substring(0,idx));
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
	}

	public static void deleteDirectory(File path) {
	    if(path.exists()) {
	        File[] files = path.listFiles();
	        for(int i=0; i<files.length; i++) {
	           if(files[i].isDirectory()) {
	             deleteDirectory(files[i]);
	           } else {
	             files[i].delete();
	           }
	        }
	      }
	}
	
	/**
	 * Given the path of a directory and the extension, returns the files match your query in a File list
	 * <br>
	 * For example: 
	 * 		getFileList("./somedir/", "mp3");
	 * 
	 * @param directoryName path of the directory, can be multilevel
	 * @param fileExt the extension you want (case insensitive)
	 * @return all the files in List
	 */
	private static void getDirectoryContent(List<File> fileList, File fileDirectory, String fileExt) {
		String[] arrayFileExt = {fileExt};
		getDirectoryContent(fileList, fileDirectory, arrayFileExt);
	}
	
	private static void getDirectoryContent(List<File> fileList, File fileDirectory, String[] arrayFileExt) {
		if(fileDirectory.exists()) {
			try {
				for(File file: fileDirectory.listFiles()) {
					if(file.isDirectory()) {
						getDirectoryContent(fileList, file, arrayFileExt);
					} else {
						String fileName = file.getName();					
						/* skip the .XXX.ext files on OSX */					
						if(!fileName.startsWith(".")) {
							for(String fileExt: arrayFileExt) {
								if(fileName.endsWith(fileExt.toLowerCase()) || fileName.endsWith(fileExt.toUpperCase()))
									fileList.add(file);
							}
						}
					} // end of else
				} // end of for
			} catch(Exception e) {
				System.out.println("error at: " + fileDirectory);
			}	
		}
	}
	
	
	

}
