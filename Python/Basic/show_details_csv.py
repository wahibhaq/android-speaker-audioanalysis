'''
Created on 18 Oct, 2014

This is to show full shape of one csv file

@author: wahib

'''
from __future__ import print_function
import numpy as np
import csv
import array


# reading in all data into a NumPy array
all_data = np.loadtxt(open("csv/200mels.csv","r"),
    delimiter=",",
    skiprows=0,
    dtype=np.float16
    )

data = all_data[0:]

print('Full Data : ', data.shape)
