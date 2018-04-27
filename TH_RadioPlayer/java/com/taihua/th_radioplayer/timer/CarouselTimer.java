package com.taihua.th_radioplayer.timer;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.taihua.th_radioplayer.database.PlayerDB;
import com.taihua.th_radioplayer.domain.ReturnSetDataCarouselOB;
import com.taihua.th_radioplayer.domain.ReturnSetOB;

import com.taihua.th_radioplayer.manager.AlarmManager;

public class CarouselTimer {
	public static final int CAROUSEL_TIMER_MSG = 0x8001;
	private Handler mHandler = null;
	private ArrayList<CarouselItem> mCarouseList = null;
	private boolean mIsSend = false;
	private static final String ALARM_NAME = "CarouselTimer";
	
	public CarouselTimer(Handler handler) {

		mCarouseList = new ArrayList<CarouselItem>();
		mHandler = handler;
	}
	
	public void init() {

		ReturnSetOB returnSetOB = PlayerDB.getInstance().getServerData();
		if(returnSetOB != null) {
			setCarouselList(returnSetOB.getData().getCarousel());
		}
	}

	private AlarmManager.TimeCallback mCallback = new AlarmManager.TimeCallback() {
		
        @Override
        public void onAlarm(Object privateData, boolean isStart) {
			touchCarouse((CarouselItem)privateData, isStart);
		}
	};

	private boolean removeCarouse(CarouselItem item) {
		if(item != null) {
			return AlarmManager.getInstance().unregisterAlarm(item.getAlarmID());
		}
		return false;
	}

	private void addCarouse(ReturnSetDataCarouselOB c) {
		CarouselItem item = new CarouselItem(c);

		int weeks = 0;
		String[] weekList = c.getPlay_week().split(",");
		for(int j = 0; j < weekList.length; j++) {
			int w = Integer.parseInt(weekList[j]);
			weeks |= (1 << w);
		}
		
		int alarmID = AlarmManager.getInstance().registerAlarm(ALARM_NAME, mCallback, weeks, c.getCa_start_time() * 1000, c.getCa_end_time() * 1000, item);
		if(alarmID != -1) {
			item.setAlarmID(alarmID);
        	mCarouseList.add(item);
		}
	}

	public void touchCarouse(ReturnSetDataCarouselOB c, boolean isStart) {
        touchCarouse(new CarouselItem(c), isStart);
    }

    public void touchCarouse(CarouselItem item, boolean isStart) {
        if(mIsSend && mHandler != null && item != null) {

			item.setIsStart(isStart);
			
            Message msg = new Message();
            msg.what = CAROUSEL_TIMER_MSG;
            msg.obj = item;
            mHandler.sendMessage(msg);
        }
    }

	public void open() {
		mIsSend = true;
		CarouselItem item = (CarouselItem)AlarmManager.getInstance().getStartPrivateData(ALARM_NAME);
		if(item != null) {
			touchCarouse(item, true);
		}
	}

	public void close() {
		mIsSend = false;
	}
	
	public void setCarouselList(List<ReturnSetDataCarouselOB> carouselList) {
		for(CarouselItem item : mCarouseList) {
			removeCarouse(item);
		}
		mCarouseList.clear();
		for(ReturnSetDataCarouselOB c : carouselList) {
			addCarouse(c);
		}
	}
	
	public static class CarouselItem {
		private boolean mIsStart = false;
		private int mCarouselID = -1;
		private int mPacketID = -1;
		private int mChannelID = -1;
		private int mPlayMode = -1;
		private int mAlarmID = -1;
		
		public CarouselItem(ReturnSetDataCarouselOB c) {
			
			mCarouselID = c.getCa_id();
			mChannelID = c.getChannel_id();
			mPlayMode = c.getPlay_mode();
		}
		
		public boolean getIsStart() {
			return mIsStart;
		}

		public void setIsStart(boolean isStart) {
			mIsStart = isStart;
		}
		
		public int getCarouselID() {
			return mCarouselID;
		}
		
		public int getPacketID() {
			return mPacketID;
		}
		
		public int getChannelID() {
			return mChannelID;
		}
		
		public int getPlayMode() {
			return mPlayMode;
		}
		
		public void setAlarmID(int alarmID) {
			mAlarmID = alarmID;
		}

		public int getAlarmID() {
			return mAlarmID;
		}
	}
}
