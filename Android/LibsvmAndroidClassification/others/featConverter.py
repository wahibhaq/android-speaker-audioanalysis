"""
Convert the feature matrix from MATLAB to LIBSVM format and vise versa

Input:
    src_file: path
    Y_file  : path
    out_file: path
    inputFormat: ['csv', 'libsvm']
Output:
    out_file: path

'csv': csv format, row by row
'libsvm': txt format, [idx]:[value], row by row

Yuan-Ching Teng 
June 6 2014
"""


import sys
import csv

def convert2libfm(X_file, Y_file, out_file):
    ## output -> LIBSVM (txt)
    fin = open(X_file, 'rb')
    feat_list = list(csv.reader(fin, delimiter=','))
    fin.close()

    fin = open(Y_file, 'rb')
    Y_list = fin.read().splitlines()
    fin.close()

    #print 'feat_list:', feat_list
    dim = len(feat_list[0])
    #print 'dim:', dim
    fout = open(out_file, 'wb')
    for idx, feat in enumerate(feat_list):
        Y = Y_list[idx]
        fout.write(Y + ' ')
        #print Y
        for i in range(0, dim):
            idx = str(i+1)
            fout.write(idx+':'+feat[i]+' ')
            #feat_revised = idx + ':' + feat[i] + ' '
        fout.write('\n')
    fout.close()
    print 'Done! Check', out_file

def getMaxLength(matrix):
    maxLen = 0;
    for feat in matrix:
        pair_list = feat.split(' ')
        # the first item is Y, not included in the data pairs
        rowLen = len(pair_list) - 1
        if rowLen > maxLen:
            maxLen =  rowLen
    return maxLen
def convert2matlab(src_file, Y_file, out_file):
    ## output -> MATLAB (csv)
    fin = open(src_file, 'rb')
    feat_list = fin.read().splitlines()
    fin.close()

    fout = open(Y_file, 'wb')
    revised_list = []
    rowMaxLen = getMaxLength(feat_list)
    for feat in feat_list:
        pair_list = feat.split(' ')
        Y = pair_list[0]
        fout.write(Y+'\n')
        revised_feat = []
        
        idx = 1
        for i in range(1, rowMaxLen+1):
            pair = pair_list[idx]
            if not pair:
                continue
            idxItem = int(pair.split(':')[0])
            item = pair.split(':')[1]
            if i != idxItem:
                item = 0
            else:
                idx = idx + 1
            revised_feat.append(item)
        revised_list.append(revised_feat)
    fout.close()
    #
    fout = open(out_file, 'wb')
    csvWriter = csv.writer(fout, delimiter=',')
    csvWriter.writerows(revised_list)
    fout.close()i
    print 'Done! Check', out_file, 'and', Y_file

    
def print_usage():
    print "python featConverter.py [src_fiie] [out_file] [Y_file] [inputFormat]"
    print "This script helps you convert svm data from csv files to libsvm files and vise versa"
    print "\nFour arguments are needed:"
    print "src_file: the source file you have, may in either csv or libsvm files"
    print "Y_file: label file, one row for one label; this may be input or output, depends on the function you select"
    print "out_file: the file you will get after the conversion"
    print "inputFormat: type csv or libsvm, we will give you the opposite"
    print "\n"
    print "note:"
    print "for inputFormat is libsvm, we will parse the label-feature-integrated libsvm file and split it into two files"
    print "on the other hand, if you type csv as the inputFormat, we will integrate the src_file and Y_file into the out_file"
if __name__ == '__main__':
    if len(sys.argv) < 5:
        print_usage()
    else:
        src_file    = sys.argv[1]
        Y_file      = sys.argv[2]
        out_file    = sys.argv[3]
        inputFormat = sys.argv[4]

        if inputFormat == 'csv':
            convert2libfm(src_file, Y_file, out_file)
        elif inputFormat == 'libsvm':
            convert2matlab(src_file, Y_file, out_file)
        


