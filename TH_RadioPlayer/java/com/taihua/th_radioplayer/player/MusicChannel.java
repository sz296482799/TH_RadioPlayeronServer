package com.taihua.th_radioplayer.player;

import java.util.ArrayList;

import android.annotation.SuppressLint;

import com.taihua.th_radioplayer.domain.ReturnMusicChannelMusicOB;
import com.taihua.th_radioplayer.domain.ReturnMusicChannelOB;
import com.taihua.th_radioplayer.download.DownloadState;

public class MusicChannel {
	private int mChannelID = -1;
	private int mChannelVer = -1;
	private int mMusicNum = 0;
    private int downloadSuccNum = 0;
    private int downloadFailNum = 0;
	private String mChannelName = null;
	private String mChannelPic = null;
	private int mIndex = -1;
	private ArrayList<MusicItem> mMusicList = null;
	private ArrayList<Integer> hasPacketList = null;
	
	@SuppressLint("UseSparseArrays")
	private void init() {
		mIndex = 0;
		mMusicList = new ArrayList<MusicItem>();
		hasPacketList = new ArrayList<Integer>();
	}
	
	public MusicChannel() {

		init();
	}
	
	public MusicChannel(int channelID, int channelVer) {
		
		mChannelID = channelID;
		mChannelVer = channelVer;
		init();
	}

    public void setDownloadSuccNum(int downloadSuccNum) {
        this.downloadSuccNum = downloadSuccNum;
    }

    public int getDownloadSuccNum() {
        return downloadSuccNum;
    }

    public void setDownloadFailNum(int downloadFailNum) {
        this.downloadFailNum = downloadFailNum;
    }

    public int getDownloadFailNum() {
        return downloadFailNum;
    }

    public MusicItem getMusic(int musicID) {
		// TODO Auto-generated method stub
		for (int i = 0; i < mMusicList.size(); i++) {
			if(mMusicList.get(i).getMusicID() == musicID) {
				return mMusicList.get(i);
			}
		}
		return null;
	}

	public int getMusicIndex(int musicID) {
		// TODO Auto-generated method stub
		for (int i = 0; i < mMusicList.size(); i++) {
			if(mMusicList.get(i).getMusicID() == musicID) {
				return i;
			}
		}
		return -1;
	}

	public ArrayList<MusicItem> getMusics() {
		return mMusicList;
	}
	
	public void setMusics(ArrayList<MusicItem> musics) {
		mMusicList = musics;
	}

	public int getChannelID() {
		return mChannelID;
	}
	
	public void setChannelID(int channelID) {
		mChannelID = channelID;
	}
	
	public String getChannelName() {
		return mChannelName;
	}
	
	public void setChannelName(String channelName) {
		mChannelName = channelName;
	}
	
	public String getChannelPic() {
		return mChannelPic;
	}
	
	public void setChannelPic(String channelPic) {
		mChannelPic = channelPic;
	}
	
	public int getChannelVer() {
		return mChannelVer;
	}
	
	public void setChannelVer(int channelVer) {
		mChannelVer = channelVer;
	}

    public int getMusicNum() {
        return mMusicNum;
    }

    public void setMusicNum(int mMusicNum) {
        this.mMusicNum = mMusicNum;
    }

    public int size() {
		if(mMusicList != null)
			return mMusicList.size();
		return 0;
	}
	
	public String current() {

		if(mMusicList != null && mMusicList.size() > 0) {
			if(mIndex >= 0 && mIndex < mMusicList.size())
				return mMusicList.get(mIndex).getPlayPath();
		}
		return null;
	}

    public MusicItem getMusic() {

        if(mMusicList != null && mMusicList.size() > 0) {
            if(mIndex >= 0 && mIndex < mMusicList.size())
                return mMusicList.get(mIndex);
        }
        return null;
    }

	public String next() {

		if(mMusicList != null && mMusicList.size() > 0) {
			mIndex++;
			if(mIndex >= mMusicList.size()) mIndex = 0;
			if(mIndex >= 0 && mIndex < mMusicList.size())
				return mMusicList.get(mIndex).getPlayPath();
		}
		return null;
	}

	public String prev() {
		
		if(mMusicList != null && mMusicList.size() > 0) {
			mIndex--;
			if(mIndex < 0) mIndex = mMusicList.size() - 1;
			if(mIndex >= 0 && mIndex < mMusicList.size())
				return mMusicList.get(mIndex).getPlayPath();
		}
		return null;
	}
	
	public boolean isEnd() {
		if(mMusicList == null || mMusicList.size() == 0)
			return true;
		return mIndex == mMusicList.size() - 1;
	}
	
	public String set(int index) {

		if(mMusicList != null && mIndex >= 0 && mIndex < mMusicList.size()) {
			mIndex = index;
			return mMusicList.get(mIndex).getPlayPath();
		}
		return null;
	}
	
	public String setPlayID(int musicID) {
		if(mMusicList == null)
			return null;
		if(switchMusic(musicID))
			return current();
		return null;
	}

	public boolean add(MusicItem music) {

		if(mMusicList != null && music != null) {
			mMusicList.add(music);
			return true;
		}
		return false;
	}

	public boolean switchMusic(int musicID) {
        int index = getMusicIndex(musicID);
	    if(index > 0) {
	        mIndex = index;
	        return true;
        }
        return false;
    }
	
	public ArrayList<Integer> getHasPacketList() {
		return hasPacketList;
	}

	// just use to MusicManager ! ! !
	public void update(ReturnMusicChannelOB c) {
		// TODO Auto-generated method stub
		if(c.getChannel_id() != mChannelID)
			return;
		
		mChannelVer = c.getChannel_ver();
		mMusicList.clear();
        downloadSuccNum = 0;
        downloadFailNum = 0;
        mIndex = 0;

		if(c.getMusics() == null) {
			return;
		}

		for(ReturnMusicChannelMusicOB m : c.getMusics()) {
			MusicItem item = new MusicItem();
			
			item.setChannelID(c.getChannel_id());
			item.setMusicID(m.getMusic_id());
			item.setMusicAlbum(m.getAlbum());
			item.setMusicName(m.getName());
			item.setMusicSinger(m.getSinger());
			item.setDowanloadUrl(m.getDownload_url());
			item.setDowanloadStatus(DownloadState.NONE);
			
			add(item);
		}
		setMusicNum(mMusicList.size());
	}

    public void downloadOne(MusicItem item) {
	    if(item != null && mMusicList.indexOf(item) >= 0) {
            if(item.getDowanloadStatus() != DownloadState.ERROR)
                ++downloadSuccNum;
            else
                ++downloadFailNum;
        }
    }

	public boolean isFinish() {
	    /*
	    int count = 0;
	    for(MusicItem music : mMusicList) {
	        if(music.getDowanloadStatus().value() > DownloadState.STARTED.value()) {
	            count++;
            }
        }
        */
        if((downloadSuccNum + downloadFailNum) == mMusicList.size()) {
	        return true;
        }
	    return false;
    }

	@Override
	public String toString() {
		return "MusicChannel [mChannelID=" + mChannelID
				+ ", mChannelVer=" + mChannelVer 
				+ ", mIndex=" + mIndex
				+ ", mMusicList=" + mMusicList
				+ "]";
	}
}
