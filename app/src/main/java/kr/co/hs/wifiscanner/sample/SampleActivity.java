package kr.co.hs.wifiscanner.sample;

import android.content.DialogInterface;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import kr.co.hs.app.HsActivity;
import kr.co.hs.app.OnRequestPermissionResult;
import kr.co.hs.content.HsDialogInterface;
import kr.co.hs.content.HsPermissionChecker;
import kr.co.hs.widget.recyclerview.HsRecyclerView;
import kr.co.hs.wifiscanner.HsWifiConstant;
import kr.co.hs.wifiscanner.HsWifiScanner;
import kr.co.hs.wifiscanner.HsWifiUtil;

/**
 * Created by privacydev on 2017. 8. 29..
 */

public class SampleActivity extends HsActivity implements HsWifiScanner.OnScanResultsListener, HsWifiConstant, HsRecyclerView.OnItemClickListener{

    HsRecyclerView mHsRecyclerView;
    HsWifiScanner mHsWifiScanner;
    Adapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        Toolbar toolbar = (Toolbar) findViewById(R.id.Toolbar);
        setSupportActionBar(toolbar);

        mHsRecyclerView = (HsRecyclerView) findViewById(R.id.HsRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mHsRecyclerView.setLayoutManager(linearLayoutManager);


        mAdapter = new Adapter();
        mHsRecyclerView.setAdapter(mAdapter);


        mHsWifiScanner = new HsWifiScanner(getContext());
        mHsWifiScanner.setOnScanResultsListener(this);

        mHsRecyclerView.setOnItemClickListener(this);

        startScan();
    }



    @Override
    public void onScanResults(List<ScanResult> scanResults) {
        mAdapter.clear();
//        String wifiBSSID = mHsWifiScanner.getWifiManager().getConnectionInfo().getBSSID();
//        for(ScanResult scanResult : scanResults){
//            if(scanResult.BSSID.equals(wifiBSSID)){
//                mAdapter.add(scanResult);
//            }
//        }
        mAdapter.addAll(scanResults);
        mAdapter.notifyDataSetChanged();
        startScan();
    }


    //와이파이 스캔 시작 함수
    private void startScan(){
        String permission[] = {
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        };
        HsPermissionChecker.requestPermissions(this, permission, 0, new OnRequestPermissionResult() {
            @Override
            public void onResult(int i, @NonNull String[] strings, @NonNull int[] ints, boolean b) {
                if(b){
                    mHsWifiScanner.scan();
                }
            }
        });
    }

