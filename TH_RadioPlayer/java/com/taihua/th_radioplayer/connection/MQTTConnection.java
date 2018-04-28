package com.taihua.th_radioplayer.connection;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.taihua.th_radioplayer.domain.Connected;
import com.taihua.th_radioplayer.domain.DeviceID;
import com.taihua.th_radioplayer.domain.PublishItem;
import com.taihua.th_radioplayer.domain.Reason;
import com.taihua.th_radioplayer.global.Config;
import com.taihua.th_radioplayer.utils.LogUtil;
import com.taihua.th_radioplayer.utils.MD5Util;
import com.taihua.th_radioplayer.utils.JasonUtils;
import com.taihua.th_radioplayer.manager.MusicStorageManager.USBNode;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Date;

public class MQTTConnection implements MqttCallback {

    public static final String TAG = "MQTTConnection";

	private static MQTTConnection mInstance;
    private String mClientID;
    private long mClientTime = 0;

    private MqttThread mMqttThread;
    private final static short KEEP_ALIVE = 30;

    private ConnectionCallback mCallback;

	private MQTTConnection() {
	}

	public static MQTTConnection getInstance() {
	    if (mInstance == null) {
	        synchronized (MQTTConnection.class) {
	            if(mInstance == null) {
	                mInstance = new MQTTConnection();
                }
            }
        }
        return mInstance;
    }

    public void init(String deviceID, ConnectionCallback cb) {

        mCallback = cb;

        mMqttThread = new MqttThread(deviceID, this);
        mMqttThread.start();
    }

    public void start() {
	    if(mMqttThread != null) {

			LogUtil.d(TAG, "start!");

            mMqttThread.connect(true, KEEP_ALIVE);
            clientSubscribe(mMqttThread.mClientID);
        }
    }

    public void stop() {
        if(mMqttThread != null) {
            mMqttThread.disconnect();
            clientUnsubscribe(mMqttThread.mClientID);
        }
    }

	public void tryConnect() {
		if(mMqttThread != null && !mMqttThread.isConnected() && mMqttThread.isWaitConnect) {
			LogUtil.d(TAG, "tryConnect! start");
			start();
		}
	}

	public void tryConnect(int delayMs) {
		if(mMqttThread != null && !mMqttThread.isConnected() && mMqttThread.isWaitConnect) {
			LogUtil.d(TAG, "tryConnect! start");
			mMqttThread.connect(true, KEEP_ALIVE, delayMs);
			clientSubscribe(mMqttThread.mClientID, delayMs);
		}
	}

    private void clientSubscribe(String clientID) {
		if(mMqttThread == null)
			return;
	    String[] topicName = {clientID};
	    int[] quality = {0};
        mMqttThread.subscribe(topicName, quality);
    }

	private void clientSubscribe(String clientID, int delayMs) {
		if(mMqttThread == null)
			return;
	    String[] topicName = {clientID};
	    int[] quality = {0};
        mMqttThread.subscribe(topicName, quality, delayMs);
    }

    private void clientUnsubscribe(String clientID) {
		if(mMqttThread == null)
			return;
        String[] topicName = {clientID};
        mMqttThread.unsubscribe(topicName);
    }

    private String getToken(String option, String id) {
        if(id != null && mClientTime > 0) {
            return MD5Util.MD5(option + mClientTime + id);
        }
        return null;
    }

    private synchronized boolean publishSecret(String option, Object obj) {

        return publishSecret(option, obj == null ? null : JasonUtils.object2JsonString(obj));
    }

    private synchronized boolean publishSecret(String option, String dataStr) {

		if(!isConnected()) {
			LogUtil.e(TAG,"publishSecret: no connect! option:" + option);
            return false;
		}
        if(mClientID == null || mClientTime <= 0) {
            LogUtil.e(TAG,"publishSecret: no client connect! option:" + option);
            return false;
        }

        PublishItem item = new PublishItem();

        item.setOption(option);
        item.setToken(getToken(option, mMqttThread.getDeviceID()));
        if (dataStr != null)
            item.setData(dataStr);

        LogUtil.d(TAG,"publishSecret:" + JasonUtils.object2JsonString(item));
        mMqttThread.publish(mClientID, JasonUtils.object2JsonString(item));
        return true;
    }

    private boolean publishConnected() {
        Connected connected = new Connected();

        mClientTime = new Date().getTime() / 1000;
        connected.setCode(200);
        connected.setDate(mClientTime);

        return publishSecret(Config.Option.connected.getName(), connected);
    }

    private boolean publishDisconnected() {
        mClientTime = 0;
        return publishSecret(Config.Option.disconnected.getName(), null);
    }

    private boolean checkToken(String option, String token) {
	    return option != null && token != null && token.equals(getToken(option, mClientID));
    }

