package com.taihua.th_radioplayer.player;

import com.taihua.th_radioplayer.download.DownloadState;

public class MusicItem {
	
	private int mChannelID = -1;
	private int mMusicID = -1;
	private DownloadState mDownloadStatus;
	private String mMusicName = null;
	private String mMusicSinger = null;
	private String mMusicAlbum = null;
	private String mDownloadUrl = null;
	private String mSavePath = null;
	
	public MusicItem() {
		
	}
	
	public MusicItem(int channelID, int musicID, String name, String singer, String album, String url) {
		
		mChannelID = channelID;
		mMusicID = musicID;
		mDownloadStatus = DownloadState.STOPPED;
		mMusicName = name;
		mMusicSinger = singer;
		mMusicAlbum = album;
		mDownloadUrl = url;
	}
	
	public void setMusicID(int musicID) {
		mMusicID = musicID;
	}

	public int getMusicID() {
		return mMusicID;
	}
	
	public int getChannelID() {
		return mChannelID;
	}
	
	public void setChannelID(int channelID) {
		mChannelID = channelID;
	}

	public void setMusicName(String musicName) {
		mMusicName = musicName;
	}
	
	public String getMusicName() {
		return mMusicName;
	}

	public String getSuffix() {
		if(mDownloadUrl != null) {
			int index = mDownloadUrl.lastIndexOf(".");
			if(index >= 0) {
				String suffix = mDownloadUrl.substring(index);
				return suffix.length() > 4 ? "" : suffix;
			}
		}
		return "";
	}
	
	public void setMusicSinger(String musicSinger) {
		mMusicSinger = musicSinger;
	}
	
	public String getMusicSinger() {
		return mMusicSinger;
	}
	
	public void setMusicAlbum(String musicAlbum) {
		mMusicAlbum = musicAlbum;
	}
	
	public String getMusicAlbum() {
		return mMusicAlbum;
	}
	
	public void setDowanloadUrl(String dowanloadUrl) {
		mDownloadUrl = dowanloadUrl;
	}
	
	public String getDowanloadUrl() {
		return mDownloadUrl;
	}
	
	public void setSavePath(String savePath) {
		mSavePath = savePath;
	}
	
	public String getSavePath() {
		return mSavePath;
	}
	
	public String getPlayPath() {
		if(mDownloadStatus == DownloadState.FINISHED)
			return mSavePath;
		return mDownloadUrl;
	}

	public void setDowanloadStatus(DownloadState status) {
		// TODO Auto-generated method stub
		mDownloadStatus = status;
	}

	public DownloadState getDowanloadStatus() {
		// TODO Auto-generated method stub
		return mDownloadStatus;
	}

	@Override
	public String toString() {
		return "MusicItem ["
				+ " mMusicID=" + mMusicID
				+ ", mMusicName=" + mMusicName
				+ ", mMusicSinger=" + mMusicSinger
				+ ", mMusicAlbum=" + mMusicAlbum
				+ ", mDownloadUrl=" + mDownloadUrl
                + ", mDownloadStatus=" + mDownloadStatus
				+ "]";
	}
}