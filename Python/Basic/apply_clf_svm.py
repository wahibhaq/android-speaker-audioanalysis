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
print('size', shape)

#print(fullData.iloc[:,1:15])

#trainData = fullData.iloc[:,0:36]
#trainTarget = fullData.iloc[:,36:37]

trainData = fullData.iloc[:,:-1] #all except last column
trainTarget = fullData.iloc[:,-1] # only last column

print('target size', trainTarget.shape)
print('target values', trainTarget)

trainData = np.array(trainData)
trainTarget = np.array(trainTarget)
trainTarget = np.squeeze(trainTarget)
#print(trainTarget)
#print(trainData)
#c=(trainTarget.view(np.ubyte)-96).astype('int32')
le = preprocessing.LabelEncoder()
le.fit(trainTarget)
trainTarget = le.transform(trainTarget)

print('data read over')
#print(trainTarget)

trainData = preprocessing.scale(trainData)
print('preprocessing over')
# Split the dataset in two equal parts
X_train, X_test, y_train, y_test = train_test_split(
    trainData, trainTarget, test_size=0.2, random_state=123)
print(X_train.shape)
print(y_train.shape)
print(X_test.shape)
print(y_test.shape)

clf = svm.LinearSVC()
#clf = svm.SVC(gamma=0.001, C=100) #this gives improved results

clf.fit(X_train,y_train)
#print('training over')
#print(le.inverse_transform(clf.predict(X_test)))
score = clf.score(X_test, y_test)
print('score')
print(score)

# Set the parameters by cross-validation

'''
tuned_parameters = [{
                     'C': [0.1,1]}]

scores = ['precision', 'recall']

for score in scores:
    print("# Tuning hyper-parameters for %s" % score)
    print()

    clf = GridSearchCV(svm.SVC( probability=True), tuned_parameters, cv=2, scoring=score)
    clf.fit(X_train, y_train)

    print("Best parameters set found on development set:")
    print()
    print(clf.best_estimator_)
    print()
    print("Grid scores on development set:")
    print()
    for params, mean_score, scores in clf.grid_scores_:
        print("%0.3f (+/-%0.03f) for %r"
              % (mean_score, scores.std() / 2, params))
    print()

    print("Detailed classification report:")
    print()
    print("The model is trained on the full development set.")
    print("The scores are computed on the full evaluation set.")
    print()
    y_true, y_pred = y_test, clf.predict(X_test)
    print(classification_report(y_true, y_pred))
    probas_ = clf.predict_proba(X_test)

    # Compute ROC curve and area the curve
    fpr, tpr, thresholds = roc_curve(y_test, probas_[:, 1])
    print("fpr:tpr")
    fpr.dump('fprdump')
    print(fpr.shape,tpr.shape)
    roc_auc = auc(fpr, tpr)
    print("Area under the ROC curve : %f" % roc_auc)
    
    # Plot ROC curve
    pl.clf()
    pl.plot(fpr, tpr, label='ROC curve (area = %0.2f)' % roc_auc)
    pl.plot([0, 1], [0, 1], 'k--')
    pl.xlim([0.0, 1.0])
    pl.ylim([0.0, 1.0])
    pl.xlabel('False Positive Rate')
    pl.ylabel('True Positive Rate')
    pl.title('Receiver operating characteristic example')
    pl.legend(loc="lower right")
    pl.savefig('roc'+score)
    print()
'''
