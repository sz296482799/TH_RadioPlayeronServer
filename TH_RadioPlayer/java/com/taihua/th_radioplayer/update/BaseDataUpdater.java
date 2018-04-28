package com.taihua.th_radioplayer.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;

import com.taihua.th_radioplayer.connection.ServerConnection;
import com.taihua.th_radioplayer.database.PlayerDB;
import com.taihua.th_radioplayer.domain.*;
import com.taihua.th_radioplayer.manager.MusicManager;
import com.taihua.th_radioplayer.player.MusicChannel;
import com.taihua.th_radioplayer.utils.LogUtil;

public class BaseDataUpdater {

    public static final String TAG = "BASEDATA_UPDATER";

	public static final int UPDATE_BASE_DATA = 1;
	public static final int UPDATE_MUSIC_PACKET_LIST = 2;
    public static final int UPDATE_MUSIC_CHANNEL_LIST = 3;
    public static final int UPDATE_MUSIC_CHANNEL_FINISH = 4;
	public static final int UPDATE_SET_BROADCAST = 5;
	public static final int UPDATE_SET_CAROUSEL = 6;
	public static final int UPDATE_NOTHING = 7;

	private static final int MIN_CYCLE_NUM = 1; //1 min
	
	private String mDeviceID = "UNKNOW";
	private BaseDataIB mBaseData = null;
	private Handler mHandler = null;
	private Timer mTimer = null;
    private UpdateTask mTask = null;
	private int mCheckCycle = MIN_CYCLE_NUM;
	private boolean isRunning = false;
	
	public BaseDataUpdater(String deviceID, Handler handler) {
		// TODO Auto-generated constructor stub
		mDeviceID = deviceID;
		mHandler = handler;
		
	}
	
	public void init() {
		// TODO Auto-generated method stub
		mBaseData = PlayerDB.getInstance().getBaseData();
		if(mBaseData != null) {
			sendMessage(UPDATE_BASE_DATA, 1, 0, mBaseData.getData().getBase());
			sendMessage(UPDATE_MUSIC_PACKET_LIST, 1, 0, mBaseData.getData().getMusic());
            setCycle(mBaseData.getData().getBase().getBox_check_cycle());
		}
	}
	
	private boolean checkBaseDataChannelList(List<BaseDataDataChannelIB> list1, List<BaseDataDataChannelIB> list2) {
		// TODO Auto-generated constructor stub
		
		if(list1 != null && list2 != null) {
			if(list1.size() != list2.size()) {
				return true;
			}
			else if(list1.size() == 0){
				return false;
			}
			for(int i = 0; i < list1.size(); i++) {
				for(int j = 0; j < list2.size(); j++) {
					if(list1.get(i).getChannel_id() == list2.get(j).getChannel_id()
						&& list1.get(i).getChannel_ver() == list2.get(j).getChannel_ver())
						return true;
				}
			}
		}
		else if(list1 != list2)
			return true;
		return false;
	}
	
	private boolean checkBaseDataItemList(ArrayList<BaseDataDataIteamIB> list1, ArrayList<BaseDataDataIteamIB> list2) {
		// TODO Auto-generated constructor stub
		if(list1 != null && list2 != null) {
			if(list1.size() != list2.size()) {
				return true;
			}
			else if(list1.size() == 0){
				return false;
			}
			for(int i = 0; i < list1.size(); i++) {
				for(int j = 0; j < list2.size(); j++) {
					if(list1.get(i).getPks_id() == list2.get(j).getPks_id()) {
					    if(list1.get(i).getPks_ver() != list2.get(j).getPks_ver())
					        return true;
                    }
				}
			}
		}
		else if(list1 != list2)
			return true;
		return false;
	}
	
	private boolean sendMessage(Message msg) {
		if(mHandler != null) {
			return mHandler.sendMessage(msg);
		}
		return false;
	}
	
	private boolean sendMessage(int type, Object obj) {
        return sendMessage(type, 0, 0, obj);
	}

    private boolean sendMessage(int type) {
        return sendMessage(type, 0, 0, null);
    }
	
	private boolean sendMessage(int type, int arg1, int arg2, Object obj) {
		if(mHandler != null) {
			Message msg = new Message();
			msg.what = type;
			msg.obj = obj;
			msg.arg1 = arg1;
			msg.arg2 = arg2;
			sendMessage(msg);
		}
		return false;
	}
	
