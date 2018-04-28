package com.taihua.th_radioplayer.manager;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.DhcpInfo;
import android.net.ethernet.EthernetManager;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.IpAssignment;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.ProxySettings;
import android.net.wifi.*;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.net.InetAddress;

import android.os.Handler;
import android.os.Message;
import com.taihua.th_radioplayer.utils.LogUtil;

public class NetWorkManager {

    private static final String TAG = "NetWorkManager";

    public static final int NETWORK_MSG_WIFILIST = 0x9000;
	public static final int NETWORK_MSG_CONNECT = 0x9001;
	public static final int NETWORK_MSG_DISCONNECT = 0x9002;

    private static final int SECURITY_NONE = 0;
    private static final int SECURITY_WEP = 1;
    private static final int SECURITY_PSK = 2;
    private static final int SECURITY_EAP = 3;

    private static NetWorkManager mInstance;
    private EthernetManager mEthManager;
    private WifiManager mWifiManager;
	private WifiManager.ActionListener mConnectListener;
    private Scanner mScanner;
    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
    private Handler mHandler;
	private boolean isScanning = false;

    // Combo scans can take 5-6s to complete - set to 10s.
    private static final int WIFI_RESCAN_INTERVAL_MS = 10 * 1000;

    private NetWorkManager() {
		
        mScanner = new Scanner();
		
		mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		
		mFilter.addAction(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
		mFilter.addAction(EthernetManager.NETWORK_STATE_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(intent);
            }
        };
		
		mConnectListener = new WifiManager.ActionListener() {
                                   @Override
                                   public void onSuccess() {
                                       sendMessage(NETWORK_MSG_CONNECT, 1, 0, null);
                                   }
                                   @Override
                                   public void onFailure(int reason) {
                                       sendMessage(NETWORK_MSG_CONNECT, 1, 1, null);
                                   }
                               };

    }

    public static NetWorkManager getInstance() {

        if (mInstance == null) {
            synchronized (NetWorkManager.class) {
                if (mInstance == null) {
                    mInstance = new NetWorkManager();
                }
            }
        }
        return mInstance;
    }

    public void init(Context context, Handler handler) {
        mEthManager = (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        context.registerReceiver(mReceiver, mFilter);

        mHandler = handler;
    }

    private void sendMessage(int what, int arg1, int arg2, Object obj) {
        if(mHandler != null) {
            Message msg = new Message();
            msg.what = what;
            msg.arg1 = arg1;
            msg.arg2 = arg2;
            msg.obj = obj;
            mHandler.sendMessage(msg);
        }
    }

    public void startPppoe() {
        mEthManager.setEthernetEnabled(false);
        mEthManager.setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE, null);
        mEthManager.setEthernetEnabled(true);
    }

