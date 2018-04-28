package com.taihua.th_radioplayer.player;

import java.io.IOException;
import java.util.Random;

import com.taihua.th_radioplayer.timer.CarouselTimer.CarouselItem;
import com.taihua.th_radioplayer.timer.BroadcastTimer.BroadcastItem;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;

public class MusicPlayer implements OnPreparedListener, OnCompletionListener, OnErrorListener {
	
	public static final int PLAYMODE_NO_LOOP = 0;
	public static final int PLAYMODE_ONE_LOOP = 1;
	public static final int PLAYMODE_LIST_LOOP = 2;
	public static final int PLAYMODE_LIST_NOLOOP = 3;
	public static final int PLAYMODE_RANMOD_PLAY = 4;
	
	public static final int PLAYTYPE_SYSTEM = 1;
	public static final int PLAYTYPE_CAROUSEL = 2;
	public static final int PLAYTYPE_BROADCAST = 3;
	
	private MusicList mList = null;
	private CarouselItem mCarouselItem = null;
	private BroadcastItem mBroadcastItem = null;
	private MediaPlayer mPlayer = null;
	private int mPlayMode = PLAYMODE_NO_LOOP;
	private int mPlayType = PLAYTYPE_SYSTEM;
	private Random mRandom = null;

	public MusicPlayer(MusicList list) {

		mPlayer = new MediaPlayer();
		setPlayerList(list);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnErrorListener(this);
		mRandom = new Random();
	}
	
	private void playBoradcast(BroadcastItem item) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		play(item.getFileUrl());
		item.mRepeatNum--;
		if(item.mRepeatNum <= 0) {
			mBroadcastItem = null;
		}
		mPlayType = PLAYTYPE_BROADCAST;
	}
	
	private void playCarousel(CarouselItem item) throws Exception {
		mCarouselItem = null;
		
		if(switchChannel(item.getChannelID())) {
			setPlayerMode(PLAYMODE_LIST_LOOP);
			play();
			mPlayType = PLAYTYPE_CAROUSEL;
		}
	}
	
	private void play(String path) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		
		if(mList != null && mList.size() > 0 && path != null) {
			
			mPlayer.reset();
			mPlayer.setDataSource(path);
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayer.prepareAsync();
		}
	}
	
	private boolean completionAction() {
		try {
			if(mBroadcastItem != null) {
				playBoradcast(mBroadcastItem);
			}
			else if(mCarouselItem != null) {
				playCarousel(mCarouselItem);
			}
			else if(mPlayMode == PLAYMODE_ONE_LOOP) {
				play();
			}
			else if(mPlayMode == PLAYMODE_LIST_LOOP) {
				next();
			}
			else if(mPlayMode == PLAYMODE_LIST_NOLOOP) {
				if(mList != null && !mList.isEnd()) {
					next();
				}
			}
			else if(mPlayMode == PLAYMODE_RANMOD_PLAY) {
				int index = mRandom.nextInt(mList.size());
				mList.set(index);
				play();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public MusicItem getPlayMusic() {
	    return mList.getMusic();
    }

    public int getActionType() {
		return mPlayType;
	}
	
	public boolean playSystem(int channelID, int musicID) throws Exception {
		boolean isSuccess = true;
		
        try {
			
            if (channelID != -1)
                isSuccess |= switchChannel(channelID);
            if (musicID != -1)
                isSuccess |= switchMusic(musicID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(isSuccess) {
            play();
			mPlayType = PLAYTYPE_SYSTEM;
			return true;
        }
		return false;
	}
	
	public void play() throws Exception {
		
		if(mList == null)
			throw new Exception("The play list is null!");
		play(mList.current());
	}
	
	public void pause() {

		mPlayer.pause();
	}
	
	public void stop() {
		mPlayer.stop();
	}
	
	public void next() throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		play(mList.next());
	}
	
	public void prev() throws Exception {
		
		if(mList == null)
			throw new Exception("The play list is null!");
		play(mList.prev());
	}
	
	public boolean switchChannel(int channelID) throws Exception {
		
		if(mList == null)
			throw new Exception("The play list is null!");
        return mList.switchChannel(channelID);
	}

	public boolean switchMusic(int musicID) throws Exception {

		if(mList == null)
			throw new Exception("The play list is null!");
        return mList.switchMusic(musicID);
	}
	
	public boolean isPlaying() {
		
		return mPlayer.isPlaying();
	}
	
	public void setPlayerList(MusicList list) {
		mList = list;
	}
	
	public MusicList getPlayerList() {
		return mList;
	}
	
	public void setPlayerMode(int mode) {
		mPlayMode = mode;
	}
	
	public void setBroadcast(BroadcastItem item) {
		try {
			if(item != null) {
				mBroadcastItem = item;
				if(item.getPlayMode() == 1) {
					playBoradcast(item);
				}
			}
			else if(mBroadcastItem != null) {
				mBroadcastItem = null;
				mPlayType = PLAYTYPE_SYSTEM;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void setCarousel(CarouselItem item) {
		try {
			if(item != null) {
				if(item.getIsStart()) {
					mCarouselItem = item;
					if(item.getPlayMode() == 1) {
						playCarousel(item);
					}
				}
				else {
					mCarouselItem = null;

				}
			}
			else if(mCarouselItem != null) {
				mCarouselItem = null;
			}
			if(mCarouselItem == null)
			    mPlayType = PLAYTYPE_SYSTEM;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public int getPlayerMode() {
		return mPlayMode;
	}
	
	public void release() {
		
		if(mPlayer.isPlaying())
			stop();
		mPlayer.release();
	}

	@Override
	public void onPrepared(MediaPlayer player) {
		player.start();
	}

	@Override
	public void onCompletion(MediaPlayer player) {
		completionAction();
	}

	@Override
	public boolean onError(MediaPlayer player, int what, int extra) {
		return completionAction();
	}
}