    private void parse(PublishItem item) {

        if(Config.Option.connected.getName().equals(item.getOption())) {
            String dataStr = item.getData();
			if(dataStr != null) {
	            DeviceID data = JSON.parseObject(dataStr, DeviceID.class);
	            LogUtil.d(TAG,"DeviceID:" + data);
	            if(data != null) {
	                LogUtil.d(TAG,"device_id:" + data.getDeviceid());
	                clientConnect(data.getDeviceid());
	            }
        	}
        }
        else if(checkToken(item.getOption(), item.getToken())) {
            if(Config.Option.disconnected.getName().equals(item.getOption())) {
                clientDisconnect();
            }
            else if(Config.Option.get_base_data.getName().equals(item.getOption())
                    || Config.Option.get_set_data.getName().equals(item.getOption())
                    || Config.Option.get_music_data.getName().equals(item.getOption())
                    || Config.Option.get_play_action.getName().equals(item.getOption())
                    || Config.Option.get_usb_list.getName().equals(item.getOption())
                    || Config.Option.send_update_msg.getName().equals(item.getOption())
                    || Config.Option.get_box_status.getName().equals(item.getOption())
                    ) {
                if(mCallback != null) {
                    String publishStr = mCallback.onAction(item);
                    if(publishStr != null) {
                        publishSecret(item.getOption(), publishStr);
                    }
                }
            }
			else {
				publishSecret(item.getOption(), publishReasonCode(301, "Unknow Option:" + item.getOption()));
			}
        }
        else {
			if(Config.DEBUG)
				publishSecret(item.getOption(), publishReasonCode(300, "Token Error! MD5 this string:" + item.getOption() + mClientTime + mClientID));
            LogUtil.e(TAG,"Check Error! token:" + getToken(item.getOption(), mClientID) + " new token:" + item.getToken());
        }
    }

    private synchronized void clientDisconnect() {
        publishDisconnected();
        mClientID = null;
    }

    private synchronized void clientConnect(String clientID) {

        if( !isConnected() || clientID == null) {
            LogUtil.e(TAG,"no Connected or param error!");
            return;
        }

        if(mClientID != null) {
            if (!mClientID.equals(clientID))
                clientDisconnect();
            else {
                LogUtil.e(TAG,"Already Connect!");
            }
        }
        mClientID = clientID;
        publishConnected();
    }

	public String publishReasonCode(int code, String msg) {
        Reason reason = new Reason();
        reason.setReasonCode(code);
        reason.setMsg(msg);
        return JasonUtils.object2JsonString(reason);
    }

	public boolean publishUsbList(USBNode node) {
		if(node != null) {
        	return publishSecret(Config.Option.get_usb_list.getName(), node);
		}
		return false;
    }

	public boolean publishUpdate(String updateName) {

		if(updateName != null) {
			JSONObject obj = new JSONObject();
			obj.put("update", updateName);
			return publishSecret(Config.Option.send_update_msg.getName(), obj);
		}
        return false;
    }

    public boolean isConnected() {
		return mMqttThread != null && mMqttThread.isConnected();
	}

	public boolean waitConnect(long waitTime) throws InterruptedException {
	    long start = System.currentTimeMillis();
	    while (!isConnected() && (waitTime <= 0 || start + waitTime > System.currentTimeMillis())) {
	        Thread.sleep(100);
        }
        return isConnected();
    }

