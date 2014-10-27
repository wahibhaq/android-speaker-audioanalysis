import numpy as np
from sklearn.svm import SVC
from matplotlib import pyplot as plt
import csv
import array
from sklearn.cross_validation import train_test_split

 
# Get the pseudo-data from the npy file
#npy_data = numpy.load('data.npy')

# reading in all data into a NumPy array except the last column
all_data = np.loadtxt(open("csv/1percent_of_200mels.csv","r"),
    delimiter=",",
    skiprows=0,
    dtype=np.float16,
    )

 


print('Full Data : ', all_data.shape)

data_train, data_test = train_test_split( all_data, test_size=0.10, random_state=123)

 
# Split numpy array into X (data) and Y (target)
# target is just last column
target = data_train[:,-1]

# data is all but last column
train = data_train[:,:-1]


print('Train Data : ' , train.shape)
print('Target Data : ' , target.shape)

clf = SVC(gamma=0.001, C=100)
clf.fit(train, target)
#print('clf = ', clf)

test = data_test[:,:-1]
print('Test Data : ' , test.shape)

output = clf.predict(test)
#print('output = ', output)

# The percent of correctly classified training and testing data
# should be roughly equivalent (i.e. not overtrained)
# and ideally, near 100%.  We will see about 90% success.
print('---------- Training/Testing info ----------')
print('Trained SVM correctly classifies')
print(100*(sum(clf.predict(test)))/test.shape[0])
print('%  of the testing data and ')
print(100*(sum(clf.predict(train)))/train.shape[0])
#print('%  of the training data. ' print(-*50)
