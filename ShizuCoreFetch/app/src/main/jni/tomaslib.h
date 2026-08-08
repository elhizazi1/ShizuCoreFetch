#ifndef TOMASLIB_H
#define TOMASLIB_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Returns a greeting string from TomasLib C++
 */
JNIEXPORT jstring JNICALL
Java_xyz_siwane_shizucorefetch_MainActivity_stringFromJNI(JNIEnv* env, jobject thiz);

/**
 * Adds two integers and returns the result
 */
JNIEXPORT jint JNICALL
Java_xyz_siwane_shizucorefetch_MainActivity_addNumbers(JNIEnv* env, jobject thiz, jint a, jint b);

/**
 * Says hello to a person with their name
 */
JNIEXPORT jstring JNICALL
Java_xyz_siwane_shizucorefetch_MainActivity_sayHello(JNIEnv* env, jobject thiz, jstring name);

/**
 * Initialize the TomasLib native library
 */
JNIEXPORT void JNICALL
Java_xyz_siwane_shizucorefetch_MainActivity_initTomasLib(JNIEnv* env, jobject thiz);

#ifdef __cplusplus
}
#endif

#endif // TOMASLIB_H