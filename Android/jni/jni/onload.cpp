#include <string.h>
#include <assert.h>
#include "jni.h"
#include "../src/log.h"

namespace LibsvmJNI {
    int register_library(JNIEnv* env);
};

using namespace LibsvmJNI;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("GetEnv failed!");
        return result;
    }
    assert(env != NULL);
    if (!register_library(env)) {
        LOGE("ERROR: Test native registration failed");
        return result;
    }
    /* success -- return valid version number */
    return JNI_VERSION_1_4;
}
