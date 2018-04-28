package com.taihua.th_radioplayer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.*;
import android.net.wifi.*;

import com.taihua.th_radioplayer.connection.MQTTConnection;
import com.taihua.th_radioplayer.connection.ServerConnection;
import com.taihua.th_radioplayer.database.PlayerDB;
import com.taihua.th_radioplayer.domain.*;
import com.taihua.th_radioplayer.global.Config;
import com.taihua.th_radioplayer.manager.BluetoothManager;
import com.taihua.th_radioplayer.manager.MusicManager;
import com.taihua.th_radioplayer.manager.MusicStorageManager;
import com.taihua.th_radioplayer.manager.NetWorkManager;
import com.taihua.th_radioplayer.manager.NetWorkManager.WifiItem;
import com.taihua.th_radioplayer.manager.AlarmManager;
import com.taihua.th_radioplayer.player.MusicChannel;
import com.taihua.th_radioplayer.player.MusicItem;
import com.taihua.th_radioplayer.player.MusicList;
import com.taihua.th_radioplayer.player.MusicPlayer;
import com.taihua.th_radioplayer.timer.BroadcastTimer;
import com.taihua.th_radioplayer.timer.CarouselTimer;
import com.taihua.th_radioplayer.update.BaseDataUpdater;
import com.taihua.th_radioplayer.update.UploadLogUpdater;
import com.taihua.th_radioplayer.utils.LogUtil;
import com.taihua.th_radioplayer.utils.JasonUtils;
import com.taihua.th_radioplayer.utils.TurnJson;
import com.taihua.th_radioplayer.utils.UsbUtils;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class MainService extends Service {

    public static final String TAG = "MAIN_SERVICE";

    private String mDeviceID = null;
    private MusicPlayer mPlayer = null;
    private CarouselTimer mCarouselTimer = null;
    private BroadcastTimer mBroadcastTimer = null;
    private BaseDataUpdater mBaseDataUpdater = null;
    private UploadLogUpdater mUploadLogUpdater = null;
    private MusicManager mMusicManager = null;
    private MusicStorageManager mStorageManager = null;
    private PlayerDB mPlayerDB = null;
    private AudioManager mAudioManager;
    private BluetoothManager mBluetoothManager;
	private NetWorkManager mNetworkManager;
	private MQTTConnection mMqttConnection;

    public MainService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    public void onCreate()
    {
        super.onCreate();
    }

    private void setSystemTime() {
        ReturnTimeOB returnTimeOB = ServerConnection.getInstance().getServerTime();
        if(returnTimeOB != null && returnTimeOB.getResponse_code() == 1) {
            LogUtil.d(TAG, "setCurrentTimeMillis Time:" + returnTimeOB.getTime());
            SystemClock.setCurrentTimeMillis(returnTimeOB.getTime() * 1000);
        }
    }

    private Thread mClientThread = new Thread(new Runnable() {
        @Override
        public void run() {

            LogUtil.d(TAG, "ClientThread Start!");

            setSystemTime();

            mBaseDataUpdater.init();

            mCarouselTimer.init();
            mBroadcastTimer.init();

            mMqttConnection.init(mDeviceID, mCallback);
			mMqttConnection.start();

            ReturnOB returnOB = mPlayerDB.getKeyData();
            if(returnOB == null) {
                InitclientIB initclientIB = new InitclientIB(mDeviceID, Config.CLINE_TYPE);
                returnOB = ServerConnection.getInstance().setClientInit(initclientIB);
            }

            if(returnOB != null && returnOB.getResponse_code() == 1) {

                ServerConnection.getInstance().setClientSecret("3affc105c803e49da18849b3ccb1c96a");//returnOB.getClient_secret()
                mBaseDataUpdater.start();
                mUploadLogUpdater.start();

                mPlayerDB.writeKeyData(returnOB);
            }
        }
    });

    private Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {

            LogUtil.d(TAG, "Handler:" + msg);
            switch (msg.what) {

				case BaseDataUpdater.UPDATE_NOTHING:
                    mMqttConnection.publishUpdate("nothing");
                    break;

                case BaseDataUpdater.UPDATE_BASE_DATA:
                    BaseDataDataBaseIB baseDataIB = (BaseDataDataBaseIB)msg.obj;
                    if(baseDataIB != null) {
                        setBaseData(baseDataIB);
						if(msg.arg1 == 0)
							mMqttConnection.publishUpdate(Config.Option.get_base_data.getName());
                    }
                    break;

                case BaseDataUpdater.UPDATE_MUSIC_PACKET_LIST:
                    mMusicManager.updateAllPacket((ArrayList<BaseDataDataIteamIB>)msg.obj);
					mMusicManager.syncChannel();
                    break;

                case BaseDataUpdater.UPDATE_MUSIC_CHANNEL_LIST:
					
                    ReturnMusicOB returnMusicOB = (ReturnMusicOB) msg.obj;
					if(returnMusicOB == null)
			            break;

					mMusicManager.updateAllChannel(returnMusicOB.getData());
                    break;

                case BaseDataUpdater.UPDATE_MUSIC_CHANNEL_FINISH:
                    mMusicManager.syncChannel();
					mMqttConnection.publishUpdate(Config.Option.get_music_data.getName());
                    break;

                case BaseDataUpdater.UPDATE_SET_BROADCAST:
                    mBroadcastTimer.setBroadcastList((List<ReturnSetDataBroadcastOB>) msg.obj);
					mMqttConnection.publishUpdate(Config.Option.get_set_data.getName());
                    break;
                case BaseDataUpdater.UPDATE_SET_CAROUSEL:
                    mCarouselTimer.setCarouselList((List<ReturnSetDataCarouselOB>) msg.obj);
					mMqttConnection.publishUpdate(Config.Option.get_set_data.getName());
                    break;

                case BroadcastTimer.BROADCAST_TIMER_MSG:
                    mPlayer.setBroadcast((BroadcastTimer.BroadcastItem) msg.obj);
                    addAction(UploadLogUpdater.ACTION_TYPE_BROADCAST);
                    break;

                case CarouselTimer.CAROUSEL_TIMER_MSG:
                    mPlayer.setCarousel((CarouselTimer.CarouselItem) msg.obj);
                    addAction(UploadLogUpdater.ACTION_TYPE_CAROUSEL);
                    break;

                case MusicManager.MUSIC_DOWNLOAD_START:
                    mUploadLogUpdater.addUpdate((MusicChannel) msg.obj, UploadLogUpdater.UPADTE_TYPE_START);
                    break;

                case MusicManager.MUSIC_DOWNLOAD_END:
                    MusicChannel channel = (MusicChannel) msg.obj;
                    if(channel != null)
                        mUploadLogUpdater.addUpdate(channel,
                                channel.getDownloadFailNum() > 0 ? UploadLogUpdater.UPADTE_TYPE_FAIL : UploadLogUpdater.UPADTE_TYPE_SUCC);
                    break;

                case BluetoothManager.MESSAGE_DISCONNECTED:
                    LogUtil.d(TAG, "disconnected");
                    break;
                case BluetoothManager.MESSAGE_READ:
                    LogUtil.d(TAG, "message_read");
                    Bundle temp =  msg.getData();
                    byte[] buffer = temp.getByteArray(BluetoothManager.READ_MSG);
                    String str = new String(buffer);
                    LogUtil.d(TAG, "BluetoothManager.MESSAGE_READ Str:" + str);
                    break;
                case BluetoothManager.MESSAGE_WRITE:
                    LogUtil.d(TAG, "message_write");
                    break;
                case BluetoothManager.MESSAGE_STATE_CHANGE:
                    LogUtil.d(TAG, "state_change state:" + msg.arg1);
                    break;

				case NetWorkManager.NETWORK_MSG_WIFILIST:
					//LogUtil.d(TAG, "NETWORK_MSG_WIFILIST list:" + (List<WifiItem>)msg.obj);
					break;
				case NetWorkManager.NETWORK_MSG_CONNECT:
					if(msg.arg2 == 0) {
						mMqttConnection.tryConnect(1000);
					}
					break;

				case MusicStorageManager.USB_MOUNT_CHANGE:
					LogUtil.d(TAG, "MusicStorageManager USB_MOUNT_CHANGE");
					mMqttConnection.publishUsbList(mStorageManager.getRadioFile());
					break;

                default:
                    return false;
            }
            return true;
        }
    });
	
	private void writeWifiListToBlutooth(List<WifiItem> items) {
        if(items != null && items.size() > 0) {
			JSONArray array = JasonUtils.List2JsonArray(items, new TurnJson<WifiItem>() {
				@Override
				public JSONObject turn(WifiItem item) {
					JSONObject obj = new JSONObject();
					obj.put("wifiname", item.wifiName);
					obj.put("wifisignStrength", item.level);
					obj.put("isNeedpwd", item.capabilities);
					obj.put("connectState", item.isConnected ? 1 : 0);
					return obj;
				}
			});
		}
    }

    private MQTTConnection.ConnectionCallback mCallback = new MQTTConnection.ConnectionCallback() {
        @Override
        public String onAction(PublishItem item) {
            if(Config.Option.get_base_data.getName().equals(item.getOption())) {
                String data = item.getData();
                if(JasonUtils.jsonStrIsEmpty(data)) {
                    BaseDataIB baseDataIB = mPlayerDB.getBaseData();
                    if(baseDataIB != null)
                        return JasonUtils.object2JsonString(baseDataIB.getData().getBase());
                }
                else {
                    BaseDataDataBaseIB baseDataIB = JasonUtils.Jason2Object(data, BaseDataDataBaseIB.class);
                    if(baseDataIB != null) {
                        if(setBaseData(baseDataIB))
                            return mMqttConnection.publishReasonCode(200, "OK");
                        else
                            return mMqttConnection.publishReasonCode(201, "ERROR");
                    }
                }
            }
            else if (Config.Option.get_set_data.getName().equals(item.getOption())) {
                String data = item.getData();
                if(JasonUtils.jsonStrIsEmpty(data)) {
                    ReturnSetOB returnSetOB = mPlayerDB.getServerData();
                    if(returnSetOB != null)
                        return JasonUtils.object2JsonString(returnSetOB);
                }
                else {
                    ReturnSetOB returnSetOB = JasonUtils.Jason2Object(data, ReturnSetOB.class);
                    if(returnSetOB != null) {
                        List<ReturnSetDataBroadcastOB> broadcastList = returnSetOB.getData().getBroadcast();
                        if(broadcastList != null)
                            mBroadcastTimer.setBroadcastList(broadcastList);

                        List<ReturnSetDataCarouselOB> carouselList = returnSetOB.getData().getCarousel();
                        if(carouselList != null)
                            mCarouselTimer.setCarouselList(carouselList);

                        return mMqttConnection.publishReasonCode(200, "OK");
                    }
                }
            }
            else if (Config.Option.get_music_data.getName().equals(item.getOption())){
                String data = item.getData();
                if(!JasonUtils.jsonStrIsEmpty(data)) {
                    JSONObject jsonObject = JSON.parseObject(data);
                    if(jsonObject != null) {
                        Integer channel_id = jsonObject.getInteger("channel_id");
                        if(channel_id != null) {
							MusicChannel channel = mMusicManager.getChannelByID(channel_id);
                            if(channel != null) {
                                List<MusicItem> list = channel.getMusics();
                                return JasonUtils.object2JsonString(list);
                            }
                            else {
                                return mMqttConnection.publishReasonCode(203, "No find channel!");
                            }
                        }
                    }
                }
                return JasonUtils.object2JsonString(mMusicManager.getChannelList());
            }
            else if (Config.Option.get_play_action.getName().equals(item.getOption())) {
                String data = item.getData();
                if(!JasonUtils.jsonStrIsEmpty(data)) {
                    JSONObject jsonObject = JSON.parseObject(data);
                    if(jsonObject != null) {
                        Integer action_id = jsonObject.getInteger("action_id");
                        if(action_id != null) {
                            int id = action_id;
                            switch (id) {
                                case UploadLogUpdater.ACTION_TYPE_STARTPLAY:
                                    play();
                                    break;
                                case UploadLogUpdater.ACTION_TYPE_STOPPLAY:
                                    stop();
                                    break;
                                case UploadLogUpdater.ACTION_TYPE_SWITCH:
                                    Integer channel_id = jsonObject.getInteger("channel_id");
                                    Integer music_id = jsonObject.getInteger("music_id");

                                    switchMusic(
                                            channel_id == null ? -1 : channel_id,
                                            music_id == null ? -1 : music_id
                                            );
                                    break;
                                case UploadLogUpdater.ACTION_TYPE_VOLUP:
                                    vol_up();
                                    break;
                                case UploadLogUpdater.ACTION_TYPE_VOLDOWN:
                                    vol_down();
                                    break;
                                case UploadLogUpdater.ACTION_TYPE_BROADCAST:
                                    ReturnSetDataBroadcastOB broadcast = jsonObject.getObject("broadcast", ReturnSetDataBroadcastOB.class);
                                    if(broadcast != null) {
                                        mBroadcastTimer.touchBroadcast(broadcast);
                                    }
                                    break;
								case UploadLogUpdater.ACTION_TYPE_CAROUSEL:
                                    ReturnSetDataCarouselOB carousel = jsonObject.getObject("carousel", ReturnSetDataCarouselOB.class);
									Boolean isStart = jsonObject.getBoolean("isStart");
                                    if(carousel != null && isStart != null) {
                                        mCarouselTimer.touchCarouse(carousel, isStart);
                                    }
                                    break;

								default:
                    				return mMqttConnection.publishReasonCode(204, "Unknow Action!");
                            }
							return mMqttConnection.publishReasonCode(200, "OK");
                        }
                    }
                }
            }
			else if(Config.Option.get_usb_list.getName().equals(item.getOption())) {
				return JasonUtils.object2JsonString(mStorageManager.getRadioFile());
			}
			else if(Config.Option.send_update_msg.getName().equals(item.getOption())) {
				mBaseDataUpdater.update();
				return mMqttConnection.publishReasonCode(200, "OK");
			}
			else if(Config.Option.get_box_status.getName().equals(item.getOption())) {
				return get_box_status();
			}
            return mMqttConnection.publishReasonCode(202, "ERROR");
        }
    };

	private String get_box_status() {
        JSONObject jsonObj = new JSONObject();
		int curVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		if(maxVolume <= 0) {
			curVolume = 0;
			maxVolume = 1;
		}
		jsonObj.put("volume", curVolume * 100 / maxVolume);
		ReturnMusicChannelMusicOB music = null;
		if(mPlayer.isPlaying()) {
			music = mMusicManager.MusicItem2JsonItem(mPlayer.getPlayMusic());
		}
		jsonObj.put("music", music == null ? "null" : music);
		jsonObj.put("mode", music == null ? "null" : mPlayer.getPlayerMode());
		return JasonUtils.object2JsonString(jsonObj);
    }
	
    private void addAction(int actionID) {
        MusicItem music = mPlayer.getPlayMusic();
        if(music != null)
            mUploadLogUpdater.addAction(music.getChannelID(), actionID, mPlayer.getActionType());
    }

    private void play() {
        try {
            mPlayer.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
        addAction(UploadLogUpdater.ACTION_TYPE_STARTPLAY);
    }

    private void stop() {
        mPlayer.stop();
        addAction(UploadLogUpdater.ACTION_TYPE_STOPPLAY);
    }

    private void switchMusic(int channelID, int musicID) {
		
        try {
			if(mPlayer.playSystem(channelID, musicID))
				addAction(UploadLogUpdater.ACTION_TYPE_SWITCH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void vol_up() {
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI);
        addAction(UploadLogUpdater.ACTION_TYPE_VOLUP);
    }

    private void vol_down() {
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI);
        addAction(UploadLogUpdater.ACTION_TYPE_VOLDOWN);
    }

    private boolean setBaseData(BaseDataDataBaseIB basedate) {
        if(mPlayer == null || basedate == null)
            return false;

        boolean isBroadcast = basedate.getIs_broadcast() == 1;
        boolean isCarousel = basedate.getIs_carousel() == 1;

        if(!isCarousel) {
            mPlayer.setCarousel(null);
            mCarouselTimer.close();
        }
        else {
            mCarouselTimer.open();
        }

        if(!isBroadcast) {
            mPlayer.setBroadcast(null);
            mBroadcastTimer.close();
        }
        else {
            mBroadcastTimer.open();
        }
        mBaseDataUpdater.setCycle(basedate.getBox_check_cycle());
        mUploadLogUpdater.setCycle(basedate.getBox_log_cycle());
        mUploadLogUpdater.setIsUploadAction(basedate.getIs_action_log_upload() == 1);
        mUploadLogUpdater.setIsUploadUpdate(basedate.getIs_update_log_upload() == 1);
        return true;
    }

    private void serviceInit()
    {
        mDeviceID = Config.MAC;
        mStorageManager = MusicStorageManager.getInstance();
        mPlayer = new MusicPlayer(new MusicList());
        mCarouselTimer = new CarouselTimer(mHandler);
        mBroadcastTimer = new BroadcastTimer(mHandler);
        mBaseDataUpdater = new BaseDataUpdater(mDeviceID, mHandler);
        mUploadLogUpdater = new UploadLogUpdater(mDeviceID);
        mMusicManager = MusicManager.getInstance();
        mPlayerDB = PlayerDB.getInstance();
        mAudioManager = (AudioManager)getSystemService(Service.AUDIO_SERVICE);
        mBluetoothManager = BluetoothManager.getInstance();
		mNetworkManager = NetWorkManager.getInstance();
		mMqttConnection = MQTTConnection.getInstance();

		AlarmManager.getInstance().init(this);

		mPlayerDB.init(this);

		mNetworkManager.init(this, mHandler);
		mBluetoothManager.init(this, mHandler);
		
        mStorageManager.init(this, mHandler);
        mMusicManager.init(mHandler);
        
        mBluetoothManager.start();

		mClientThread.start();
		//mNetworkManager.Wifi_Scan();
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        serviceInit();
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy()
    {
        super.onDestroy();
    }
}
