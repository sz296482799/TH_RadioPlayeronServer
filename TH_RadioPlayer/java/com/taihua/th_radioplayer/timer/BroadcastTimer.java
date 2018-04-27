package com.taihua.th_radioplayer.timer;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.taihua.th_radioplayer.database.PlayerDB;
import com.taihua.th_radioplayer.domain.ReturnSetDataBroadcastOB;
import com.taihua.th_radioplayer.domain.ReturnSetOB;

import com.taihua.th_radioplayer.manager.AlarmManager;

public class BroadcastTimer {
	public static final int BROADCAST_TIMER_MSG = 0x8002;
	private Handler mHandler = null;
	private ArrayList<BroadcastItem> mBroadcastList = null;
	private boolean mIsSend = false;
	private static final String ALARM_NAME = "BroadcastTimer";
	
	public BroadcastTimer(Handler handler) {

		mBroadcastList = new ArrayList<BroadcastItem>();
		mHandler = handler;
	}
	
	public void init() {

		ReturnSetOB returnSetOB = PlayerDB.getInstance().getServerData();
		if(returnSetOB != null) {
			setBroadcastList(returnSetOB.getData().getBroadcast());
		}
	}

	private AlarmManager.TimeCallback mCallback = new AlarmManager.TimeCallback() {
		
        @Override
        public void onAlarm(Object privateData, boolean isStart) {
			touchBroadcast((BroadcastItem)privateData);
		}
	};

	private boolean removeBroadcast(BroadcastItem item) {
		if(item != null) {
			return AlarmManager.getInstance().unregisterAlarm(item.getAlarmID());
		}
		return false;
	}
	
	private void addBroadcast(ReturnSetDataBroadcastOB b) {

		String[] times = b.getBr_time().split(",");
		for (int i = 0; i < times.length; i++) {
			BroadcastItem item = new BroadcastItem(b);

			int weeks = 0;
			String[] weekList = b.getBr_date().split(",");
			for(int j = 0; j < weekList.length; j++) {
				int w = Integer.parseInt(weekList[j]);
				weeks |= (1 << w);
			}
			
			int alarmID = AlarmManager.getInstance().registerAlarm(ALARM_NAME, mCallback, weeks, Long.parseLong(times[i], 10) * 1000, -1, item);
			if(alarmID != -1) {
				item.setAlarmID(alarmID);
	        	mBroadcastList.add(item);
			}
		}
	}

	public void open() {
		mIsSend = true;
	}

	public void close() {
		mIsSend = false;
	}

    public void touchBroadcast(ReturnSetDataBroadcastOB b) {
        touchBroadcast(new BroadcastItem(b));
    }

    public void touchBroadcast(BroadcastItem item) {
        if(mIsSend && mHandler != null && item != null) {
            Message msg = new Message();
            msg.what = BROADCAST_TIMER_MSG;
            msg.obj = item;
            mHandler.sendMessage(msg);
        }
    }
	
	public void setBroadcastList(List<ReturnSetDataBroadcastOB> broadcastList) {
		
		for(BroadcastItem item : mBroadcastList) {
			removeBroadcast(item);
		}
		mBroadcastList.clear();
		for(ReturnSetDataBroadcastOB b : broadcastList) {
			addBroadcast(b);
		}
	}

	public static class BroadcastItem {
		private int mBroadcastID = -1;
		private int mPlayMode = -1;
		public int mRepeatNum = 0;
		private String mFileName = null;
		private String mFileUrl = null;
		private int mAlarmID = -1;
		
		public BroadcastItem(ReturnSetDataBroadcastOB b) {
			// TODO Auto-generated constructor stub
			
			mBroadcastID = b.getBr_id();
			mPlayMode = b.getBr_mode();
			mFileName = b.getBr_file_name();
			mFileUrl = b.getBr_file();
			mRepeatNum = b.getBr_repeat_num();
		}
		
		public int getBroadcastID() {
			return mBroadcastID;
		}
		
		public int getPlayMode() {
			return mPlayMode;
		}
		
		public String getFileName() {
			return mFileName;
		}
		
		public String getFileUrl() {
			return mFileUrl;
		}

		public void setAlarmID(int alarmID) {
			mAlarmID = alarmID;
		}

		public int getAlarmID() {
			return mAlarmID;
		}
	}
}