    @Override
    public void connectionLost(Throwable throwable) {
        LogUtil.e(TAG,"Lost! throwable:" + throwable);
		mMqttThread.disconnect();
		mMqttThread.connect(true, KEEP_ALIVE);
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        String publishStr = new String(mqttMessage.getPayload());
		
        LogUtil.d(TAG,"messageArrived:" + publishStr);
        if(s != null && s.equals(mMqttThread.getDeviceID()) && !mqttMessage.isRetained()) {
            PublishItem item = JSON.parseObject(publishStr, PublishItem.class);
            if(item != null) {
                LogUtil.d(TAG,"messageArrived PublishItem:" + item);
                parse(item);
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        LogUtil.d(TAG,"deliveryComplete:" + iMqttDeliveryToken.isComplete());
    }

    private class MqttThread implements Runnable, Handler.Callback {

	    private static final int MSG_CONNECT = 0;
        private static final int MSG_DISCONNECT = 1;
        private static final int MSG_PUBLISH = 2;
        private static final int MSG_SUBSCRIBE = 3;
        private static final int MSG_UNSUBSCRIBE = 4;

        private String mClientID;
        private MemoryPersistence mPersistence;
        private MqttClient mClient = null;
        private Thread mThread;
        private boolean isWaitConnect = false;
        private Handler mHandler;

        private MqttThread(String clientID, MqttCallback cb) {
            try {
                mClientID = clientID;

                mPersistence = new MemoryPersistence();
                mClient = new MqttClient(Config.MQTT_SERVER_URL, clientID, mPersistence);
                mClient.setCallback(cb);

                mThread = new Thread(this);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        private void _connect(boolean isCleanStart, short keepLive) throws MqttException {

            if(mClient != null) {
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(isCleanStart);
                connOpts.setKeepAliveInterval(keepLive);
                connOpts.setUserName("admin");
                connOpts.setPassword("password".toCharArray());
                LogUtil.d(TAG,"Connecting to broker: " + mClient.getServerURI());
                mClient.connect(connOpts);
				LogUtil.d(TAG,"Connected!");
            }
        }

        private void _disconnect() throws MqttException {

            if(!mClient.isConnected())
                return;
            mClient.disconnect();
        }

        private void _publish(String topicName, String message, int quality, boolean isRetained) throws MqttException {

            if(!mClient.isConnected())
                return;

            mClient.publish(topicName,
                    message.getBytes(),
                    quality,
                    isRetained);
        }

        private void _subscribe(String[] topicName, int[] quality) throws MqttException {

            if(!mClient.isConnected())
                return;
            mClient.subscribe(topicName, quality);
        }

        private void _unsubscribe(String[] topicName) throws MqttException {

            if(!mClient.isConnected())
                return;
            mClient.unsubscribe(topicName);
        }

        private void start() {
            mThread.start();
			while(mHandler == null) {
				try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
			}
        }

        private String getDeviceID() {
            return mClientID;
        }

        private boolean isConnected() {
            return mClient.isConnected();
        }

        private void connect(boolean isCleanStart, short keepLive) {
            Message msg = new Message();

            mHandler.removeMessages(MSG_CONNECT);

            msg.what = MSG_CONNECT;

            Bundle bundle = new Bundle();
            bundle.putBoolean("isCleanStart", isCleanStart);
            bundle.putShort("keepLive", keepLive);

            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

		private void connect(boolean isCleanStart, short keepLive, int delayMs) {
            Message msg = new Message();

            mHandler.removeMessages(MSG_CONNECT);

            msg.what = MSG_CONNECT;

            Bundle bundle = new Bundle();
            bundle.putBoolean("isCleanStart", isCleanStart);
            bundle.putShort("keepLive", keepLive);

            msg.setData(bundle);
            mHandler.sendMessageDelayed(msg, delayMs);
        }

        private void disconnect() {
            Message msg = new Message();

            msg.what = MSG_DISCONNECT;
            mHandler.sendMessage(msg);
        }

        private void publish(String topicName, String message) {
            Message msg = new Message();

            msg.what = MSG_PUBLISH;

            Bundle bundle = new Bundle();
            bundle.putString("topicName", topicName);
            bundle.putString("message", message);

            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        private void subscribe(String[] topicName, int[] quality) {
            Message msg = new Message();

            msg.what = MSG_SUBSCRIBE;

            Bundle bundle = new Bundle();
            bundle.putStringArray("topicName", topicName);
            bundle.putIntArray("quality", quality);

            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

		private void subscribe(String[] topicName, int[] quality, int delayMs) {
            Message msg = new Message();

            msg.what = MSG_SUBSCRIBE;

            Bundle bundle = new Bundle();
            bundle.putStringArray("topicName", topicName);
            bundle.putIntArray("quality", quality);

            msg.setData(bundle);
            mHandler.sendMessageDelayed(msg, delayMs);
        }

        private void unsubscribe(String[] topicName) {
            Message msg = new Message();

            msg.what = MSG_UNSUBSCRIBE;

            Bundle bundle = new Bundle();
            bundle.putStringArray("topicName", topicName);

            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler(this);
            Looper.loop();
        }

        @Override
        public boolean handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MSG_CONNECT:
                        if(!isConnected()) {
                            LogUtil.d(TAG, "MSG_CONNECT!");
							isWaitConnect = true;
                            _connect(msg.getData().getBoolean("isCleanStart"), msg.getData().getShort("keepLive"));
                            isWaitConnect = false;
                        }
                        break;
                    case MSG_DISCONNECT:
                        LogUtil.d(TAG,"MSG_DISCONNECT!");
                        _disconnect();
            			isWaitConnect = false;
                        break;
                    case MSG_PUBLISH:
                        LogUtil.d(TAG,"MSG_PUBLISH!");
                        _publish(msg.getData().getString("topicName"), msg.getData().getString("message"), 0, false);
                        break;
                    case MSG_SUBSCRIBE:
                        LogUtil.d(TAG,"MSG_SUBSCRIBE!");
                        _subscribe(msg.getData().getStringArray("topicName"), msg.getData().getIntArray("quality"));
                        break;
                    case MSG_UNSUBSCRIBE:
                        LogUtil.d(TAG,"MSG_UNSUBSCRIBE!");
                        _unsubscribe(msg.getData().getStringArray("topicName"));
                        break;
                }
            } catch (MqttException me) {
                LogUtil.e(TAG, "MqttThread MqttException:" + me);
                if((msg.what == MSG_CONNECT) && !mClient.isConnected() && isWaitConnect) {
                    LogUtil.e(TAG, "can't connect! retry after 10s!");
					mHandler.removeMessages(MSG_CONNECT);
					connect(msg.getData().getBoolean("isCleanStart"), msg.getData().getShort("keepLive"), 10000);
                }
            }
            return true;
        }
    }

    public interface ConnectionCallback {
	    String onAction(PublishItem item);
    }
}
