package com.taihua.th_radioplayer.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.os.storage.StorageManager;
import android.os.Handler;
import android.os.Message;

import com.taihua.th_radioplayer.utils.UsbUtils;
import com.taihua.th_radioplayer.utils.LogUtil;

public class MusicStorageManager {

	public static final int USB_MOUNT_CHANGE = 0x7000;

	public static final String TAG = "RadioStorageManager";

	private static final String MUSICS_DIR_NAME = "musics";
	private static final int MAX_DEPTH = 3;
	private static final String[] FILE_EXT = new String[] {"wma", "mp3"};
	
	private static MusicStorageManager mInstance;
	private Context mContext = null;

	private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
	private Handler mHandler;
	
	private MusicStorageManager() {

		mFilter = new IntentFilter();
		
        mFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		mFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		mFilter.addDataScheme("file");

        mReceiver = new BroadcastReceiver() {
			
            @Override
            public void onReceive(Context context, Intent intent) {
            LogUtil.d(TAG, "BroadcastReceiver");
            	if (Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())
					|| Intent.ACTION_MEDIA_REMOVED.equals(intent.getAction())
					|| Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction())) {
					LogUtil.d(TAG, "USB_MOUNT_CHANGE");
					sendMessage(USB_MOUNT_CHANGE, 0, 0, null);
				}
            }
        };

	}

	public static MusicStorageManager getInstance() {
		if (mInstance == null) {
			synchronized (MusicStorageManager.class) {
				if (mInstance == null) {
					mInstance = new MusicStorageManager();
				}
			}
		}
		return mInstance;
	}
	
	public MusicStorageManager init(Context context, Handler handler) {
		// TODO Auto-generated constructor stub
		
		mContext = context;
		mHandler = handler;
		context.registerReceiver(mReceiver, mFilter);
		
		return mInstance;
	}

	private void sendMessage(int what, int arg1, int arg2, Object obj) {
        if(mHandler != null) {
            Message msg = new Message();
            msg.what = what;
            msg.arg1 = arg1;
            msg.arg2 = arg2;
            msg.obj = obj;
            mHandler.sendMessage(msg);
        }
    }
	
	public String getSavePath() {
		if(mContext == null)
			return "";
		return mContext.getFilesDir().getPath() + "/" + MUSICS_DIR_NAME;
	}
	
	public boolean delFile(File file) {
		if(file.exists() && file.isFile()) {
			return file.delete(); 
		}
		return false;
	}
	
	public boolean delFile(String fileName) {
		return delFile(new File(fileName));
	}

	public USBNode getUSBFile(File f, String[] Extensions, int maxDepth) {
	    return getUSBFile(f.getAbsolutePath(), Extensions, maxDepth);
    }

    public USBNode getUSBFile(String path, String[] Extensions, int maxDepth) {

        if(path != null) {
            File file = new File(path);
            if(file.exists()) {
                USBNode node = new USBNode();
                node.setName(file.getName());
                node.setPath(file.getAbsolutePath());
                node.setType(file.isDirectory() ? USBNode.NODE_TYPE_DIR : USBNode.NODE_TYPE_FILE);
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    for (File f : files) {
                        if (f.isFile() && Extensions != null && Extensions.length > 0) {
                            for (String e : Extensions) {
                                if (e != null && f.getPath().substring(f.getPath().length() - e.length()).equals(e)) {
									LogUtil.d(TAG, "getUSBFile Find path:" + f.getAbsolutePath());
                                    node.addNode(getUSBFile(f, Extensions, maxDepth - 1));
                                    break;
                                }
                            }
                        }
                        else if(f.isDirectory() && f.getPath().indexOf("/.") == -1 && maxDepth >= 0)
                            node.addNode(getUSBFile(f, Extensions, maxDepth - 1));
                    }
                    if(node.size() == 0)
                        return null;
                    return node;
                }
                return node;
            }
        }
        return null;
    }
	
	public USBNode getRootFile(String[] Extensions) {

        List<String> paths = UsbUtils.getMountPathList();
        if(paths != null && paths.size() > 0) {
			
            USBNode node = new USBNode();
            node.setName("ROOT");
            node.setPath("");
            node.setType(USBNode.NODE_TYPE_DIR);

            for (String path : paths) {
				LogUtil.d(TAG, "getRootFile paths:" + path);
                node.addNode(getUSBFile(path, Extensions, MAX_DEPTH));
            }
            return node;
        }
        return null;
	}

	public USBNode getRadioFile() {
        return getRootFile(FILE_EXT);
	}
	
	static public class USBNode {

		public static final int NODE_TYPE_DIR = 0;
		public static final int NODE_TYPE_FILE = 1;
		
		private String mName = null;
		private int mType = -1;
		private ArrayList<USBNode> mList = null;
		private String mPath;
		
		public USBNode() {
			mName = "UNKNOW";
			mList = new ArrayList<USBNode>();
		}
		
		public void setName(String name) {
			mName = name;
		}
		
		public String getName() {
			return mName;
		}
		
		public void setType(int type) {
			mType = type;
		}
		
		public int getType() {
			return mType;
		}

        public void setPath(String path) {
            mPath = path;
        }

        public String getPath() {
            return mPath;
        }

        public int size() {
		    return mList.size();
        }

        public boolean addNode(USBNode node) {

            if(node == null)
                return false;
			return mList.add(node);
		}

		@Override
		public String toString() {
			return "USBNode [mPath=" + mPath
					+ ", mList=" + mList + "]";
		}
	}
}
