#include <jni.h>
#include <string.h>
#include <stdio.h>
#include "log.h"
#include "train.h"
#include "./svm/svm-train.h"

#define LOG_TAG "TRAIN"
#define PARA_LEN 12 // Max length for each parameter

/*
 * this file has been modified in order to accept more parameters
 *
 * Yuan-Ching Spencer Teng
 * Update: May 9 2014
 */
int train(int svmType, int kernelType, double degree, double gamma, double coef0, double cost, double nu,
		double epsilonSVR, int cacheSize, double epsilon, int shrinking, int isProb, double weightCsvc, int nFold,
		const char* trainingFile, const char *modelFile) {
	/*
	 * native LIBSVM receive parameters as string, use char* rather than numeric types
	 * int cmdLen = 1 + the others (char*=>1, the others => 2)
	 *
	 * Short example of the native LIBSVM train:
	 * $svm-train -t 0 -c 1 modelFile
	 * which needs string with cmdLen = 1 + 5 ([-t, 0, -c, 1, modelFile]) = 6
	 */

   	//__android_log_print(ANDROID_LOG_ERROR, "STATUS", "train.cpp train() 1-D array");
   	LOGD("Status : train.cpp train() 1-D array");

    int cmdLen = 31;
    char *cmd[cmdLen];

    cmd[0] = "donotcare";

    cmd[1] = "-s";
    cmd[2] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[2], "%d", svmType); // Too lazy to implement a itoa ^_^

    cmd[3] = "-t";
    cmd[4] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[4], "%d", kernelType);

    cmd[5] = "-d";
    cmd[6] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[6], "%.5f", degree);

    cmd[7] = "-g";
    cmd[8] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[8], "%.5f", gamma);

    cmd[9] = "-r";
    cmd[10] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[10], "%.5f", coef0);

    cmd[11] = "-c";
    cmd[12] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[12], "%.5f", cost);

    cmd[13] = "-n";
    cmd[14] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[14], "%.5f", nu);

    cmd[15] = "-p";
    cmd[16] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[16], "%.5f", epsilonSVR);

    cmd[17] = "-m";
    cmd[18] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[18], "%d", cacheSize);

    cmd[19] = "-e";
    cmd[20] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[20], "%.5f", epsilon);

    cmd[21] = "-h";
    cmd[22] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[22], "%d", shrinking);

    cmd[23] = "-b";
    cmd[24] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[24], "%d", isProb);

    cmd[25] = "-wi";
    cmd[26] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[26], "%.5f", weightCsvc);

    // if -v [<2] => disable
    if(nFold >= 2) {
    	cmd[27] = "-v";
    	cmd[28] = (char *)calloc(PARA_LEN, sizeof(char));
    	sprintf(cmd[28], "%d", nFold);
    }
    else{ // repeat a flag setting rather than including
    	  // the trainFile and ModelFile setting in this if-else closure
        cmd[27] = "-b";
        cmd[28] = (char *)calloc(PARA_LEN, sizeof(char));
        sprintf(cmd[28], "%d", isProb);
    }
    // set the strings
    int len = strlen(trainingFile);
    cmd[29] = (char *)calloc(len+1, sizeof(char));
    strncpy(cmd[29], trainingFile, len);
    cmd[29][len] = '\0';

    len = strlen(modelFile);
    cmd[30] = (char *)calloc(len+1, sizeof(char));
    strncpy(cmd[30], modelFile, len);
    cmd[30][len] = '\0';

    int result = svmtrain(cmdLen, cmd);

    // Clean up space (those with even index)
    for (int i = 2; i < cmdLen; i+=2) {
        free(cmd[i]);
        cmd[i] = NULL;
    }
    // Clean up space (this is the only one with odd index)
    free(cmd[29]);
    cmd[29] = NULL;

    return result;
}

// 2-D double array as the training data input
int train(int svmType, int kernelType, double degree, double gamma, double coef0, double cost, double nu,
		double epsilonSVR, int cacheSize, double epsilon, int shrinking, int isProb, double weightCsvc, int nFold,
		double **arrayXtr, int *arrayYtr, int **arrayIdx, int nRow, int *arrayNcol, const char *modelFile) {
	/*
	 * native LIBSVM receive parameters as string, use char* rather than numeric types
	 * int cmdLen = 1 + the others (char*=>1, the others => 2)
	 *
	 * Short example of the native LIBSVM train:
	 * $svm-train -t 0 -c 1 modelFile
	 * which needs string with cmdLen = 1 + 5 ([-t, 0, -c, 1, modelFile]) = 6
	 */

   	//__android_log_print(ANDROID_LOG_ERROR, "STATUS", "train.cpp train() 2-D array");
   	LOGD("STATUS : train.cpp train() 2-D array");

    int cmdLen = 30;
    char *cmd[cmdLen];

    cmd[0] = "donotcare";

    cmd[1] = "-s";
    cmd[2] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[2], "%d", svmType); // Too lazy to implement a itoa ^_^

    cmd[3] = "-t";
    cmd[4] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[4], "%d", kernelType);

    cmd[5] = "-d";
    cmd[6] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[6], "%.5f", degree);

    cmd[7] = "-g";
    cmd[8] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[8], "%.5f", gamma);

    cmd[9] = "-r";
    cmd[10] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[10], "%.5f", coef0);

    cmd[11] = "-c";
    cmd[12] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[12], "%.5f", cost);

    cmd[13] = "-n";
    cmd[14] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[14], "%.5f", nu);

    cmd[15] = "-p";
    cmd[16] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[16], "%.5f", epsilonSVR);

    cmd[17] = "-m";
    cmd[18] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[18], "%d", cacheSize);

    cmd[19] = "-e";
    cmd[20] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[20], "%.5f", epsilon);

    cmd[21] = "-h";
    cmd[22] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[22], "%d", shrinking);

    cmd[23] = "-b";
    cmd[24] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[24], "%d", isProb);

    cmd[25] = "-wi";
    cmd[26] = (char *)calloc(PARA_LEN, sizeof(char));
    sprintf(cmd[26], "%.5f", weightCsvc);

    // if -v [<2] => disable
    if(nFold >= 2) {
    	cmd[27] = "-v";
    	cmd[28] = (char *)calloc(PARA_LEN, sizeof(char));
    	sprintf(cmd[28], "%d", nFold);
    }
    else{ // repeat a flag setting rather than including
    	  // the trainFile and ModelFile setting in this if-else closure
        cmd[27] = "-b";
        cmd[28] = (char *)calloc(PARA_LEN, sizeof(char));
        sprintf(cmd[28], "%d", isProb);
    }

    int len = strlen(modelFile);
    cmd[29] = (char *)calloc(len+1, sizeof(char));
    strncpy(cmd[29], modelFile, len);
    cmd[29][len] = '\0';

    int result = svmtrain(cmdLen, cmd, arrayXtr, arrayYtr, arrayIdx, nRow, arrayNcol);

    // Clean up space (those with even index)
    for (int i = 2; i < cmdLen; i+=2) {
        free(cmd[i]);
        cmd[i] = NULL;
    }
    // Clean up space (this is the only one with odd index)
    free(cmd[29]);
    cmd[29] = NULL;

    return result;
}

