#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


LOCAL_MODULE_TAGS := optional
LOCAL_STATIC_JAVA_LIBRARIES := fastjson
LOCAL_STATIC_JAVA_LIBRARIES += xutils3 
LOCAL_STATIC_JAVA_LIBRARIES += mqttv3
LOCAL_SRC_FILES := $(call all-java-files-under, java)

LOCAL_PROGUARD_ENABLED:= disabled  
LOCAL_PACKAGE_NAME := TH_RadioPlayer
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
	fastjson:libs/fastjson-1.1.68.android.jar \
	xutils3:libs/xutils3.jar \
	mqttv3:libs/org.eclipse.paho.client.mqttv3-1.1.1.jar 

include $(BUILD_MULTI_PREBUILT)
include $(call all-makefiles-under,$(LOCAL_PATH))
