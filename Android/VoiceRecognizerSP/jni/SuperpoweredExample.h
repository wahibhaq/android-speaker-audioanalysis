#ifndef Header_SuperpoweredExample
#define Header_SuperpoweredExample

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <math.h>
#include <pthread.h>
#include <jni.h>


#include "SuperpoweredExample.h"
#include "SuperpoweredAdvancedAudioPlayer.h"
#include "SuperpoweredFilter.h"
#include "SuperpoweredRoll.h"
#include "SuperpoweredFlanger.h"
#include "SuperpoweredMixer.h"
#include "SuperpoweredFFT.h"

#define NUM_BUFFERS 2
#define HEADROOM_DECIBEL 3.0f
static const float headroom = powf(10.0f, -HEADROOM_DECIBEL * 0.025);

class SuperpoweredExample {
public:

	SuperpoweredExample(const char *path, int *params);
	SuperpoweredExample();

	~SuperpoweredExample();

	void process(SLAndroidSimpleBufferQueueItf caller);
	void onPlayPause(bool play);
	void onCrossfader(int value);
	void onFxSelect(int value);
	void onFxOff();
	void onFxValue(int value);

	//void onFFT(float *real, float *imag);
	void onFFTReal(JNIEnv *env, jobject obj, jfloatArray real, jfloatArray imag, int logSize, bool ifForward);
	void onFFTPrepare(int logSize, bool ifReal);

private:
	SLObjectItf openSLEngine, outputMix, bufferPlayer;
	SLAndroidSimpleBufferQueueItf bufferQueue;

    SuperpoweredAdvancedAudioPlayer *playerA, *playerB;
    SuperpoweredRoll *roll;
    SuperpoweredFilter *filter;
    SuperpoweredFlanger *flanger;
    SuperpoweredStereoMixer *mixer;


    unsigned char activeFx;
    float crossValue, volA, volB;
    pthread_mutex_t mutex;

	float *outputBuffer[NUM_BUFFERS];
	int currentBuffer, buffersize;
};

#endif
