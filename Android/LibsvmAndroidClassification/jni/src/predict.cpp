#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <math.h>
#include "log.h"
#include "predict.h"
#include "./svm/svm-predict.h"

#define LOG_TAG "PREDICT"

int predict(double **values, int **indices, int rowNum, int *colNum, int isProb,
        const char *modelFile, int *labels, double** prob_estimates) {
    LOGD("Coming into classification\n");
    // located in svm-predict.cpp
    return svmpredict(values, indices, rowNum, colNum, isProb, modelFile, labels, prob_estimates);
}
