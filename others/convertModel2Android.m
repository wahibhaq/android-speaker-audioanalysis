clear;
clc;

fileAndroid = './modelAndroid';
fileMatlab  = './modelMatlab.mat';

svmTypeStr = {' c_svc ', ' nu_svc ', ' one_class ', ' epsilon_svr ', ' nu_svr '};
kernelTypeStr = {' linear ', ' polynomial ', ' rbf ', ' sigmoid ', ' precomputed '};
load(fileMatlab);

fout = fopen(fileAndroid, 'wb');
idxSVM    = model.Parameters(1) + 1;
idxKernel = model.Parameters(2) + 1;
degree    = model.Parameters(3);
gamma     = model.Parameters(4);
coef0     = model.Parameters(5);

fprintf(fout, 'svm_type %s\n', svmTypeStr{idxSVM});
fprintf(fout, 'kernel_type %s\n', kernelTypeStr{idxKernel});
if (idxKernel ~=1), fprintf(fout, 'gamma %g\n', gamma);end
fprintf(fout, 'nr_class %d\n', model.nr_class);
fprintf(fout, 'total_sv %d\n', model.totalSV);
fprintf(fout, 'rho %g\n', model.rho);

fprintf(fout, 'label ');
%label_list = model.Label;
for label = model.Label;
    fprintf(fout, '%d ', label);
end
fprintf(fout, '\n');

if isfield(model, 'probA')
    %probA & probB
    fprintf(fout, 'probA %g\n', model.ProbA);
    fprintf(fout, 'probB %g\n', model.ProbB);
end
%nr_sv
fprintf(fout, 'nr_sv ');
nSV_list = model.nSV;
for nSV = model.nSV
    fprintf(fout, '%d ', nSV);
end
fprintf(fout, '\n');
%SV (sv_coef + SVs)
fprintf(fout, 'SV\n');
SVs = full(model.SVs);
for i = 1:model.totalSV
    fprintf(fout, '%g ', model.sv_coef(i));
    SV = SVs(i,:);
    for idxItem = 1:length(SV)
        item = SV(idxItem);
        if(item ~= 0)
            fprintf(fout, '%d:%g ', idxItem, item);
        end
    end
    fprintf(fout, '\n');
end
fclose(fout);
