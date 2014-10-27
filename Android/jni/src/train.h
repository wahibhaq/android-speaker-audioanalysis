/*
 * Copyright (C) 2011 http://www.csie.ntu.edu.tw/~cjlin/libsvm/
 * Ported by likunarmstrong@gmail.com
 */

#ifndef TRAIN_H
#define TRAIN_H

int train(int svmType, int kernelType, double degree, double gamma, double coef0, double cost, double nu,
		double epsilonSVR, int cacheSize, double epsilon, int shrinking, int isProb, double weightCsvc, int nFold,
		const char* trainingFile, const char *modelFile);

int train(int svmType, int kernelType, double degree, double gamma, double coef0, double cost, double nu,
		double epsilonSVR, int cacheSize, double epsilon, int shrinking, int isProb, double weightCsvc, int nFold,
		double **arrayXtr, int *arrayYtr, int **arrayIdx, int nRow, int *arrayNcol, const char *modelFile);
#endif
