'''
Created on 18 Oct, 2014

@author: wahib
'''
from __future__ import print_function
from sklearn.cross_validation import train_test_split
from sklearn import preprocessing
import pandas as pd
import numpy as np
import csv
import array

#all_data = pd.read_csv('200mels.csv')
# reading in all data into a NumPy array except the last column

all_data = np.loadtxt(open("csv/1percent_of_200mels.csv","r"),
    delimiter=",",
    skiprows=0,
    dtype=np.float16,
    usecols=range(0,36)
    )

# load rows & columns features
#X_data = all_data[:,1:]
#Y_data = all_data[:,0]

# printing some general information about the data
#print('\ntotal number of samples (rows):', X_data.shape[0])
#print('\ntotal number of samples (columns):', Y_data.shape[0])

#X_data = np.array(X_data)
#print('X_data shape : ', X_data.shape)

#Y_data = np.array(Y_data)
#print('Y_data shape : ', Y_data.shape)


#X_train, X_test, y_train, y_test = train_test_split(X_data, Y_data,
#   test_size=0.01, random_state=123)

data = all_data[0:]

print('Full Data : ',data.shape)

data_train, data_test = train_test_split(data,
   test_size=0.10, random_state=123)

#print(X_train.shape)
#print(y_train.shape)
#print(X_test.shape)
#print(y_test.shape)

print('Test Data : ' , data_test.shape)
print('Train Data : ' , data_train.shape)

'''
with open('onepercent_mels.csv', 'w') as fp:
    a = csv.writer(fp, delimiter=',')
    for rownum in range(0,36):
        a.writerows(X_test[rownum])
'''


with open('csv/1percent_trainfile.csv', 'w') as FOUT:
	np.savetxt(FOUT, data_train ,fmt='%1.5f',delimiter=',')


with open('csv/1percent_testfile.csv', 'w') as FOUT:
	np.savetxt(FOUT, data_test, fmt='%1.5f',delimiter=',')


print('train and test csv files created')
    
