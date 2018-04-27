package com.taihua.th_radioplayer.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.*;

import com.taihua.th_radioplayer.utils.LogUtil;

public class AlarmManager {

	private static final String TAG = "AlarmManager";

    private static AlarmManager mInstance;

    public static final int WEEK_SUNDEY = 0x1;
    public static final int WEEK_MONDEY = 0x2;
    public static final int WEEK_TUESDEY = 0x4;
    public static final int WEEK_WEDNESDAT = 0x8;
    public static final int WEEK_THURSDAY = 0x10;
    public static final int WEEK_FRIDAY = 0x20;
    public static final int WEEK_SATURD = 0x40;

    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    private Timer mTimer;
    private AlarmTask mAlarmTask;
    private TimeItem mCurItem;
    private int mAlarmCount;

    private HashMap<Integer, ArrayList<TimeItem>> mWeekMap;
    private int[] mWeeks = {
            WEEK_SUNDEY,
            WEEK_MONDEY,
            WEEK_TUESDEY,
            WEEK_WEDNESDAT,
            WEEK_THURSDAY,
            WEEK_FRIDAY,
            WEEK_SATURD
    };

    private AlarmManager() {
        mWeekMap = new HashMap<Integer, ArrayList<TimeItem>>();
        for (int i = 0; i < mWeeks.length; i++) {
            mWeekMap.put(mWeeks[i], new ArrayList<TimeItem>());
        }
        mTimer = new Timer();
        mAlarmCount = 0;
		LogUtil.i(TAG, "AlarmManager curDate:" + new Date());
    }

    public static AlarmManager getInstance() {

        if (mInstance == null) {
            synchronized (AlarmManager.class) {
                if (mInstance == null) {
                    mInstance = new AlarmManager();
                }
            }
        }
        return mInstance;
    }