    @Override
    public void onItemClick(HsRecyclerView hsRecyclerView, RecyclerView.ViewHolder viewHolder, View view, int i) {
        final ScanResult scanResult = mAdapter.getItem(i);
        final int security = HsWifiUtil.getSecurity(scanResult);
        final WifiConfiguration config = HsWifiUtil.getWifiConfiguration(mHsWifiScanner.getWifiManager(), scanResult, security);

        View alertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_wifiinfo, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        TextView textViewSignal = (TextView) alertView.findViewById(R.id.TextViewSignal);
        TextView textViewSecurity = (TextView) alertView.findViewById(R.id.TextViewSec);
        TextView textViewStatus = (TextView) alertView.findViewById(R.id.TextViewStatus);
        TextView textViewSpeed = (TextView) alertView.findViewById(R.id.TextViewSpeed);
        TextView textViewAddress = (TextView) alertView.findViewById(R.id.TextViewAddress);
        final EditText editTextPassword = (EditText) alertView.findViewById(R.id.EditTextPassword);


        String sigFormat = "신호세기 : %d(1~5)";
        sigFormat = String.format(sigFormat, WifiManager.calculateSignalLevel(scanResult.level, 5));
        textViewSignal.setText(sigFormat);

        String strSec = HsWifiUtil.getReadableSecurity(scanResult);
        String secFormat = "보안 : %s";
        secFormat = String.format(secFormat, strSec);
        textViewSecurity.setText(secFormat);

        final int numOpenNetworksKept = Settings.Secure.getInt(getContentResolver(), Settings.Secure.WIFI_NUM_OPEN_NETWORKS_KEPT, 10);

        if(config == null) {
            //새로운 네트워크

            textViewStatus.setVisibility(View.GONE);
            textViewSpeed.setVisibility(View.GONE);
            textViewAddress.setVisibility(View.GONE);

            if(strSec.equals("Open"))
                editTextPassword.setVisibility(View.GONE);
            else
                editTextPassword.setVisibility(View.VISIBLE);

            builder.setView(alertView);
            builder.setPositiveButton("연결", new HsDialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    boolean connResult;
                    if (editTextPassword.getVisibility() == View.GONE) {
                        connResult = HsWifiUtil.connectToNewNetwork(getContext(), mHsWifiScanner.getWifiManager(), scanResult, null, numOpenNetworksKept);
                    } else {
                        String password = editTextPassword.getText().toString();
                        connResult = HsWifiUtil.connectToNewNetwork(getContext(), mHsWifiScanner.getWifiManager(), scanResult, password, numOpenNetworksKept);
                    }
                    if (!connResult)
                        Toast.makeText(getContext(), "연결 실패", Toast.LENGTH_LONG).show();
                }
            });
            builder.setNegativeButton("취소", null);
            builder.show();

        }else{
            final boolean isCurrentNetwork_ConfigurationStatus = config.status == WifiConfiguration.Status.CURRENT;
            final WifiInfo info = mHsWifiScanner.getWifiManager().getConnectionInfo();
            final boolean isCurrentNetwork_WifiInfo = info != null
                    && android.text.TextUtils.equals(info.getSSID(), scanResult.SSID)
                    && android.text.TextUtils.equals(info.getBSSID(), scanResult.BSSID);

            if(isCurrentNetwork_ConfigurationStatus || isCurrentNetwork_WifiInfo) {
                //현재 연결된 정보

                textViewStatus.setVisibility(View.GONE);
                textViewSpeed.setVisibility(View.GONE);
                textViewAddress.setVisibility(View.GONE);
                editTextPassword.setVisibility(View.GONE);

                if(info == null) {
                    Toast.makeText(getContext(), "연결 실패", Toast.LENGTH_LONG).show();
                }else{
                    final SupplicantState state = info.getSupplicantState();
                    final NetworkInfo.DetailedState detailedState = WifiInfo.getDetailedStateOf(state);
                    if(detailedState == NetworkInfo.DetailedState.CONNECTED||(detailedState == NetworkInfo.DetailedState.OBTAINING_IPADDR && info.getIpAddress() != 0)) {
                        textViewStatus.setVisibility(View.VISIBLE);
                        textViewSpeed.setVisibility(View.VISIBLE);
                        textViewAddress.setVisibility(View.VISIBLE);

                        textViewStatus.setText("연결됨");
                        textViewSpeed.setText(info.getLinkSpeed()+" "+WifiInfo.LINK_SPEED_UNITS);
                        textViewAddress.setText(getIPAddress(info.getIpAddress()));
                    }else if(detailedState == NetworkInfo.DetailedState.AUTHENTICATING
                            || detailedState == NetworkInfo.DetailedState.CONNECTING
                            || detailedState == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                        textViewStatus.setVisibility(View.VISIBLE);
                        textViewStatus.setText("연결중...");
                    }
                }

                builder.setView(alertView);
                builder.setPositiveButton("삭제", new HsDialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final WifiConfiguration config = HsWifiUtil.getWifiConfiguration(mHsWifiScanner.getWifiManager(), scanResult, security);
                        boolean result = false;
                        if(config != null) {
                            result = mHsWifiScanner.getWifiManager().removeNetwork(config.networkId)
                                    && mHsWifiScanner.getWifiManager().saveConfiguration();
                        }
                        if(!result) {
                            Toast.makeText(getContext(), "실패!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                builder.setNegativeButton("취소", null);
                builder.show();
            }else{
                //기존 연결설정 했었던 정

                textViewStatus.setVisibility(View.GONE);
                textViewSpeed.setVisibility(View.GONE);
                textViewAddress.setVisibility(View.GONE);
                editTextPassword.setVisibility(View.VISIBLE);

                builder.setView(alertView);
                builder.setPositiveButton("연결", new HsDialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final WifiConfiguration config = HsWifiUtil.getWifiConfiguration(mHsWifiScanner.getWifiManager(), scanResult, security);
                        boolean connResult = false;
                        if(config != null) {
                            connResult = HsWifiUtil.connectToConfiguredNetwork(getContext(), mHsWifiScanner.getWifiManager(), config, false);
                        }
                        if(!connResult) {
                            Toast.makeText(getContext(), "실패!!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                builder.setNeutralButton("비번 변경", new HsDialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(editTextPassword.getText().length() > 0){
                            final WifiConfiguration config = HsWifiUtil.getWifiConfiguration(mHsWifiScanner.getWifiManager(), scanResult, security);
                            boolean saveResult = false;
                            if(config != null) {
                                saveResult = HsWifiUtil.changePasswordAndConnect(getContext(), mHsWifiScanner.getWifiManager(), config
                                        , editTextPassword.getText().toString()
                                        , numOpenNetworksKept);
                            }

                            if(!saveResult) {
                                Toast.makeText(getContext(), "실패!", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                builder.setNegativeButton("취소", null);
                builder.show();
            }
        }


    }


    class Holder extends HsRecyclerView.HsViewHolder{
        ImageView mImageViewSig;
        TextView mTextViewSSID;
        TextView mTextViewSec;

        public Holder(View itemView) {
            super(itemView);
            mImageViewSig = (ImageView) findViewById(R.id.ImageViewWifiSig);
            mTextViewSSID = (TextView) findViewById(R.id.TextViewWifiSSID);
            mTextViewSec = (TextView) findViewById(R.id.TextViewWifiSec);
        }
    }

    //와이파이 어댑터
    class Adapter extends HsRecyclerView.HsAdapter<Holder>{
        List<ScanResult> mScanResults = null;

        public Adapter() {
            mScanResults = new ArrayList<>();
        }

        @Override
        public Holder onCreateHsViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.viewholder_wifi_item, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindHsViewHolder(Holder holder, int i, boolean b) {
            String ssid = getItem(i).SSID;
            if(ssid == null || ssid.length() == 0)
                holder.mTextViewSSID.setText(getItem(i).BSSID);
            else
                holder.mTextViewSSID.setText(ssid);


            int difference = WifiManager.calculateSignalLevel(getItem(i).level, 5);
            if(difference >= 4)
                holder.mImageViewSig.setImageResource(R.drawable.ic_wifi_sig_4_20dp);
            else if(difference >= 3)
                holder.mImageViewSig.setImageResource(R.drawable.ic_wifi_sig_3_20dp);
            else if(difference >= 2)
                holder.mImageViewSig.setImageResource(R.drawable.ic_wifi_sig_2_20dp);
            else if(difference >= 1)
                holder.mImageViewSig.setImageResource(R.drawable.ic_wifi_sig_1_20dp);
            else
                holder.mImageViewSig.setImageResource(R.drawable.ic_wifi_sig_0_20dp);

            boolean isAdHoc = HsWifiUtil.isAdHoc(getItem(i));
            if(isAdHoc){
                holder.mImageViewSig.setColorFilter(getColorCompat(R.color.colorRed500));
            }else{
                holder.mImageViewSig.setColorFilter(getColorCompat(R.color.colorBlack));
            }

            holder.mTextViewSec.setText(String.format("보안 : %s", HsWifiUtil.getReadableSecurity(getItem(i))));
        }

        @Override
        public int getHsItemCount() {
            return mScanResults.size();
        }

        @Override
        protected ScanResult getItem(int position) {
            return mScanResults.get(position);
        }

        public void clear(){
            mScanResults.clear();
        }
        public void addAll(List<ScanResult> scanResults){
            mScanResults.addAll(scanResults);
        }
        public void add(ScanResult scanResult){
            mScanResults.add(scanResult);
        }
        public void remove(ScanResult scanResult){
            mScanResults.remove(scanResult);
        }
    }


    private String getIPAddress(int address) {
        StringBuilder sb = new StringBuilder();
        sb.append(address & 0x000000FF).append(".")
                .append((address & 0x0000FF00) >> 8).append(".")
                .append((address & 0x00FF0000) >> 16).append(".")
                .append((address & 0xFF000000L) >> 24);
        return sb.toString();
    }

}
