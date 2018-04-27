package com.taihua.th_radioplayer.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import com.taihua.th_radioplayer.database.PlayerDB;
import com.taihua.th_radioplayer.domain.BaseDataDataIteamIB;
import com.taihua.th_radioplayer.domain.ReturnMusicChannelOB;
import com.taihua.th_radioplayer.domain.ReturnMusicChannelMusicOB;
import com.taihua.th_radioplayer.download.DownloadInfo;
import com.taihua.th_radioplayer.download.DownloadManager;
import com.taihua.th_radioplayer.download.DownloadState;
import com.taihua.th_radioplayer.player.MusicChannel;
import com.taihua.th_radioplayer.player.MusicDownloadHolder;
import com.taihua.th_radioplayer.player.MusicItem;
import com.taihua.th_radioplayer.player.MusicPacket;
import com.taihua.th_radioplayer.utils.LogUtil;
import org.xutils.ex.DbException;

@SuppressLint("UseSparseArrays")
public class MusicManager {

    public static final String TAG = "MusicManager";

    public static final int MUSIC_DOWNLOAD_START = 0x1001;
    public static final int MUSIC_DOWNLOAD_END = 0x1002;
	
	private static MusicManager mInstance;
	private HashMap<Integer, MusicPacket> mPacketMap;
	private HashMap<Integer, MusicChannel> mChannelMap;
	private HashMap<MusicItem, MusicDownloadHolder> mDownloadMap;
	private boolean mIsChange = false;
	private Handler mHandler;
	
	private MusicManager() {
		mChannelMap = new HashMap<Integer, MusicChannel>();
		mPacketMap = new HashMap<Integer, MusicPacket>();
        mDownloadMap = new HashMap<MusicItem, MusicDownloadHolder>();
	}

	public static MusicManager getInstance() {

		if (mInstance == null) {
			synchronized (MusicManager.class) {
				if (mInstance == null) {
					mInstance = new MusicManager();
				}
			}
		}
		return mInstance;
	}
	
	public void init(Handler handler) {
		// TODO Auto-generated method stub

        mHandler = handler;

		mChannelMap = PlayerDB.getInstance().getChannelData();
        LogUtil.d(TAG, "Channel Map:" + mChannelMap);
		if(mChannelMap == null) {
            mChannelMap = new HashMap<Integer, MusicChannel>();
        }
		else {
			Iterator<Entry<Integer, MusicChannel>> citer = mChannelMap.entrySet().iterator();
			while (citer.hasNext()) {
				downloadAllMusic(citer.next().getValue());
			}
		}
	}

	private void sendMessage(int what, Object obj) {
	    if(mHandler != null) {
            Message msg = new Message();
            msg.what = what;
            msg.obj = obj;
	        mHandler.sendMessage(msg);
        }
    }
	
	private boolean addPacket(MusicPacket packet) {
		if(packet == null)
			return false;
		
		if((getPacketByID(packet.getPacketID())) != null) {
			return false;
		}
		return mPacketMap.put(packet.getPacketID(), packet) != null;
	}
	
	private boolean addChannel(MusicChannel channel) {
		// TODO Auto-generated method stub
		if(channel == null)
			return false;
		if(getChannelByID(channel.getChannelID()) != null)
			return false;
		return mChannelMap.put(channel.getChannelID(), channel) != null;
	}
	
	private String getSavePath(MusicItem item) {
		String path = null;
		if(item != null) {
			MusicChannel c = getChannelByID(item.getChannelID());
			if(c != null) {
				path = MusicStorageManager.getInstance().getSavePath();
				path += "/" + c.getChannelID() + "-" + c.getChannelName();
				path += "/" + item.getMusicID() + "-" + item.getMusicName() + item.getSuffix();
			}
		}
		return path;
	}
	
