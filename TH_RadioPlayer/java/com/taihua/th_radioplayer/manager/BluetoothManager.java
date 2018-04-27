package com.taihua.th_radioplayer.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.net.ConnectivityManager;

import com.taihua.th_radioplayer.utils.LogUtil;

public class BluetoothManager {

    private static final String TAG = "BluetoothManager";

    private static final String BLUETOOTH_NAME = "THRadioPlayer";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mAdapter;
    private Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private static BluetoothManager mInstance;
    private BluetoothDevice mConnectedBluetoothDevice;
	private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
	private AtomicReference<BluetoothPan> mBluetoothPan = new AtomicReference<BluetoothPan>();
	private boolean mBluetoothAvailable = false;

    private static final int STATE_NONE = 0x4000;
    private static final int STATE_LISTEN = 0x4001;
    private static final int STATE_CONNECTING = 0x4002;
    private static final int STATE_CONNECTED = 0x4003;
    private static final int STATAE_CONNECT_FAILURE = 0x4004;

    public static final int MESSAGE_DISCONNECTED = 0x4005;
    public static final int MESSAGE_STATE_CHANGE = 0x4006;
    public static final int MESSAGE_READ = 0x4007;
    public static final int MESSAGE_WRITE= 0x4008;

    public static final String DEVICE_NAME = "device_name";
    public static final String READ_MSG = "read_msg";

    private BluetoothManager() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
		if(checkHasBluetooth()) {
			
			mAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0); //DISCOVERABLE_TIMEOUT_NEVER
			mAdapter.setName(BLUETOOTH_NAME);

			LogUtil.d(TAG, "Adapter getName:" + mAdapter.getName());
			LogUtil.d(TAG, "Adapter getAddress:" + mAdapter.getAddress());
			
			mFilter = new IntentFilter();
			
	        mFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
			mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
			mFilter.addAction(BluetoothDevice.ACTION_DISAPPEARED);
			
			mFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
			mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

