package kr.co.hs.wifiscanner;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by privacydev on 2017. 8. 30..
 */

public class HsWifiUtil implements HsWifiConstant{
    public static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    public static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    public static String getReadableSecurity(int sec){
        switch (sec){
            case SECURITY_WEP:return "WEP";
            case SECURITY_PSK:return "PSK";
            case SECURITY_EAP:return "EAP";
            default:return "Open";
        }
    }

    public static String getReadableSecurity(ScanResult scanResult){
        return getReadableSecurity(getSecurity(scanResult));
    }

    public static boolean isAdHoc(final ScanResult scanResult) {
        return scanResult.capabilities.indexOf("IBSS") != -1;
    }


    public static String convertToQuotedString(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }

        final int lastPos = string.length() - 1;
        if(lastPos > 0 && (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
            return string;
        }

        return "\"" + string + "\"";
    }

    private static final String BSSID_ANY = "any";
    public static WifiConfiguration getWifiConfiguration(final WifiManager wifiMgr, final ScanResult hotsopt, int hotspotSecurity) {
        final String ssid = convertToQuotedString(hotsopt.SSID);
        if(ssid.length() == 0) {
            return null;
        }

        final String bssid = hotsopt.BSSID;
        if(bssid == null) {
            return null;
        }

        if(hotspotSecurity == -1) {
            hotspotSecurity = getSecurity(hotsopt);
        }

        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        if(configurations == null) {
            return null;
        }

        for(final WifiConfiguration config : configurations) {
            if(config.SSID == null || !ssid.equals(config.SSID)) {
                continue;
            }
            if(config.BSSID == null || BSSID_ANY.equals(config.BSSID) ||  bssid.equals(config.BSSID)) {
                final int configSecurity = getSecurity(config);
                if(hotspotSecurity == configSecurity) {
                    return config;
                }
            }
        }
        return null;
    }



    private static boolean checkForExcessOpenNetworkAndSave(final WifiManager wifiMgr, final int numOpenNetworksKept) {
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);

        boolean modified = false;
        int tempCount = 0;
        for(int i = configurations.size() - 1; i >= 0; i--) {
            final WifiConfiguration config = configurations.get(i);
            int security = getSecurity(config);
            if(security == SECURITY_NONE){
                tempCount++;
                if(tempCount >= numOpenNetworksKept) {
                    modified = true;
                    wifiMgr.removeNetwork(config.networkId);
                }
            }
        }
        if(modified) {
            return wifiMgr.saveConfiguration();
        }

