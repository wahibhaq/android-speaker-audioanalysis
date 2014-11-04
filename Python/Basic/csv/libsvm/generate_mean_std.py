'''
Created on Oct 24, 2014

@author: wahib
'''
from __future__ import print_function


import csv
import numpy as np
from sklearn import preprocessing
import pandas as pd


print(__doc__)

parentFolder = '1000rows'

fullData = pd.read_csv(parentFolder + '/Xtr.csv', delimiter=",",header=-1, dtype=np.float16)



shape = fullData.shape
print('size of Xtr data ', shape)



meanData = fullData.ix[:,:len(fullData.columns) - 1].astype('float16').mean()
stdData = fullData.ix[:,:len(fullData.columns) - 1].astype('float16').std()

meanData = preprocessing.scale(meanData)
stdData = preprocessing.scale(stdData)
print('scaling-normalization over')

with open(parentFolder+'/mean_tr.csv', 'w') as FOUT:
    np.savetxt(FOUT, np.atleast_2d(meanData) ,fmt='%1.5f' , delimiter=',', newline='')


with open(parentFolder + '/std_tr.csv', 'w') as FOUT:
    np.savetxt(FOUT, np.atleast_2d(stdData), fmt='%1.5f' , delimiter=',', newline='')


print('mean and std csv files created')
   
