'''
Created on Oct 24, 2014

@author: wahib
'''
from __future__ import print_function


import csv
import numpy as np
from sklearn import preprocessing
import pandas as pd
import sys

print(__doc__)

#folderName = '10000rows'

def generateMeanStd(folderName):

	fullData = pd.read_csv(folderName + '/Xtr.csv', delimiter=",",header=-1, dtype=np.float16)



	shape = fullData.shape
	print('size of Xtr data ', shape)



	meanData = fullData.ix[:,:len(fullData.columns) - 1].astype('float16').mean()
	stdData = fullData.ix[:,:len(fullData.columns) - 1].astype('float16').std()

	meanData = preprocessing.scale(meanData)
	stdData = preprocessing.scale(stdData)
	print('scaling-normalization over')

	with open(folderName+'/mean_tr.csv', 'w') as FOUT:
	    np.savetxt(FOUT, np.atleast_2d(meanData) ,fmt='%1.5f' , delimiter=',', newline='')


	with open(folderName + '/std_tr.csv', 'w') as FOUT:
	    np.savetxt(FOUT, np.atleast_2d(stdData), fmt='%1.5f' , delimiter=',', newline='')


	print('mean and std csv files created')

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('folder name/path is missing e.g 1000rows/')
    else:
        folderName    = sys.argv[1]
        generateMeanStd(folderName)   
