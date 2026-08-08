#include <jni.h>
#include <string>
#include "tomaslib.h"

extern "C" JNIEXPORT jstring JNICALL
Java_xyz_siwane_shizucorefetch_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from TomasLib C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_xyz_siwane_shizucorefetch_MainActivity_addNumbers(
        JNIEnv* env,
        jobject /* this */,
        jint a,
        jint b) {
    return a + b;
}

extern "C" JNIEXPORT void JNICALL
Java_xyz_siwane_shizucorefetch_MainActivity_initTomasLib(
        JNIEnv* env,
        jobject /* this */) {
    // Initialize TomasLib native library
    // Add your initialization code here
}