	        mReceiver = new BroadcastReceiver() {
	            @Override
	            public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					
					if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
						
						LogUtil.d(TAG, "Receive PAIRING REQUEST!");
						BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
						if(mDevice != null) {

							LogUtil.d(TAG, "getAddress:" + mDevice.getAddress());
							LogUtil.d(TAG, "getName:" + mDevice.getName());
							if(mDevice.getUuids() != null) {
								for(ParcelUuid uuid : mDevice.getUuids()) {
									LogUtil.d(TAG, "getUuid:" + uuid);
								}
							}
							else {
								LogUtil.d(TAG, "getUuids:" + mDevice.getUuids());
							}
							
							LogUtil.d(TAG, "getAliasName:" + mDevice.getAliasName());
							
							int mType = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
							int mPairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
			                if (mPairingKey != BluetoothDevice.ERROR) {
								String pairingKey = null;
								if (mType == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY) {
				                    pairingKey = String.format("%06d", mPairingKey);
				                } else if (mType == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN) {
				                    pairingKey = String.format("%04d", mPairingKey);
				                } else if (mType == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION) {
				                    pairingKey = String.format(Locale.US, "%06d", mPairingKey);
				                }
			                    LogUtil.d(TAG, "pairingKey:" + pairingKey);
			                }
							switch (mType) {
					            case BluetoothDevice.PAIRING_VARIANT_PIN:
					            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
					                LogUtil.d(TAG, "Can't Auto Connect!cancel pairing!");
									mDevice.cancelPairingUserInput();
					                break;

					            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
					            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
					            case BluetoothDevice.PAIRING_VARIANT_CONSENT:
									mDevice.setPairingConfirmation(true);
									break;
					            case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
					                mDevice.setRemoteOutOfBandData();
					                break;
					            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
					                if (mPairingKey == BluetoothDevice.ERROR) {
					                    LogUtil.e(TAG, "Invalid Confirmation PIN received, do nothing");
										mDevice.cancelPairingUserInput();
					                    return;
					                }
					                byte[] pinBytes = BluetoothDevice.convertPinToBytes(String.format("%04d", mPairingKey));
							        mDevice.setPin(pinBytes);
					                break;

					            default:
					                LogUtil.e(TAG, "Incorrect pairing type received, do nothing");
					        }
						}
					}
					else if(action.equals(BluetoothDevice.ACTION_DISAPPEARED)) {
						BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
						if(mDevice != null) {
							LogUtil.e(TAG, "BluetoothDevice Disappeared!");
						}
					}
					else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
						int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
						switch (bondState) {
							case BluetoothDevice.BOND_NONE:
								LogUtil.e(TAG, "BluetoothDevice BOND_NONE!");
				                break;
				            case BluetoothDevice.BOND_BONDING:
								LogUtil.e(TAG, "BluetoothDevice BOND_BONDING!");
				                break;
							case BluetoothDevice.BOND_BONDED:
								LogUtil.e(TAG, "BluetoothDevice BOND_BONDED!");
				                break;
							default:
					            LogUtil.e(TAG, "BluetoothDevice BOND_STATE UNKNOW!");
						}
					}
					else if(action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
						int connectionState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothDevice.ERROR);
						BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
						if(mDevice != null) {
							switch (connectionState) {
								case BluetoothAdapter.STATE_DISCONNECTED:
									LogUtil.e(TAG, "BluetoothAdapter DISCONNECTED!");
					                break;
					            case BluetoothAdapter.STATE_CONNECTING:
									LogUtil.e(TAG, "BluetoothAdapter CONNECTING!");
					                break;
								case BluetoothAdapter.STATE_CONNECTED:
									LogUtil.e(TAG, "BluetoothAdapter CONNECTED!");
					                break;
					            case BluetoothAdapter.STATE_DISCONNECTING:
					                LogUtil.e(TAG, "BluetoothAdapter DISCONNECTING!");
					                break;
								default:
					            	LogUtil.e(TAG, "BluetoothAdapter CONNECTION_STATE UNKNOW!");
							}
						}
					}
					else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
						int buletoohState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
						switch (buletoohState) {
	                        case BluetoothAdapter.STATE_ON:
	                            LogUtil.e(TAG, "BluetoothAdapter Buletooh ON!");
	                            break;

	                        case BluetoothAdapter.STATE_OFF:
	                        case BluetoothAdapter.ERROR:
								LogUtil.e(TAG, "BluetoothAdapter Buletooh OFF!");
	                            break;

	                        default:
	                            // ignore transition states
	                    }
						
						if(mBluetoothAvailable && buletoohState == BluetoothAdapter.STATE_ON) {
			                BluetoothPan bluetoothPan = mBluetoothPan.get();
                            if (bluetoothPan != null && !bluetoothPan.isTetheringOn()) {
                                bluetoothPan.setBluetoothTethering(true);
                            }
						}
		            }
	            }
	        };
		}
        mState = STATE_NONE;
    }

    public static BluetoothManager getInstance() {

        if (mInstance == null) {
            synchronized (BluetoothManager.class) {
                if (mInstance == null) {
                    mInstance = new BluetoothManager();
                }
            }
        }
        return mInstance;
    }

	private BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mBluetoothPan.set((BluetoothPan) proxy);
			BluetoothPan bluetoothPan = mBluetoothPan.get();
            if (!bluetoothPan.isTetheringOn()) {
                bluetoothPan.setBluetoothTethering(true);
            }
			LogUtil.e(TAG, "Open Bluetooth Tether Successful!");
        }
        public void onServiceDisconnected(int profile) {
            mBluetoothPan.set(null);
			LogUtil.e(TAG, "Open Bluetooth Tether Fail!");
        }
    };

	public void init(Context context, Handler handler) {
        context.registerReceiver(mReceiver, mFilter);
        mHandler = handler;

		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		mBluetoothAvailable = cm.getTetherableBluetoothRegexs().length != 0;

		if(mBluetoothAvailable) {
			// get BluetoothPan to open Bluetooth Tether
			// may be used getApplicationContext() for memory leak. by zshang 
			LogUtil.e(TAG, "Open Bluetooth Tether!");
			mAdapter.getProfileProxy(context, mProfileServiceListener, BluetoothProfile.PAN);
			
		}
		else {
			LogUtil.e(TAG, "No Bluetooth Tether Available!");
		}
    }

    private synchronized void setState(int state) {
        LogUtil.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    private boolean checkHasBluetooth() {
        if(mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                return mAdapter.enable();
            }
            return true;
        }
        LogUtil.d(TAG, "No Bluetooth!");
        return false;
    }

    public synchronized int getState() {
        return mState;
    }

    public BluetoothDevice getConnectedDevice(){
        return mConnectedBluetoothDevice;
    }

    public synchronized void start() {
        LogUtil.d(TAG, "start");

        if(!checkHasBluetooth())
            return;

        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device) {
        LogUtil.d(TAG, "connect to: " + device);

        if(!checkHasBluetooth())
            return;

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        LogUtil.d(TAG, "connected");

        if(!checkHasBluetooth())
            return;

        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        mConnectedBluetoothDevice = device;
        Message msg = mHandler.obtainMessage(STATE_CONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        LogUtil.d(TAG, "stop");

        if(!checkHasBluetooth())
            return;

        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        setState(STATE_NONE);
    }

    public void write(byte[] out) {

        if(!checkHasBluetooth())
            return;

        ConnectedThread r;

        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    private void connectionFailed() {
        setState(STATE_LISTEN);

        Message msg = mHandler.obtainMessage(STATAE_CONNECT_FAILURE);
        mHandler.sendMessage(msg);
        mConnectedBluetoothDevice = null;
    }

    private void connectionLost() {
        setState(STATE_LISTEN);

        Message msg = mHandler.obtainMessage(MESSAGE_DISCONNECTED);
        mHandler.sendMessage(msg);
        mConnectedBluetoothDevice = null;
        stop();
    }

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mServerSocket;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(BLUETOOTH_NAME, MY_UUID);

            } catch (IOException e) {
                LogUtil.e(TAG, "listen() failed! error:" + e);
            }
            mServerSocket = tmp;
        }

        public void run() {
            LogUtil.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            if(mServerSocket == null) {
                return;
            }

            while (mState != STATE_CONNECTED) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    LogUtil.e(TAG, "accept() failed! error:" + e);
                    break;
                }

				LogUtil.d(TAG, "accept() OK!");
                if (socket != null) {
                    synchronized (BluetoothManager.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:

                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:

                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    LogUtil.e(TAG, "Could not close unwanted socket! error:" + e);
                                }
                                break;
                        }
                    }
                }
            }
            LogUtil.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            LogUtil.d(TAG, "cancel " + this);
            try {
                mServerSocket.close();
            } catch (IOException e) {
                LogUtil.e(TAG, "close() of server failed! error:" + e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;

            try {
                mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                LogUtil.e(TAG, "create() failed! error:" + e);
                mmSocket = null;
            }
        }

        public void run() {
            LogUtil.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
            mAdapter.cancelDiscovery();

            try {

                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();

                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    LogUtil.e(TAG, "unable to close() socket during connection failure! error:" +  e2);
                }

                BluetoothManager.this.start();
                return;
            }

            synchronized (BluetoothManager.this) {
                mConnectThread = null;
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                LogUtil.e(TAG, "close() of connect socket failed! error:" + e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            LogUtil.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                LogUtil.e(TAG, "sockets error:" + e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            LogUtil.i(TAG, "BEGIN mConnectedThread");
            int bytes;

            while (true) {
                try {
                    byte[] buffer = new byte[1024];
                    bytes = mmInStream.read(buffer);

                    Message msg = mHandler.obtainMessage(MESSAGE_READ);
                    Bundle bundle = new Bundle();
                    bundle.putByteArray(READ_MSG, buffer);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                } catch (IOException e) {
                    LogUtil.e(TAG, "disconnected error:" + e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                LogUtil.e(TAG, "Exception during write! error:" + e);
            }

        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                LogUtil.e(TAG, "close() of connect socket failed! error:" + e);
            }
        }
    }
}