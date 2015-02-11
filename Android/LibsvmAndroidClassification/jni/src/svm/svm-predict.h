/*
 * Copyright (C) 2011 http://www.csie.ntu.edu.tw/~cjlin/libsvm/
 * Ported by likunarmstrong@gmail.com
 */

#ifndef SVM_PREDICT_H
#define SVM_PREDICT_H

int svmpredict(double **values, int **indices, int rowNum, int *arrayNcol,
        int isProb, const char *modelFile, int *labels, double **prob_estimates);

#endif
