package com.taihua.th_radioplayer.player;

import com.taihua.th_radioplayer.manager.MusicManager;

public class MusicList {
	
	private MusicChannel mCurChannel = null;
	
	public MusicList() {
		// TODO Auto-generated constructor stub
	}
	
	public synchronized void clear() {
		mCurChannel = null;
	}
	
	public synchronized int size() {
		if(mCurChannel != null)
			return mCurChannel.size();
		return 0;
	}

    public MusicItem getMusic() {
        if(mCurChannel != null)
            return mCurChannel.getMusic();
        return null;
    }

	public synchronized String current() {
		if(mCurChannel != null)
			return mCurChannel.current();
		return null;
	}

	public synchronized String next() {
		if(mCurChannel != null)
			return mCurChannel.next();
		return null;
	}

	public synchronized String prev() {
		if(mCurChannel != null)
			return mCurChannel.prev();
		return null;
	}
	
	public synchronized boolean isEnd() {
		if(mCurChannel != null)
			return mCurChannel.isEnd();
		return true;
	}
	
	public synchronized String set(int index) {
		if(mCurChannel != null)
			return mCurChannel.set(index);
		return null;
	}
	
	public synchronized String setPlayID(int musicID) {
		if(mCurChannel != null)
			return mCurChannel.setPlayID(musicID);
		return null;
	}

	public MusicChannel getChannel(int channelID) {
		MusicChannel channel = null;
		
		if(channelID == -1)
			channel = mCurChannel;
		else {
			channel = MusicManager.getInstance().getChannelByID(channelID);
		}
		return channel;
	}
	
	public boolean switchChannel(int channelID) {
		MusicChannel channel = getChannel(channelID);
		if(channel != null) {
			mCurChannel = channel;
			return true;
		}
		return false;
	}

	public synchronized boolean switchMusic(int musicID) {
        if(mCurChannel != null) {
            return mCurChannel.switchMusic(musicID);
        }
		return false;
	}
}
