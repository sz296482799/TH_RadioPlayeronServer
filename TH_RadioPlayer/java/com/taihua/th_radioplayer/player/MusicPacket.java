package com.taihua.th_radioplayer.player;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import com.taihua.th_radioplayer.domain.BaseDataDataChannelIB;
import com.taihua.th_radioplayer.domain.BaseDataDataIteamIB;
import com.taihua.th_radioplayer.manager.MusicManager;

public class MusicPacket {
	
	private ArrayList<Integer> hasChannelList;
	private int mCurChannelID = -1;
	private int mPacketID;
	private int mPacketVer;
	MusicManager mCManager;
	
	@SuppressLint("UseSparseArrays")
	public MusicPacket(int packetID, int packetVer) {
		mPacketID = packetID;
		mPacketVer = packetVer;
		hasChannelList = new ArrayList<Integer>();
		mCManager = MusicManager.getInstance();
	}
	
	private MusicChannel getChannel(int channelID) {
		if(channelID < 0)
			return null;
		return mCManager.getChannelByID(channelID);
	}
	
	private MusicChannel getCurChannel() {
		return getChannel(mCurChannelID);
	}
	
	public int size() {
		MusicChannel c = getCurChannel();
		if(c != null)
			return c.size();
		return 0;
	}

	public String current() {
		
		MusicChannel c = getCurChannel();
		if(c != null)
			return c.current();
		return null;
	}

	public MusicItem getMusic() {

        MusicChannel c = getCurChannel();
        if(c != null)
            return c.getMusic();
		return null;
	}

	public String next() {
		
		MusicChannel c = getCurChannel();
		if(c != null)
			return c.next();
		return null;
	}

	public String prev() {
		MusicChannel c = getCurChannel();
		if(c != null)
			return c.prev();
		return null;
	}
	
	public boolean isEnd() {
		MusicChannel c = getCurChannel();
		if(c != null)
			return c.isEnd();
		return true;
	}
	
	public String set(int index) {
		MusicChannel c = getCurChannel();
		if(c != null)
			return c.set(index);
		return null;
	}
	
	public String setPlayID(int musicID) {
		MusicChannel c = getCurChannel();
		if(c != null)
			return c.setPlayID(musicID);
		return null;
	}

	public boolean switchChannel(int channelID) {
		MusicChannel channel = getChannel(channelID);
		if(channel != null) {
			mCurChannelID = channelID;
			return true;
		}
		return false;
	}

    public boolean switchMusic(int musicID) {
        MusicChannel channel = getCurChannel();
        if(channel != null) {
            return channel.switchMusic(musicID);
        }
        return false;
    }
	
	public int getPacketID() {
		return mPacketID;
	}
	
	public int getPacketVer() {
		return mPacketVer;
	}
	
	public ArrayList<Integer> getHasChannelList() {
		return hasChannelList;
	}

	public void update(BaseDataDataIteamIB p) {
		// TODO Auto-generated method stub
		if(p.getPks_id() != mPacketID)
			return;
		
		mPacketVer = p.getPks_ver();
		hasChannelList.clear();

		if(p.getChannel() != null) {
			for(BaseDataDataChannelIB c : p.getChannel()) {
				hasChannelList.add(c.getChannel_id());
			}
			if(mCurChannelID == -1 && hasChannelList.size() > 0) {
				mCurChannelID = hasChannelList.get(0);
			}
		}

        if(hasChannelList.size() == 0)
            mCurChannelID = -1;
	}
}