    public void init(Context context) {
        mFilter = new IntentFilter();
        mFilter.addAction(Intent.ACTION_TIME_CHANGED);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            	LogUtil.i(TAG, "Time Change:" + new Date());
                alarmResetTimer();
            }
        };
        context.registerReceiver(mReceiver, mFilter);
    }

    private int compareTime(long time1, long time2) {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(new Date(time1));

        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(new Date(time2));

        calendar1.set(Calendar.DATE, calendar2.get(Calendar.DATE));

        if(calendar1.getTimeInMillis() > calendar2.getTimeInMillis())
            return 1;
        else if(calendar1.getTimeInMillis() < calendar2.getTimeInMillis())
            return -1;
        return 0;
    }

    //item1 > item2  1
    //item1 < item2 -1
    //item1 = item2  0 chong tu
    private int compareItem(TimeItem item1, TimeItem item2) {
        if(item1.startTime > 0 && item1.endTime > 0) {
            if(item2.startTime > 0 && item2.endTime > 0) {
                if (compareTime(item1.endTime, item2.startTime) != 1)
                    return -1;
                else if (compareTime(item2.endTime, item1.startTime) != 1) {
                    return 1;
                } else {
                    return 0;
                }
            }
            else {
                if(compareTime(item1.endTime, item2.startTime) != 1)
                    return -1;
                else if (compareTime(item2.startTime, item1.startTime) == -1) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
        else if(item1.startTime > 0 && item1.endTime < 0) {
            if(item2.startTime > 0 && item2.endTime > 0) {
                if (compareTime(item1.startTime, item2.startTime) == -1)
                    return -1;
                else if (compareTime(item2.endTime, item1.startTime) != 1) {
                    return 1;
                } else {
                    return 0;
                }
            }
            else {
                return compareTime(item1.startTime, item2.startTime);
            }
        }
        return -2;
    }

    private Calendar getItemCalendar(TimeItem item, boolean isStart) {
        if(item != null) {
            Calendar curCal = Calendar.getInstance();

            Calendar itemCal = Calendar.getInstance();
            if(isStart)
                itemCal.setTime(new Date(item.startTime));
            else
                itemCal.setTime(new Date(item.endTime));

			itemCal.set(Calendar.YEAR, curCal.get(Calendar.YEAR));
			itemCal.set(Calendar.MONTH, curCal.get(Calendar.MONTH));
            itemCal.set(Calendar.DATE, curCal.get(Calendar.DATE));

			itemCal.add(Calendar.DATE, (item.week + 1 + 7 - curCal.get(Calendar.DAY_OF_WEEK)) % 7);
            return itemCal;
        }
        return null;
    }

    private TimeItem getNextItem() {
        if(mCurItem != null) {
            ArrayList<TimeItem> list = mWeekMap.get(mWeeks[mCurItem.week]);
            int index = list.indexOf(mCurItem);
            assert index > 0;
            if(index + 1 < list.size()) {
                return list.get(index + 1);
            }
            else {
                for (int i = mCurItem.week + 1; i < mWeeks.length + mCurItem.week + 1; i++) {
                    list = mWeekMap.get(mWeeks[i%mWeeks.length]);
                    if(list.size() > 0) {
                        return list.get(0);
                    }
                }
            }
        }
        return getNewItem();
    }

    private synchronized TimeItem getNewItem() {
        Calendar curCal = Calendar.getInstance();
        curCal.setTime(new Date());

        int curWeek = curCal.get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = curWeek; i < mWeeks.length + curWeek; i++) {
            ArrayList<TimeItem> list = mWeekMap.get(mWeeks[i%mWeeks.length]);
            if(list.size() > 0) {
                if(i == curWeek) {
                    for (TimeItem item : list) {
                        if(compareTime(item.startTime, curCal.getTimeInMillis()) != -1) {
                            return item;
                        }
                        else if(item.endTime > 0 && compareTime(item.endTime, curCal.getTimeInMillis()) != -1) {
                            return item;
                        }
                    }
                }
                else {
                    return list.get(0);
                }
            }
        }
        return null;
    }

    private void alarmResetTimer() {
        
        if(getNewItem() != mCurItem) {
            // maybe happen to system time change;
            // if running task already start and wait end then touch end at change other item
            if (mAlarmTask != null && !mAlarmTask.mIsStart) {
                mAlarmTask.touchItem(false);
            }
            mCurItem = null;
            alarmNextTimer(true);
        }
    }

    private void alarmNextTimer(boolean isStart) {
        TimeItem item;
        if(isStart)
            item = getNextItem();
        else
            item = mCurItem;
        if(item != null) {

            Calendar itemCal = getItemCalendar(item, isStart);
            if(itemCal != null) {
                if(mAlarmTask != null)
                    mAlarmTask.cancel();
                mAlarmTask = new AlarmTask(item, isStart);
                LogUtil.d(TAG, "alarmNextTimer at date:" + itemCal.getTime());
                mTimer.schedule(mAlarmTask, itemCal.getTime());
                mCurItem = item;
            }
        }
    }

    private boolean setTime(ArrayList<TimeItem> timeList, TimeItem t) {

        if(timeList.size() == 0) {
            timeList.add(t);
            return true;
        }

        for (int i = 0; i < timeList.size(); i++) {
            TimeItem item = timeList.get(i);
            int ret = compareItem(item, t);

            if(ret == -1)
                continue;
            if(ret == 1) {
                timeList.add(i, t);
                return true;
            }
            return false;
        }
        timeList.add(t);
        return true;
    }

	public Object getStartPrivateData(String userName) {
		if(mCurItem != null && mCurItem.userName.equals(userName) && mAlarmTask != null && !mAlarmTask.mIsStart) {
			return mCurItem.privateData;
		}
		return null;
	}

    public synchronized int registerAlarm(String userName, TimeCallback cb, int weeks, long startTime, long endTime, Object privateData) {
        if(cb == null || weeks == 0 || startTime <= 0 || (endTime >= 0 && endTime <= startTime)) {
            LogUtil.e(TAG, "param error!");
            return 0;
        }

        for (int i = 0; i < mWeeks.length; i++) {
            if((weeks & mWeeks[i]) > 0) {
                TimeItem item = new TimeItem();
                item.startTime = startTime;
                item.endTime = endTime;
                item.cb = cb;
                item.privateData = privateData;
                item.week = i;
                item.useID = mAlarmCount;
				item.userName = userName;

                if (!setTime(mWeekMap.get(mWeeks[i]), item)) {
                    LogUtil.e(TAG, "setTime error!");
                    weeks &= ~mWeeks[i];
                }
				else {
					LogUtil.d(TAG, "userName:" + userName + " setTime start:" + startTime + " date:" + getItemCalendar(item, true).getTime());
					if(item.endTime > 0)
						LogUtil.d(TAG, "userName:" + userName + " setTime end:" + endTime + " date:" + getItemCalendar(item, false).getTime());
				}
            }
        }

        if(weeks != 0) {
            alarmResetTimer();
        }
        return weeks == 0 ? -1 : mAlarmCount++;
    }

    public synchronized boolean unregisterAlarm(int useID) {
        boolean isFind = false;
        boolean isRestart = false;
        for (int i = 0; i < mWeeks.length; i++) {

            Iterator<TimeItem> iterator = mWeekMap.get(mWeeks[i]).iterator();
            while(iterator.hasNext()) {
                TimeItem item = iterator.next();
                if(item.useID == useID) {
                    iterator.remove();
                    if(mCurItem == item)
                        isRestart = true;
                    isFind = true;
                }
            }
        }
        if(isRestart) {
            alarmNextTimer(true);
        }
        if(isFind) mAlarmCount--;
        return isFind;
    }

    private class TimeItem {
        long startTime = -1;
        long endTime = 1;
        int week;
        int useID;
		String userName = "";
        Object privateData;
        TimeCallback cb;
    }

    public interface TimeCallback {
        void onAlarm(Object privateData, boolean isStart);
    }

    private class AlarmTask extends TimerTask {

        private TimeItem mItem;
        private boolean mIsStart;

        public AlarmTask(TimeItem item, boolean isStart) {
            mItem = item;
            mIsStart = isStart;
        }

        private void touchItem(boolean isStart) {
            if(mItem != null && mItem.cb != null) {
                mItem.cb.onAlarm(mItem.privateData, mIsStart);
            }
        }

        @Override
        public void run() {
            touchItem(true);
            if(mIsStart && mItem.endTime > 0)
                alarmNextTimer(false);
            else
                alarmNextTimer(true);
        }

    }
}
