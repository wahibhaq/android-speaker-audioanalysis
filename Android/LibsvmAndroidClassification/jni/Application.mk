# To build all native files, run ndk-build in the ../eegpoc directory.

#APP_ABI := armeabi
#APP_OPTIM := release #original


APP_STL := gnustl_static
APP_CPPFLAGS := -frtti -fexceptions
APP_ABI := armeabi
APP_PLATFORM := android-14
APP_OPTIM := debug