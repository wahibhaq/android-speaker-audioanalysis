fileAndroid = './modelAndroid';
fileMatlab  = './modelMatlab.mat';


svmTypeStr = {' c_svc ', ' nu_svc ', ' one_class ', ' epsilon_svr ', ' nu_svr '};
kernelTypeStr = {' linear ', ' polynomial ', ' rbf ', ' sigmoid ', ' precomputed '};


%% output the mat file
% Load the Android file (text)
fin = fopen(fileAndroid, 'rb');
model = struct();
Parameters = zeros(5,1);
%% Set parameters to default
Parameters(1) = 0;
Parameters(2) = 2;
Parameters(3) = 3;
Parameters(4) = 0;
Parameters(5) = 0;
% svm_type
tline = fgets(fin);
[~, svm_type] = strtok(tline);

%     param[1] = svm_type
%     param[2] = kernel_type
%     param[3] = degree % only appears in polynomial kernel
%     param[4] = gamma % in poly and rbf
%     param[5] = coef0 % in poly

% kernel_type
tline = fgets(fin);
[~, kernel_type] = strtok(tline);
Parameters(2) = find(ismember(kernelTypeStr, kernel_type)) - 1;
%% the number of lines in the following depends on the kernel type
if strcmp(kernel_type, 'polynomial')
    tline = fgets(fin);
    [~, degree] = strtok(tline);
    Parameters(3) = str2num(degree);
end
if (strcmp(kernel_type, 'rbf') || strcmp(kernel_type, 'polynomial'))
    tline = fgets(fin);
    [~, gamma] = strtok(tline);
    Parameters(4) = str2num(degree);
end

if strcmp(kernel_type, 'polynomial')
    tline = fgest(fin);
    [~, coef0] = strtok(tline);
    Parameter(5) = str2num(coef0);
end
% number of class
tline = fgets(fin);
[~, nC] = strtok(tline);
nr_class = str2num(nC);
% number of support vector
tline = fgets(fin);
[~, nSV] = strtok(tline);
totalSV = str2num(nSV);
% rho
tline = fgets(fin);
[~, rho] = strtok(tline);
rho = str2num(rho);
% label list
tline = fgets(fin);
[~, strLabels] = strtok(tline);
labelCell_list = regexp(strLabels, ' ', 'split');
label_list = [];
for labelCell = labelCell_list
    label_list = [label_list;str2num(cell2mat(labelCell))];
end
Label = label_list;
% probA
tline = fgets(fin);
[~, probA] = strtok(tline);
ProbA = str2num(probA);
% probB
tline = fgets(fin);
[~, probB] = strtok(tline);
ProbB = str2num(probB);
% nr_sv
tline = fgets(fin);
[~, strNsv] = strtok(tline);
nSvCell_list = regexp(strNsv, ' ', 'split');
nSv_list = [];
for nSvCell = nSvCell_list
    nSv_list = [nSv_list;str2num(cell2mat(nSvCell))];
end
nSV = nSv_list;

% support vectors row by row
% get the original data first
tline = fgets(fin); % skip the identifier "SV"
tline = fgets(fin);
SVorigin = {};
while ischar(tline)
    SVorigin = [SVorigin;tline];
    tline = fgets(fin);
end

% check the matrix dimension
maxIdx = 1;
for i = 1:length(SVorigin)
    SVrow = SVorigin{i};
    item_list = regexp(SVrow, ' ', 'split');
    lastItem = item_list{size(item_list,2)-1};
    [idx, ~] = strtok(lastItem, ':');
    idx = str2num(idx);
    if idx > maxIdx
        maxIdx = idx;
    end
end

% get sv_coef (the first column) and SVs (the rest)
sv_coef = zeros(length(SVorigin),1);
SVs = zeros(length(SVorigin), maxIdx);
for row = 1:length(SVorigin)
    SVrow = SVorigin{row};
    item_list = regexp(SVrow, ' ', 'split');
    sv_coef(row) = str2num(cell2mat(item_list(1)));
    for j = 2:length(item_list)
        item = item_list(j); % idx:value
        [col, value] = strtok(item, ':');
        col = str2num(cell2mat(col));
        value = cell2mat(value);
        value = str2num(value(2:end));
        SVs(row, col) = value;
    end
end

Parameters(4) = 1/maxIdx;
% write into the model
model.Parameters = Parameters;
model.nr_class = nr_class;
model.totalSV = totalSV;
model.rho = rho;
model.Label = Label;
model.ProbA = ProbA;
model.ProbB = ProbB;
model.nSV = nSV;
model.sv_coef = sv_coef;
model.SVs = sparse(SVs);


fclose(fin);
% output
save(fileMatlab, 'model');




