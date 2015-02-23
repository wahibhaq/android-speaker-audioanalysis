#ifndef Header_SuperpoweredFFT
#define Header_SuperpoweredFFT

void SuperpoweredFFTPrepare(int logSize, bool real);


void SuperpoweredFFTCleanup();


void SuperpoweredFFTComplex(float *real, float *imag, int logSize, bool forward);


void SuperpoweredFFTReal(float *real, float *imag, int logSize, bool forward);


void SuperpoweredPolarFFT(float *mag, float *phase, int logSize, bool forward);

#endif