        return true;
    }

    private static void sortByPriority(final List<WifiConfiguration> configurations) {
        Collections.sort(configurations, new Comparator<WifiConfiguration>() {
            @Override
            public int compare(WifiConfiguration object1, WifiConfiguration object2) {
                return object1.priority - object2.priority;
            }
        });
    }

    private static void setupSecurity(WifiConfiguration config, int security, String password){
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        final int passwordLen = password == null ? 0 : password.length();
        switch (security) {
            case SECURITY_NONE:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;

            case SECURITY_WEP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                if (passwordLen != 0) {
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((passwordLen == 10 || passwordLen == 26 || passwordLen == 58) &&
                            password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case SECURITY_PSK:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                if (passwordLen != 0) {
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;

            case SECURITY_EAP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
//            config.eap.setValue((String) mEapMethodSpinner.getSelectedItem());
//
//            config.phase2.setValue((mPhase2Spinner.getSelectedItemPosition() == 0) ? "" :
//                    "auth=" + mPhase2Spinner.getSelectedItem());
//            config.ca_cert.setValue((mEapCaCertSpinner.getSelectedItemPosition() == 0) ? "" :
//                    KEYSTORE_SPACE + Credentials.CA_CERTIFICATE +
//                    (String) mEapCaCertSpinner.getSelectedItem());
//            config.client_cert.setValue((mEapUserCertSpinner.getSelectedItemPosition() == 0) ?
//                    "" : KEYSTORE_SPACE + Credentials.USER_CERTIFICATE +
//                    (String) mEapUserCertSpinner.getSelectedItem());
//            config.private_key.setValue((mEapUserCertSpinner.getSelectedItemPosition() == 0) ?
//                    "" : KEYSTORE_SPACE + Credentials.USER_PRIVATE_KEY +
//                    (String) mEapUserCertSpinner.getSelectedItem());
//            config.identity.setValue((mEapIdentityView.length() == 0) ? "" :
//                    mEapIdentityView.getText().toString());
//            config.anonymous_identity.setValue((mEapAnonymousView.length() == 0) ? "" :
//                    mEapAnonymousView.getText().toString());
//            if (mPasswordView.length() != 0) {
//                config.password.setValue(mPasswordView.getText().toString());
//            }
                break;
        }
    }

    public static boolean connectToNewNetwork(final Context ctx, final WifiManager wifiMgr, final ScanResult scanResult, final String password, final int numOpenNetworksKept) {
        final int security = getSecurity(scanResult);

        if(security == SECURITY_NONE) {
            checkForExcessOpenNetworkAndSave(wifiMgr, numOpenNetworksKept);
        }

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = convertToQuotedString(scanResult.SSID);
        config.BSSID = scanResult.BSSID;
        setupSecurity(config, security, password);

        int id = -1;
        try {
            id = wifiMgr.addNetwork(config);
        } catch(NullPointerException e) {
            // Weird!! Really!!
            // This exception is reported by user to Android Developer Console(https://market.android.com/publish/Home)
        }
        if(id == -1) {
            return false;
        }

        if(!wifiMgr.saveConfiguration()) {
            return false;
        }

        config = getWifiConfiguration(wifiMgr, config, security);
        if(config == null) {
            return false;
        }

        return connectToConfiguredNetwork(ctx, wifiMgr, config, true);
    }

    private static final int MAX_PRIORITY = 99999;

    public static boolean connectToConfiguredNetwork(final Context ctx, final WifiManager wifiMgr, WifiConfiguration config, boolean reassociate) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return connectToConfiguredNetworkV23(ctx, wifiMgr, config, reassociate);
        }
        final int security = getSecurity(config);

        int oldPri = config.priority;
        // Make it the highest priority.
        int newPri = getMaxPriority(wifiMgr) + 1;
        if(newPri > MAX_PRIORITY) {
            newPri = shiftPriorityAndSave(wifiMgr);
            config = getWifiConfiguration(wifiMgr, config, security);
            if(config == null) {
                return false;
            }
        }

        // Set highest priority to this configured network
        config.priority = newPri;
        int networkId = wifiMgr.updateNetwork(config);
        if(networkId == -1) {
            return false;
        }

        // Do not disable others
        if(!wifiMgr.enableNetwork(networkId, false)) {
            config.priority = oldPri;
            return false;
        }

        if(!wifiMgr.saveConfiguration()) {
            config.priority = oldPri;
            return false;
        }

        // We have to retrieve the WifiConfiguration after save.
        config = getWifiConfiguration(wifiMgr, config, security);
        if(config == null) {
            return false;
        }

        ReenableAllApsWhenNetworkStateChanged.schedule(ctx);

        // Disable others, but do not save.
        // Just to force the WifiManager to connect to it.
        if(!wifiMgr.enableNetwork(config.networkId, true)) {
            return false;
        }

        final boolean connect = reassociate ? wifiMgr.reassociate() : wifiMgr.reconnect();
        if(!connect) {
            return false;
        }

        return true;
    }

    private static boolean connectToConfiguredNetworkV23(final Context ctx, final WifiManager wifiMgr, WifiConfiguration config, boolean reassociate) {
        if(!wifiMgr.enableNetwork(config.networkId, true)) {
            return false;
        }

        return reassociate ? wifiMgr.reassociate() : wifiMgr.reconnect();
    }

    private static int getMaxPriority(final WifiManager wifiManager) {
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        int pri = 0;
        for(final WifiConfiguration config : configurations) {
            if(config.priority > pri) {
                pri = config.priority;
            }
        }
        return pri;
    }

    private static int shiftPriorityAndSave(final WifiManager wifiMgr) {
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);
        final int size = configurations.size();
        for(int i = 0; i < size; i++) {
            final WifiConfiguration config = configurations.get(i);
            config.priority = i;
            wifiMgr.updateNetwork(config);
        }
        wifiMgr.saveConfiguration();
        return size;
    }

    public static WifiConfiguration getWifiConfiguration(final WifiManager wifiMgr, final WifiConfiguration configToFind, int security) {
        final String ssid = configToFind.SSID;
        if(ssid.length() == 0) {
            return null;
        }

        final String bssid = configToFind.BSSID;


        if(security == -1) {
            security = getSecurity(configToFind);
        }

        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();

        for(final WifiConfiguration config : configurations) {
            if(config.SSID == null || !ssid.equals(config.SSID)) {
                continue;
            }
            if(config.BSSID == null || BSSID_ANY.equals(config.BSSID) || bssid == null || bssid.equals(config.BSSID)) {
                final int configSecurity = getSecurity(config);
                if(security == configSecurity) {
                    return config;
                }
            }
        }
        return null;
    }

    public static boolean changePasswordAndConnect(final Context ctx, final WifiManager wifiMgr, final WifiConfiguration config, final String newPassword, final int numOpenNetworksKept) {
        setupSecurity(config, getSecurity(config), newPassword);
        final int networkId = wifiMgr.updateNetwork(config);
        if(networkId == -1) {
            // Update failed.
            return false;
        }
        // Force the change to apply.
        wifiMgr.disconnect();
        return connectToConfiguredNetwork(ctx, wifiMgr, config, true);
    }
}
