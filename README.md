### Introduction

This project aims to help the research/industry community to use [LIBSVM] (http://www.csie.ntu.edu.tw/~cjlin/libsvm/) on Android platform as a native library for better performance (compared to the Java version).  LIBSVM is one of the most powerful tools used worldwide in many fields for classification and regression problems.

This project is actually an extension of [this project] (https://github.com/cnbuff410/Libsvm-androidjni).  Although there are some implementation in his work, some functions and parameters provided by the original (C/C++) source codes were not supported in his Android version.  Therefore, I extended his work to make it fully supportive and added several example codes dealing with several practical tasks the users might face.

#### *Please send me a message if I accidentally violate any license.  Thanks!*

### Compilation

Install Android NDK first  
https://developer.android.com/tools/sdk/ndk/index.html


Run the 'ndk_compile' script to compile the C/C++ source codes into callable library.

`sh ./ndk_compile`


In case you want to see how the NDK works  
http://spencerimp.blogspot.tw/2014/05/android-run-cc-codes-on-androidandroid.html

### Tutorial & usage
I assume you are familiar with Android programming already.  Please copy the file 'libsvm_data' directory under you phone's SD card first.  Then you should be able to compile and launch the project. We provide several examples (buttons on the panel) to illustrate how to use the core methods.  You can modify them to fit your case.

In those examples, you can find out how we perform training, prediction with different sources of input.  I do recommend you take a look at the **runExp()**
first.

The data input format can be:

1. libsvm lite (.libsvm) files
 
    [label] [idx1]:[value1] [idx2:value2] ... [idxN]:[valueN]

    This is exactly the input formant accepted by the binary excutable version. 
    
    * The label can be any number, which means multi-class classification is supported. 
    * The indices are those for non-zero terms (but you can also put value zero if you want).
    * Both the feature and label information are included in one file.


2. comma separate files (.csv) files

    We need a files for feature, and another one for label.

    Feature file:
    value1,value2,...,valueM

    Label file:
    label1
    label2
    .
    .
    .
    labelN

    This is the format widely used in the MATLAB (or others) version.  The feaure matrix should be condense (N-by-M), if there are some missing values, please replace them with zero.

3. Embedded (data are written directly in the codes)
    Although writting data in the codes are not feasible in most of the tasks, sometimes it is useful for validation/testing.

#### Hint:

1. You can use sparse representation similar to the libsvm format.  In this case, you define the non-zero items in the feature array, and define the indices in the index array.  Again, you can use zeros in the feature array, and declare all the indices if you prefer.  However, I would recommend you sparsify the matrix before using them

2. You can also enable 'offline training', which loads the pre-trained model (probably done on a PC), and the mean and stardard variation (if you applied normalization before training process) by enableing the member attribute m_OfflineTrain.

### Others
We also provide scriptsto help you transform feature or trained model files among different platforms:

1. featConverter.py
   This Python script helps you convert data from csv files to libsvm files and vise versa.

2. convertModel2Matlab.m
   This Matlab script helps you to convert the trained model gained from libsvm-Android (or the binary executable version) to the .mat file.

3. convertMatlab2Android.m
   This Matlab script helps you to convert the trained model gained from Matlab (.m) to the format libsvm-Android (or the binary executable version) use.

### Contact

Yuan-Ching Spencer Teng  
spencer.tengz@gmail.com  
done while in [MAC Lab] (http://mac.citi.sinica.edu.tw)   
June 7, 2014

