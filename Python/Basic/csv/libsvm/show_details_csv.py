'''
Created on 18 Oct, 2014

@author: wahib
'''
from __future__ import print_function
import numpy as np
import csv
import array
import sys


def showDetails():
	# reading in all data into a NumPy array
	all_data = np.loadtxt(open(folderName + "/Xtr.csv","r"),
	    delimiter=",",
	    skiprows=0,
	    dtype=np.float16
	    )

	data = all_data[0:]

	print('Training Data : ', data.shape)

	all_data = np.loadtxt(open(folderName + "/Xte.csv","r"),
	    delimiter=",",
	    skiprows=0,
	    dtype=np.float16
	    )

	data = all_data[0:]

	print('Testing Data : ', data.shape)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('folder name/path is missing e.g 1000rows/')
    else:
        folderName    = sys.argv[1]
        showDetails()