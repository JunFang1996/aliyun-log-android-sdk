LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := aliyun_log_c_sdk
LOCAL_SRC_FILES := libs/$(TARGET_ARCH_ABI)/libaliyun_log_c_sdk.a
LOCAL_EXPORT_CFLAGS := -I$(LOCAL_PATH)/libs/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES :=
LOCAL_MODULE := sls_producer
LOCAL_SRC_FILES :=  HttpTool.cpp com_aliyun_sls_android_producer_LogProducerClient.c com_aliyun_sls_android_producer_LogProducerConfig.c sls_android_log.c sls_android_http_inject.c
LOCAL_STATIC_LIBRARIES := libaliyun_log_c_sdk
LOCAL_LDLIBS := -lz -llog -landroid
#增加16KB页面对齐参数
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
include $(BUILD_SHARED_LIBRARY)