	private void delOneMusic(MusicItem item) {
    	if(item != null) {
            try {
                MusicDownloadHolder holder = mDownloadMap.get(item);
                if(holder != null) {
                    DownloadInfo info = holder.getDownloadInfo();
                    if(info != null) {
                        DownloadManager.getInstance().removeDownload(info);
                        mDownloadMap.remove(item);
                        return;
                    }
                }
                MusicStorageManager.getInstance().delFile(getSavePath(item));
            } catch (DbException e) {
                e.printStackTrace();
            }
    	}
	}
	
	private void delAllMusic(MusicChannel channel) {
		ArrayList<MusicItem> musics = channel.getMusics();

		for(int i = 0; i < musics.size(); i++) {
			delOneMusic(musics.get(i));
		}
		musics.clear();
	}

	private void downloadOneMusic(MusicItem item) {
		if(item == null)
			return;

        try {
            if(item.getDowanloadStatus().value() > DownloadState.STARTED.value()) {
                return;
            }
            String savePath = getSavePath(item);

            item.setSavePath(savePath);

            MusicDownloadHolder holder = new MusicDownloadHolder(this, item, new DownloadInfo());
            mDownloadMap.put(item, holder);
            DownloadManager.getInstance().startDownload(
                    item.getDowanloadUrl(),
                    item.getMusicName(),
                    savePath,
                    false,
                    false,
                    holder);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }
	
	private boolean downloadAllMusic(MusicChannel channel) {
		if(channel == null)
			return false;

		if(channel.getMusics() == null || channel.getMusics().size() == 0)
		    return false;

		ArrayList<MusicItem> musics = channel.getMusics();
		for(int i = 0; i < musics.size(); i++) {
			downloadOneMusic(musics.get(i));
		}
		return true;
	}
	
	private synchronized void updatePacket(BaseDataDataIteamIB p) {
		// TODO Auto-generated method stub
		MusicPacket packet = getPacketByID(p.getPks_id());
		if(packet == null || packet.getPacketVer() != p.getPks_ver()) {
			if(packet == null) {
				packet = new MusicPacket(p.getPks_id(), p.getPks_ver());
				addPacket(packet);
			}
			packet.update(p);
		}
	}

	private synchronized void updateChannel(ReturnMusicChannelOB c) {
		// TODO Auto-generated method stub
		MusicChannel channel = getChannelByID(c.getChannel_id());
		if(channel == null) {
			channel = new MusicChannel(c.getChannel_id(), c.getChannel_ver());
			channel.setChannelName(c.getChannel_name());
			channel.setChannelPic(c.getChannel_pic());
			addChannel(channel);
		}
		delAllMusic(channel);
		channel.update(c);
		if(downloadAllMusic(channel)) {
		    sendMessage(MUSIC_DOWNLOAD_START, channel);
        }
		mIsChange = true;
	}
	
	private void bindChannel() {
		// TODO Auto-generated method stub
		if(mChannelMap.isEmpty() || mPacketMap.isEmpty()) {
			return;
		}
		
		Iterator<Entry<Integer, MusicChannel>> citer = mChannelMap.entrySet().iterator();
		while (citer.hasNext()) {
			Entry<Integer, MusicChannel> entry = citer.next();
			MusicChannel c = entry.getValue();
        	if(c != null) {
        		c.getHasPacketList().clear();
        	}
		}
		
		Iterator<Entry<Integer, MusicPacket>> piter = mPacketMap.entrySet().iterator();
		while (piter.hasNext()) {
			Entry<Integer, MusicPacket> pentry = piter.next();
			MusicPacket p = pentry.getValue();
        	if(p != null) {
                ArrayList<Integer> hasChannelList = p.getHasChannelList();
                for(Integer i : hasChannelList) {
                    MusicChannel c = getChannelByID(i);
                    if(c != null) {
                        c.getHasPacketList().add(p.getPacketID());
                    }
                }
        	}
		}
	}

	private void updateDB() {
		// TODO Auto-generated method stub
		if(mIsChange) {
            LogUtil.d(TAG, "UpdateDB Channel Map:" + mChannelMap);
			PlayerDB.getInstance().writeChannelData(mChannelMap);
		}
	}

	private void clearPacket() {
		// TODO Auto-generated method stub
		mPacketMap.clear();
	}

	private void clearUnusedChannel() {
		// TODO Auto-generated method stub
		Iterator<Entry<Integer, MusicChannel>> citer = mChannelMap.entrySet().iterator();
		while (citer.hasNext()) {
			Entry<Integer, MusicChannel> entry = citer.next();
			MusicChannel c = getChannelByID(entry.getKey());
			if(isUnused(c)) {
				delAllMusic(c);
				citer.remove();
				mIsChange = true;
			}
		}
	}

	private boolean isUnused(MusicChannel c) {
		// TODO Auto-generated method stub
		return c != null && c.getHasPacketList().isEmpty();
	}

	public synchronized void touchDownloadFinish(MusicItem item, DownloadInfo info) {
        if(item != null) {

            mIsChange = true;

            item.setDowanloadStatus(info.getState());
            MusicChannel channel = getChannelByID(item.getChannelID());
            if(channel != null) {
                channel.downloadOne(item);
                if(channel.isFinish()) {
                    sendMessage(MUSIC_DOWNLOAD_END, channel);
                    updateDB();
                }
            }
            mDownloadMap.remove(item);
        }
    }
	
	public synchronized MusicPacket getPacketByID(int packetID) {
		// TODO Auto-generated method stub
		return mPacketMap.get(packetID);
	}
	
	public synchronized MusicChannel getChannelByID(int channelID) {
		return mChannelMap.get(channelID);
	}

	public synchronized ReturnMusicChannelMusicOB MusicItem2JsonItem(MusicItem item) {
		if(item != null) {
			ReturnMusicChannelMusicOB music = new ReturnMusicChannelMusicOB();

			music.setAlbum(item.getMusicAlbum());
			music.setDownload_url(item.getDowanloadUrl());
			music.setMusic_id(item.getMusicID());
			music.setName(item.getMusicName());
			music.setSinger(item.getMusicSinger());
			return music;
		}
		return null;
	}

	public synchronized List<ReturnMusicChannelOB> getChannelList() {
	    
	    ArrayList<ReturnMusicChannelOB> list = new ArrayList<ReturnMusicChannelOB>();

        Iterator<Entry<Integer, MusicChannel>> iter = mChannelMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<Integer, MusicChannel> entry = iter.next();
            MusicChannel c = entry.getValue();
            if(c != null) {
                ReturnMusicChannelOB channel = new ReturnMusicChannelOB();

				channel.setChannel_id(c.getChannelID());
				channel.setChannel_ver(c.getChannelVer());
				channel.setChannel_name(c.getChannelName());
				channel.setChannel_pic(c.getChannelPic());
				channel.setMusic_count(c.getMusicNum());

				List<ReturnMusicChannelMusicOB> musics = new ArrayList<ReturnMusicChannelMusicOB>();
				ArrayList<MusicItem> musicList = c.getMusics();
				for(MusicItem item : musicList) {
					musics.add(MusicItem2JsonItem(item));
				}
				channel.setMusics(musics);
				
				list.add(channel);
            }
        }
		return list;
	}

	public synchronized void syncChannel() {
		
        bindChannel();
        clearUnusedChannel();
        updateDB();
	}

	public synchronized void updateAllPacket(List<BaseDataDataIteamIB> packetList) {
		clearPacket();

		if(packetList != null){
	        for(BaseDataDataIteamIB p : packetList) {
	            updatePacket(p);
	        }
		}
	}

	public synchronized void updateAllChannel(List<ReturnMusicChannelOB> channelList) {
		if(channelList == null)
            return;

        for(ReturnMusicChannelOB c : channelList) {
            updateChannel(c);
        }
	}
}
