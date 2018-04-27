package com.taihua.th_radioplayer.utils;

import java.util.ArrayList;
import java.util.List;

import android.os.IBinder;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.util.Log;

public class UsbUtils {

	public static List<String> getMountPathList() {
		List<String> pathList = new ArrayList<String>();
		
		try {
			
			IBinder service = ServiceManager.getService("mount");
			if (service != null) {
				IMountService mountService = IMountService.Stub.asInterface(service);
				List<android.os.storage.ExtraInfo> mountList = mountService.getAllExtraInfos();
				
				int index = mountList.size();

				for (int i = 0; i < index; i++) {
					LogUtil.d("UsbUtils", "path:" + mountList.get(i).mMountPoint);
					pathList.add(mountList.get(i).mMountPoint);
					/*
					path[i] = mountList.get(i).mMountPoint;
					if (mountList.get(i).mLabel != null) {
						label[i] = mountList.get(i).mDiskLabel + ": "
								+ mountList.get(i).mLabel;
					} else {
						label[i] = mountList.get(i).mDiskLabel;
					}
					partition[i] = label[i];
					String typeStr = mountList.get(i).mDevType;
					if (path[i].contains("/mnt/nand")) {
						type[i] = 3;
					} else if (typeStr.equals("SDCARD")) {
						type[i] = 3;
					} else if (typeStr.equals("SATA")) {
						type[i] = 2;
					} else if (typeStr.equals("USB2.0")) {
						type[i] = 0;
					} else if (typeStr.equals("USB3.0")) {
						type[i] = 1;
					} else if (typeStr.equals("UNKOWN")) {
						type[i] = 4;
					} else if (typeStr.equals("CD-ROM")) {
                        type[i] = 5;
                    }
					Log.d("MountInfo", "path:" + path[i]);
					Log.d("MountInfo", "type:" + type[i]);
					Log.d("MountInfo", "label:" + label[i]);
					Log.d("MountInfo", "partition:" + partition[i]);
					*/
				}
			}

		} catch (Exception e) {
			LogUtil.d("UsbUtils", "" + e);
		}
		return pathList;
	}
}