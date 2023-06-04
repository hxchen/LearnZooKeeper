package com.example.vrgame.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class OsUtil {
    public static List<String> getLocalIpList(boolean includeLoopbackAddress) {
        List<String> ipList = new ArrayList<String>();
        Enumeration<NetworkInterface> e;
        try {
            e = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e1) {
            throw new RuntimeException(e1);
        }
        while (e.hasMoreElements()) {
            NetworkInterface ni = e.nextElement();
            Enumeration<InetAddress> ee = ni.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress address = ee.nextElement();
                if (includeLoopbackAddress || !address.isLoopbackAddress()) {
                    String ip = address.getHostAddress();
                    ipList.add(ip);
                }
            }
        }
        return ipList;
    }
}
