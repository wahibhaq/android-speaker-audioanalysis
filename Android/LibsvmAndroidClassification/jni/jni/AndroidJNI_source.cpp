#include <jni.h>
#include <string.h>
#include "../src/log.h"
#include "../src/predict.h"
#include "../src/train.h"

namespace LibsvmJNI {
//training data in a libsvm file
static jint trainClassifier_libsvm(JNIEnv *env, jobject obj, jint svmType, jint kernelType,
		jdouble degree, jdouble gamma, jdouble coef0, jdouble cost, jdouble nu, jdouble epsilonSVR,
		jint cacheSize, jdouble epsilon, jint shrinking, jint isProb, jdouble weightCsvc, jint nFold,
		jstring trainingFileS, jstring modelFileS) {

    jboolean isCopy;

    const char *trainingFile = env->GetStringUTFChars(trainingFileS, &isCopy);
    const char *modelFile = env->GetStringUTFChars(modelFileS, &isCopy);

    int v = train(svmType, kernelType, degree, gamma, coef0, cost, nu,
    		epsilonSVR, cacheSize, epsilon, shrinking, isProb, weightCsvc, nFold,
    		trainingFile, modelFile);
//    int v = train(trainingFile, kernelType, cost, gamma, isProb, modelFile);

    env->ReleaseStringUTFChars(trainingFileS, trainingFile);
    env->ReleaseStringUTFChars(modelFileS, modelFile);

    //added for fixing : JNI ERROR (app bug): local reference table overflow (max=512)
    env->DeleteLocalRef(trainingFileS);
    env->DeleteLocalRef(modelFileS);



    return v;
}

// support training data in arrays
static jint trainClassifier_array(JNIEnv *env, jobject obj, jint svmType, jint kernelType,
		jdouble degree, jdouble gamma, jdouble coef0, jdouble cost, jdouble nu, jdouble epsilonSVR,
		jint cacheSize, jdouble epsilon, jint shrinking, jint isProb, jdouble weightCsvc, jint nFold,
		jobjectArray XtrainArr, jintArray YtrainArr, jobjectArray IdxTrainArr, jstring modelFileS) {
    jboolean isCopy;

   	//__android_log_print(ANDROID_LOG_ERROR, "STATUS", "AndroidJNI_source.cpp trainClassifier_array()");
   	LOGD("STATUS : AndroidJNI_source.cpp trainClassifier_array()");

    const char *modelFile = env->GetStringUTFChars(modelFileS, &isCopy);

    /* Initialization */
    int nRow = env->GetArrayLength(XtrainArr);
    int nCol;
    // 1-D (label)
    int *arrayYtr = env->GetIntArrayElements(YtrainArr, NULL);
    int *arrayNcol = (int *)calloc(nRow, sizeof(int));
    // 2-D (feature and index)
    double **arrayXtr = (double **)calloc(nRow, sizeof(double *));
    int **arrayIdxTr = (int **)calloc(nRow, sizeof(int *));

    /* Load the feature */
    for(int row = 0; row < nRow; row++) {

    	// Load from the argument
    	jdoubleArray Xrow = (jdoubleArray)env->GetObjectArrayElement(XtrainArr, row);
    	jintArray idxRow = (jintArray)env->GetObjectArrayElement(IdxTrainArr, row);

    	// Get the real accessible arrays
    	jdouble *arrayXtrJni = env->GetDoubleArrayElements(Xrow, NULL);
    	jint *arrayIdxTrJni = env->GetIntArrayElements(idxRow, NULL);

    	// Compute the column size of this row
    	nCol = env->GetArrayLength(Xrow);
    	arrayNcol[row] = nCol;

    	// Copy from the jni types to native primitive types
    	arrayXtr[row] = (double *)calloc(nCol, sizeof(double));
    	arrayIdxTr[row] = (int *)calloc(nCol, sizeof(int));

    	for(int col = 0; col < nCol; col++) {
    		arrayXtr[row][col] = arrayXtrJni[col];
    		arrayIdxTr[row][col] = arrayIdxTrJni[col];
    	}

    	// Release the arrays
    	env->ReleaseDoubleArrayElements(Xrow, arrayXtrJni, JNI_ABORT);
    	env->ReleaseIntArrayElements(idxRow, arrayIdxTrJni, JNI_ABORT);

        //added for fixing : JNI ERROR (app bug): local reference table overflow (max=512)
    	env->DeleteLocalRef(Xrow);
    	env->DeleteLocalRef(idxRow);

    }

    /* Call the interface function */
    int v = train(svmType, kernelType, degree, gamma, coef0, cost, nu,
    		epsilonSVR, cacheSize, epsilon, shrinking, isProb, weightCsvc, nFold,
    		arrayXtr, arrayYtr, arrayIdxTr, nRow, arrayNcol, modelFile);

    /* Release (deallocate) the memory */
    for(int row = 0; row < nRow; row ++) {
    	free(arrayXtr[row]);
    }

    env->ReleaseIntArrayElements(YtrainArr, arrayYtr, 0);
    env->ReleaseStringUTFChars(modelFileS, modelFile);

    //added for fixing : JNI ERROR (app bug): local reference table overflow (max=512)
    env->DeleteLocalRef(YtrainArr);
    env->DeleteLocalRef(modelFileS);

    return v;
}

static jint doClassification(JNIEnv *env, jobject obj, jobjectArray valuesArr,
        jobjectArray indicesArr, jint isProb, jstring modelFiles,
        jintArray labelsArr ,jobjectArray probsArr) {

   	LOGD("STATUS : AndroidJNI_source.cpp doClassification()");

    jboolean isCopy;

    const char *modelFile = env->GetStringUTFChars(modelFiles, &isCopy);
    int *labels = env->GetIntArrayElements(labelsArr, NULL);

    // initiate the arrays
    int nRow = env->GetArrayLength(valuesArr);
    double **values = (double **)calloc(nRow, sizeof(double *));
    int **indices = (int **)calloc(nRow, sizeof(int *));
    double **probs = (double **)calloc(nRow, sizeof(double *));
    int *arrayNcol = (int *)calloc(nRow, sizeof(int));

    int colNum = 0;
//    int colNum = 12;
    for (int i = 0; i < nRow; i++) {

        jdoubleArray vrows = (jdoubleArray)env->GetObjectArrayElement(valuesArr, i);
        jintArray irows = (jintArray)env->GetObjectArrayElement(indicesArr, i);
        jdoubleArray prows = (jdoubleArray) env->GetObjectArrayElement(probsArr, i);

        jdouble *velement = env->GetDoubleArrayElements(vrows, NULL);
        jint *ielement = env->GetIntArrayElements(irows, NULL);

        colNum = env->GetArrayLength(vrows);

        arrayNcol[i] = colNum;
        // the probs array is a reference for getting the values back, not for data input
        probs[i] = env->GetDoubleArrayElements(prows, NULL);
        values[i] = (double *)calloc(colNum, sizeof(double));
        indices[i] = (int *)calloc(colNum, sizeof(int));

        for (int j = 0; j < colNum; j++) {
            values[i][j] = velement[j];
            indices[i][j] = ielement[j];
        }

        env->ReleaseDoubleArrayElements(vrows, velement, JNI_ABORT);
        env->ReleaseIntArrayElements(irows, ielement, JNI_ABORT);
        env->ReleaseDoubleArrayElements(prows, probs[i], JNI_ABORT);

        //added for fixing : JNI ERROR (app bug): local reference table overflow (max=512)
         env->DeleteLocalRef(vrows);
         env->DeleteLocalRef(irows);
         env->DeleteLocalRef(prows);

    }

    int r = predict(values, indices, nRow, arrayNcol, isProb, modelFile, labels, probs);

    // release the multi-D arrays
    for (int i = 0; i < nRow; i++) {
        free(values[i]);
        free(indices[i]);
    }
    // release the one-D arrays and strings
    free(arrayNcol);
    env->ReleaseIntArrayElements(labelsArr, labels, 0);
    env->ReleaseStringUTFChars(modelFiles, modelFile);

    //added for fixing : JNI ERROR (app bug): local reference table overflow (max=512)
    env->DeleteLocalRef(labelsArr);
    env->DeleteLocalRef(modelFiles);

    return r;
}

static JNINativeMethod sMethods[] = {
    /* name, signature, funcPtr */
    {"svmpredict", "([[D[[IILjava/lang/String;[I[[D)I",
        (void*)doClassification},
    {"svmtrain", "(IIDDDDDDIDIIDILjava/lang/String;Ljava/lang/String;)I",
        (void*)trainClassifier_libsvm},
    {"svmtrain", "(IIDDDDDDIDIIDI[[D[I[[ILjava/lang/String;)I",
        (void*)trainClassifier_array},
};

static int jniRegisterNativeMethods(JNIEnv *env, const char *className,
        JNINativeMethod* Methods, int numMethods) {
    jclass clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }

    if (env->RegisterNatives(clazz, Methods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}


int register_library(JNIEnv *env) {
    return jniRegisterNativeMethods(env, "edu/sinica/citi/mac/android/actclassification/ActClassificationActivity",
            sMethods, sizeof(sMethods) / sizeof(sMethods[0]));
}

}