    public void startDhcp() {
        mEthManager.setEthernetEnabled(false);
        mEthManager.setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_DHCP, null);
        mEthManager.setEthernetEnabled(true);
    }

    public void startStatic(String ip, String netmask, String gateway, String dns1, String dns2) {
        mEthManager.setEthernetEnabled(false);

        String ipAddress = "";
        if (ip != null && ip.length() > 0) {
            ipAddress = ip;
        }
        else {
            LogUtil.e(TAG, "Invalid ipv4 address");
            return;
        }

        String netMask = "";
        if (netmask != null) {
            netMask = netmask;
        }

        String gateWay = "";
        if (netmask != null) {
            gateWay = gateway;
        }
        String DNS1 = "";
        if (DNS1 != null) {
            DNS1 = dns1;
        }
        String DNS2 = "";
        if (DNS2 != null) {
            DNS2 = dns2;
        }

        InetAddress ipaddr = NetworkUtils.numericToInetAddress(ipAddress);
        InetAddress getwayaddr = NetworkUtils.numericToInetAddress(gateWay);
        InetAddress inetmask = NetworkUtils.numericToInetAddress(netMask);
        InetAddress idns1 = NetworkUtils.numericToInetAddress(DNS1);
        InetAddress idns2 = NetworkUtils.numericToInetAddress(DNS2);

        DhcpInfo dhcpInfo = new DhcpInfo();
        try {
            dhcpInfo.ipAddress = NetworkUtils.inetAddressToInt(ipaddr);
            dhcpInfo.gateway = NetworkUtils.inetAddressToInt(getwayaddr);
            dhcpInfo.netmask = NetworkUtils.inetAddressToInt(inetmask);
            dhcpInfo.dns1 = NetworkUtils.inetAddressToInt(idns1);
            dhcpInfo.dns2 = NetworkUtils.inetAddressToInt(idns2);
        } catch(IllegalArgumentException e) {
            LogUtil.e(TAG, "Invalid ipv4 address");
        }
        mEthManager.setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL, dhcpInfo);

        mEthManager.setEthernetEnabled(true);
    }

    public void Wifi_Start_Scan() {
        if(!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
        mScanner.forceScan();
		isScanning = true;
    }

	public void Wifi_Stop_Scan() {
        mScanner.pause();
		isScanning = false;
    }

    public boolean connect(String ssid, String password, int security) {

        if(ssid == null)
            return false;

		if(security == -1) {
	        ScanResult result = getResults(ssid);
	        if(result == null)
	            return false;	        
	        if(result.capabilities.contains("WEP"))
	            security = SECURITY_WEP;
	        else if(result.capabilities.contains("PSK"))
	            security = SECURITY_PSK;
	        else if(result.capabilities.contains("EAP"))
	            security = SECURITY_EAP;
			else
				security = SECURITY_NONE;
		}

        WifiConfiguration config = new WifiConfiguration();

        config.SSID = ssid;
        switch (security) {
            case SECURITY_NONE:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                break;

            case SECURITY_WEP:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
                if(password != null && password.length() != 0) {
                    int length = password.length();
                    if ((length == 10 || length == 26 || length == 58) && password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    }
                    else
                        config.wepKeys[0] = '"' + password + '"';
                }
                break;

            case SECURITY_PSK:
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                if(password != null && password.length() != 0) {
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.wepKeys[0] = password;
                    }
                    else
                        config.wepKeys[0] = '"' + password + '"';
                }
                break;
        }

		config.proxySettings = ProxySettings.NONE;
        config.ipAssignment = IpAssignment.DHCP;

        mWifiManager.connect(config, mConnectListener);

        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();

        return true;
    }

    private ScanResult getResults(String ssid) {
        final List<ScanResult> results = mWifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID != null && result.equals(ssid)) {
                    return result;
                }
            }
        }
        return null;
    }

    /** Returns sorted list of access points */
    private List<WifiItem> constructAccessPoints() {

        //final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();

        List<ScanResult> results = mWifiManager.getScanResults();
		ArrayList<WifiItem> list = new ArrayList<WifiItem>();
        if (results != null) {

			WifiInfo info = mWifiManager.getConnectionInfo();
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0 ||
                        result.capabilities.contains("[IBSS]")) {
                    continue;
                }
                WifiItem item = new WifiItem(result);
                if(info != null && info.getSSID().equals(result.SSID)) {
                    item.setConnected(true);
                }
				list.add(item);
            }
            Collections.sort(results, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult scanResult1, ScanResult scanResult2) {
                    if(scanResult1.level > scanResult2.level)
                        return 1;
                    return -1;
                }
            });
        }
        return list;
    }

    private void updateAccessPoints() {
		if(!isScanning)
			return;
        if(mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            List<WifiItem> items = constructAccessPoints();
            if(items != null && items.size() > 0) {
                sendMessage(NETWORK_MSG_WIFILIST, 0, 0, items);
            }
        }
    }

    private void handleEvent(Intent intent) {
        String action = intent.getAction();
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            if(WifiManager.WIFI_STATE_ENABLED != state) {
                 mScanner.pause();
            }
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) ||
                WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION.equals(action) ||
                WifiManager.LINK_CONFIGURATION_CHANGED_ACTION.equals(action)) {
            updateAccessPoints();
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            updateAccessPoints();
        }
		else if(EthernetManager.ETHERNET_STATE_CHANGED_ACTION.equals(action)) {
			int status = intent.getIntExtra(EthernetManager.EXTRA_ETHERNET_STATE, -1);
			LogUtil.d(TAG, "ETHERNET_STATE_CHANGED_ACTION:" + status);
			switch (status) {
				//case EthernetManager.EVENT_PHY_LINK_UP:
                case EthernetManager.EVENT_DHCP_CONNECT_SUCCESSED:
                case EthernetManager.EVENT_STATIC_CONNECT_SUCCESSED:
					sendMessage(NETWORK_MSG_CONNECT, 0, 0, null);
					break;
				//case EthernetManager.EVENT_PHY_LINK_DOWN:
				case EthernetManager.EVENT_DHCP_CONNECT_FAILED:
				case EthernetManager.EVENT_STATIC_CONNECT_FAILED:
					sendMessage(NETWORK_MSG_CONNECT, 0, 1, null);
					break;
				//case EthernetManager.EVENT_STATIC_DISCONNECT_SUCCESSED:
                default:
                    break;
            }
		}
    }

    private class Scanner extends Handler {
        private int mRetry = 0;

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void pause() {
            mRetry = 0;
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message message) {
            if (mWifiManager.startScan()) {
                mRetry = 0;
            } else if (++mRetry >= 3) {
                mRetry = 0;
                return;
            }
            sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
        }
    }

	public static class WifiItem {

        public String wifiName;
        public int level;
        public int capabilities;
        public boolean isConnected = false;

        public WifiItem(ScanResult result) {
            wifiName = result.SSID;
            level = WifiManager.calculateSignalLevel(result.level, 9);

            if(result.capabilities.contains("WEP"))
                capabilities = SECURITY_WEP;
            else if(result.capabilities.contains("PSK"))
                capabilities = SECURITY_PSK;
            else if(result.capabilities.contains("EAP"))
                capabilities = SECURITY_EAP;
            else
                capabilities = SECURITY_NONE;
        }

        public void setConnected(boolean b) {
            isConnected = b;
        }

		public String toString() {
			return " WifiItem:{"
				+ "wifiName:" + wifiName + ","
				+ "level:" + level + ","
				+ "capabilities:" + capabilities + ","
				+ "isConnected:" + isConnected
				+ "} ";
		}
	}
}
