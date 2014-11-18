'''
Created on Oct 24, 2014

This is to split a csv dataset into fixed number of rows and then splitting that into training and testing

@author: wahib
'''
from __future__ import print_function

from sklearn import datasets
from sklearn.cross_validation import train_test_split
from sklearn.grid_search import GridSearchCV
from sklearn.metrics import classification_report
from sklearn import svm
import numpy as np
import csv
import pylab as pl
from sklearn.metrics import roc_curve, auc
from sklearn import preprocessing
import pandas as pd
import random
import sys

print(__doc__)

def splitForLibsvm(folderPath, csvPath, rowExtractCount):

	trainTargetArray = []
	trainDataArray = []

	#folderPath = '10000rows/'

	#fullData = pd.read_csv('csv/1percent_of_200mels.csv', delimiter=",",skiprows=0, dtype=np.float16)
	#fullData = pd.read_csv('csv/200mels.csv', delimiter=",",skiprows=0, dtype=np.float16)

	fullData = pd.read_csv(csvPath, delimiter=",",skiprows=0, dtype=np.float16)

	shape = fullData.shape
	print('size of full data ', shape)

	trainData = fullData.iloc[:,:-1] #all except last column
	trainTarget = fullData.iloc[:,-1] # only last column

	print('len of traindata', len(trainData))
	#print('print traindata', trainData)


	#only commented when full dataset needs to be used
	print('count of rows to extract', rowExtractCount)
	rows = random.sample(trainData.index,rowExtractCount)
	trainData = trainData.ix[rows]
	trainTarget = trainTarget.ix[rows]

	print('target size', trainTarget.shape)
	#print('target values', trainTarget)


	trainData = np.array(trainData)
	trainTarget = np.array(trainTarget)
	trainTarget = np.squeeze(trainTarget)
	#print(trainTarget)
	#print(trainData)

	#only commented for 200k dataset because it was nullifying all values
	trainData = preprocessing.scale(trainData)
	print('scaling-normalization over for trainData')

	# Split the dataset in two equal parts
	X_train, X_test, y_train, y_test = train_test_split(
	    trainData, trainTarget, test_size=0.2, random_state=123)
	print('X_train : ', X_train.shape)
	print('y_train : ', y_train.shape)
	print('X_test : ', X_test.shape)
	print('y_test : ', y_test.shape)


	#with open('csv/libsvm/'+folderPath+'/Ytr.txt', 'w') as FOUT:
	with open(folderPath+'/Ytr.txt', 'w') as FOUT:
	    np.savetxt(FOUT, y_train ,fmt='%d',delimiter=',')


	with open(folderPath+'/Xtr.csv', 'w') as FOUT:
	    np.savetxt(FOUT, X_train, fmt='%1.5f',delimiter=',')


	with open(folderPath+'/Xte.csv', 'w') as FOUT:
	    np.savetxt(FOUT, X_test, fmt='%1.5f',delimiter=',')

	with open(folderPath+'/Yte.txt', 'w') as FOUT:
	    np.savetxt(FOUT, y_test, fmt='%d',delimiter=',')


	print('train and test csv files created')


if __name__ == '__main__':
    if len(sys.argv) < 4:
        print('3 Arguments required i.e [folderPath] [csvPath] [rowExtractCount] ')
    else:
        folderPath = sys.argv[1]
        csvPath = sys.argv[2]
        rowExtractCount = sys.argv[3]
        splitForLibsvm(folderPath, csvPath, rowExtractCount)