	private boolean isUpdateMusic(BaseDataIB baseData) {
		if(mBaseData != null) {
			ArrayList<BaseDataDataIteamIB> newMusic = baseData.getData().getMusic();
			ArrayList<BaseDataDataIteamIB> oldMusic = mBaseData.getData().getMusic();
			if(newMusic != null && oldMusic != null) {
				return checkBaseDataItemList(newMusic, oldMusic);
			}
			else if(newMusic != null) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isUpdateBroadcast(BaseDataIB baseData) {
		if(mBaseData != null) {
			ArrayList<BaseDataDataIteamIB> newMusic = baseData.getData().getBroadcast();
			ArrayList<BaseDataDataIteamIB> oldMusic = mBaseData.getData().getBroadcast();
			if(newMusic != null && oldMusic != null) {
				return checkBaseDataItemList(newMusic, oldMusic);
			}
			else if(newMusic != null) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isUpdateCarousel(BaseDataIB baseData) {
		if(mBaseData != null) {
			ArrayList<BaseDataDataIteamIB> newMusic = baseData.getData().getCarousel();
			ArrayList<BaseDataDataIteamIB> oldMusic = mBaseData.getData().getCarousel();
			if(newMusic != null && oldMusic != null) {
				return checkBaseDataItemList(newMusic, oldMusic);
			}
			else if(newMusic != null) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isUpdateBase(BaseDataIB baseData) {
		if(mBaseData != null) {
			BaseDataDataBaseIB newBase = baseData.getData().getBase();
			BaseDataDataBaseIB oldBase = mBaseData.getData().getBase();
			
			return newBase.equals(oldBase);
		}
		return false;
	}

    private boolean checkUpdateChannel(BaseDataIB baseData) {
	    boolean isUpdate = false;

        ArrayList<BaseDataDataIteamIB> musics = baseData.getData().getMusic();
        if(musics == null)
            return false;

	    for(BaseDataDataIteamIB music : musics) {
            List<BaseDataDataChannelIB> channels = music.getChannel();

            for (BaseDataDataChannelIB channel : channels) {
                MusicChannel c = MusicManager.getInstance().getChannelByID(channel.getChannel_id());
                if(c == null || c.getChannelVer() != channel.getChannel_ver()) {
                    ReturnMusicOB returnMusicOB = ServerConnection.getInstance().getChannelAllMusic(mDeviceID, channel.getChannel_id());
                    if(returnMusicOB != null) {
                        sendMessage(UPDATE_MUSIC_CHANNEL_LIST, returnMusicOB);
                        isUpdate = true;
                    }
                }
            }
        }
        if(isUpdate)
            sendMessage(UPDATE_MUSIC_CHANNEL_FINISH);
        return false;
    }

    private boolean updateBroadcast() {
		ReturnSetOB returnSetOB = ServerConnection.getInstance().getServerData(mDeviceID);
		if(returnSetOB != null) {
            PlayerDB.getInstance().writeServerData(returnSetOB);
			sendMessage(UPDATE_SET_BROADCAST, returnSetOB.getData().getBroadcast());
			return true;
		}
		return false;
	}

	private boolean updateCarousel() {
		ReturnSetOB returnSetOB = ServerConnection.getInstance().getServerData(mDeviceID);
		if(returnSetOB != null) {
            PlayerDB.getInstance().writeServerData(returnSetOB);
			sendMessage(UPDATE_SET_CAROUSEL, returnSetOB.getData().getCarousel());
			return true;
		}
		return false;
	}

	private void start(int delay) {
	    if(isRunning) return;
	    if(mTimer == null) {
            mTimer = new Timer();
        }
        if(mTask == null) {
            mTask = new UpdateTask();
        }
        mTimer.schedule(mTask, delay, mCheckCycle * 60000);
        isRunning = true;
    }

    private void reset(int delay) {
        stop();
        start(delay);
    }
	
	public void setCycle(int cycle) {
		if(cycle <= MIN_CYCLE_NUM)
		    cycle = MIN_CYCLE_NUM;

		if(mCheckCycle == cycle)
		    return;

	    mCheckCycle = cycle;
	    if(isRunning)
	        reset(mCheckCycle * 60000);
	    LogUtil.d(TAG, "setCycle:" + mCheckCycle);
	}
	
	public void update() {
		new Timer().schedule(new UpdateTask(), 0);
	}
	
	public void start() {
        start(0);
	}
	
	public void stop() {
	    if(!isRunning) return;
		if(mTask != null) {
            mTask.cancel();
        }
        if(mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
        mTimer = null;
        mTask = null;
        isRunning = false;
	}

    public BaseDataIB getBaseData() {
		return mBaseData;
    }

    private class UpdateTask extends TimerTask {

        @Override
        public void run() {
            // TODO Auto-generated method stub

			synchronized(UpdateTask.class) {
				boolean isUpdate = false;

	            BaseDataIB baseData = ServerConnection.getInstance().getBaseData(mDeviceID);
	            if(baseData == null || baseData.getResponse_code() == 0) {
	                return;
	            }

	            if(mBaseData == null) {
	                PlayerDB.getInstance().writeBaseData(baseData);
	                mBaseData = baseData;

	                sendMessage(UPDATE_BASE_DATA, baseData.getData().getBase());
	                sendMessage(UPDATE_MUSIC_PACKET_LIST, mBaseData.getData().getMusic());
	                checkUpdateChannel(mBaseData);
	                if(!updateBroadcast()) {
	                    mBaseData.getData().setBroadcast(new ArrayList<BaseDataDataIteamIB>());
	                }
	                if(!updateCarousel()) {
	                    mBaseData.getData().setCarousel(new ArrayList<BaseDataDataIteamIB>());
	                }
	                return;
	            }

	            if(isUpdateBase(baseData)) {
					LogUtil.d(TAG, "UpdateBase!");
	                sendMessage(UPDATE_BASE_DATA, baseData.getData().getBase());
	                isUpdate = true;
	            }

	            if(isUpdateMusic(baseData)) {
					LogUtil.d(TAG, "UpdateMusic!");
	                sendMessage(UPDATE_MUSIC_PACKET_LIST, mBaseData.getData().getMusic());
	                isUpdate = true;
	            }

	            if(checkUpdateChannel(baseData)) {
					LogUtil.d(TAG, "UpdateChannel!");
	                isUpdate = true;
	            }

	            if(isUpdateBroadcast(baseData)) {

	                if(updateBroadcast()) {
						LogUtil.d(TAG, "updateBroadcast!");
	                    isUpdate = true;
	                }
	                else
	                    baseData.getData().setBroadcast(mBaseData.getData().getBroadcast());
	            }

	            if(isUpdateCarousel(baseData)) {
	                if(updateCarousel()) {
						LogUtil.d(TAG, "updateCarousel!");
	                    isUpdate = true;
	                }
	                else
	                    baseData.getData().setCarousel(mBaseData.getData().getCarousel());
	            }

	            if(isUpdate) {
	                PlayerDB.getInstance().writeBaseData(baseData);
	            }
				else {
					sendMessage(UPDATE_NOTHING, null);
				}
			}
        }
    }
}
