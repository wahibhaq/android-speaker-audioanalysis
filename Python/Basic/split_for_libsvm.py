'''
Created on Oct 24, 2014

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


print(__doc__)

trainTargetArray = []
trainDataArray = []
fullData = pd.read_csv('csv/1percent_of_200mels.csv', delimiter=",",skiprows=0, dtype=np.float16)



shape = fullData.shape
print('size of full data ', shape)

trainData = fullData.iloc[:,:-1] #all except last column
trainTarget = fullData.iloc[:,-1] # only last column


rows = random.sample(trainData.index, 100) #splitting into just 100 rows
trainData = trainData.ix[rows]
trainTarget = trainTarget.ix[rows]

print('target size', trainTarget.shape)
#print('target values', trainTarget)

trainData = np.array(trainData)
trainTarget = np.array(trainTarget)
trainTarget = np.squeeze(trainTarget)
#print(trainTarget)
#print(trainData)

trainData = preprocessing.scale(trainData)
print('scaling-normalization over')

# Split the dataset in two equal parts
X_train, X_test, y_train, y_test = train_test_split(
    trainData, trainTarget, test_size=0.2, random_state=123)
print('X_train : ', X_train.shape)
print('y_train : ', y_train.shape)
print('X_test : ', X_test.shape)
print('y_test : ', y_test.shape)


with open('csv/libsvm/Ytr.txt', 'w') as FOUT:
    np.savetxt(FOUT, y_train ,fmt='%d',delimiter=',')


with open('csv/libsvm/Xtr.csv', 'w') as FOUT:
    np.savetxt(FOUT, X_train, fmt='%1.5f',delimiter=',')


with open('csv/libsvm/Xte.csv', 'w') as FOUT:
    np.savetxt(FOUT, X_test, fmt='%1.5f',delimiter=',')

with open('csv/libsvm/Yte.txt', 'w') as FOUT:
    np.savetxt(FOUT, y_test, fmt='%d',delimiter=',')


print('train and test csv files created')
   
