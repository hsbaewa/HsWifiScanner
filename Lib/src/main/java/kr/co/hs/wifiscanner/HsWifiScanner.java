package kr.co.hs.wifiscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import java.util.List;

/**
 * Created by privacydev on 2017. 8. 30..
 */

public class HsWifiScanner implements HsWifiConstant{

    Context mContext;
    OnScanResultsListener mOnScanResultsListener;
    ScanResultsReceiver mScanResultsReceiver;
    WifiManager mWifiManager;

    public HsWifiScanner(Context context){
        this.mContext = context;
    }

    //Context 리턴
    public Context getContext() {
        return this.mContext;
    }

    //WifiManager 리턴
    public WifiManager getWifiManager(){
        if(mWifiManager == null)
            mWifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return mWifiManager;
    }

    //Wifi 가능 기기 체크
    public boolean hasWifiChipSet(){
        return getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    //스캔을 시작하는 함수
    public boolean scan(){
        if(hasWifiChipSet()){
            registerScanResultReceiver();
            boolean result = getWifiManager().startScan();
            if(!result){
                //만약 스캔 실패하면 리시버 해제
                unRegisterScanResultReceiver();
            }
            return result;
        }else{
            return false;
        }
    }

    public void setOnScanResultsListener(OnScanResultsListener onScanResultsListener) {
        this.mOnScanResultsListener = onScanResultsListener;
    }

    public interface OnScanResultsListener{
        void onScanResults(List<ScanResult> scanResults);
    }


    private class ScanResultsReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
                unRegisterScanResultReceiver();
                if(hasWifiChipSet()){
                    List<ScanResult> scanResults = getWifiManager().getScanResults();
                    if(mOnScanResultsListener != null)
                        mOnScanResultsListener.onScanResults(scanResults);
                }
            }
        }
    }

    private boolean registerScanResultReceiver(){
        if(mScanResultsReceiver == null){
            mScanResultsReceiver = new ScanResultsReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            getContext().registerReceiver(mScanResultsReceiver, filter);
            return true;
        }else{
            return false;
        }
    }

    private boolean unRegisterScanResultReceiver(){
        if(mScanResultsReceiver == null){
            return false;
        }else{
            getContext().unregisterReceiver(mScanResultsReceiver);
            mScanResultsReceiver = null;
            return true;
        }
    }
}
