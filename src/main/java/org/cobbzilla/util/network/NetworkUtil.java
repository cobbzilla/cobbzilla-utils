package org.cobbzilla.util.network;

import com.sun.jna.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.util.collection.ExpirationMap;
import org.cobbzilla.util.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.HttpSchemes.SCHEME_HTTPS;
import static org.cobbzilla.util.http.HttpSchemes.isHttpOrHttps;
import static org.cobbzilla.util.http.HttpUtil.url2string;
import static org.cobbzilla.util.http.URIUtil.toUri;
import static org.cobbzilla.util.string.ValidationRegexes.IPv4_PATTERN;

@Slf4j
public class NetworkUtil {

    public static final String IPv4_ALL_ADDRS = "0.0.0.0";
    public static final String IPv4_LOCALHOST = "127.0.0.1";

    public static boolean isLocalIpv4(String addr) {
        if (empty(addr)) return false;
        if (addr.startsWith("/")) addr = addr.substring(1);
        if (!IPv4_PATTERN.matcher(addr).matches()) return false;
        if (addr.startsWith("127.")) return true;
        return false;
    }

    private static final Map<String, Boolean> localhostCache = new ExpirationMap<>();

    public static boolean isLocalHost(String host) {
        return localhostCache.computeIfAbsent(host, NetworkUtil::determineLocalHost);
    }

    private static Boolean determineLocalHost(String host) {
        if (isLocalIpv4(host)) return true;
        String hostAddress = null;
        try {
            hostAddress = InetAddress.getByName(host).getHostAddress();
            if (isLocalIpv4(hostAddress)) return true;
        } catch (Exception e) {
            log.warn("isLocalHost("+host+"): "+e);
        }
        final String ex = getExternalIp();
        return ex != null && ex.equals(hostAddress != null ? hostAddress : host);
    }

    public static boolean isPublicIpv4(String addr) {
        if (empty(addr)) return false;
        if (addr.startsWith("/")) addr = addr.substring(1);
        if (!IPv4_PATTERN.matcher(addr).matches()) return false;
        try {
            final InetAddress address = InetAddress.getByName(addr);
            if (address.isSiteLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()) return false;
            return !isLocalIpv4(addr) && !isPrivateIp4(addr);
        } catch (Exception e) {
            log.warn("isPublicIpv4: "+e);
            return false;
        }
    }

    public static boolean isPrivateIp4(String addr) {
        if (addr.startsWith("10.")) return true;
        if (addr.startsWith("192.168.")) return true;
        if (addr.startsWith("172.")) {
            final String remainder = addr.substring("172.".length());
            final int secondDot = remainder.indexOf(".");
            final String secondPart = remainder.substring(0, secondDot);
            final int octet = Integer.parseInt(secondPart);
            return octet >= 16 && octet <= 31;
        }
        return false;
    }

    public static String getEthernetIpv4(NetworkInterface iface) {
        if (iface == null) return null;
        if (!iface.getName().startsWith(getEthernetInterfacePrefix())) return null;
        final Enumeration<InetAddress> addrs = iface.getInetAddresses();
        while (addrs.hasMoreElements()) {
            String addr = addrs.nextElement().toString();
            if (addr.startsWith("/")) addr = addr.substring(1);
            if (!IPv4_PATTERN.matcher(addr).matches()) continue;
            return addr;
        }
        return null;
    }

    protected static String getEthernetInterfacePrefix() {
        if (Platform.isWindows() || Platform.isLinux()) return "eth";
        if (Platform.isMac()) return "en";
        return die("getEthernetInterfacePrefix: unknown platform "+System.getProperty("os.name"));
    }

    protected static String getLocalInterfacePrefix() {
        return "lo";
    }

    public static String getLocalhostIpv4 () {
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface i = interfaces.nextElement();
                if (i.getName().startsWith(getLocalInterfacePrefix())) {
                    final Enumeration<InetAddress> addrs = i.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        String addr = addrs.nextElement().toString();
                        if (addr.startsWith("/")) addr = addr.substring(1);
                        if (isLocalIpv4(addr)) return addr;
                    }
                }
            }
            return die("getLocalhostIpv4: no local 127.x.x.x address found");

        } catch (Exception e) {
            return die("getLocalhostIpv4: "+e, e);
        }
    }

    public static Set<String> configuredIps() {
        final Set<String> ips = new HashSet<>();
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface i = interfaces.nextElement();
                final Enumeration<InetAddress> addrs = i.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    String ip = addrs.nextElement().toString();
                    if (ip.startsWith("/")) ip = ip.substring(1);
                    ips.add(ip);
                }
            }
            return ips;
        } catch (Exception e) {
            return die("configuredIps: "+e, e);
        }
    }

    public static Set<String> configuredIpsAndExternalIp() {
        final Set<String> ips = configuredIps();
        final String externalIp = getExternalIp();
        if (externalIp != null) ips.add(externalIp);
        return ips;
    }

    @Getter(lazy=true) private static final String externalIp = initExternalIp();
    private static String initExternalIp() {
        try {
            return url2string("http://checkip.amazonaws.com/").trim();
        } catch (Exception e) {
            log.warn("initExternalIp: returning null due to: "+e);
            return null;
        }
    }

    public static String getFirstPublicIpv4() {
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface i = interfaces.nextElement();
                final Enumeration<InetAddress> addrs = i.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    final String addr = addrs.nextElement().toString();
                    if (isPublicIpv4(addr)) {
                        return addr.substring(1);
                    }
                }
            }
            log.debug("getFirstPublicIpv4: no public IPv4 address found");
            return null;

        } catch (Exception e) {
            return die("getFirstPublicIpv4: "+e, e);
        }
    }

    public static String getFirstEthernetIpv4() {
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface i = interfaces.nextElement();
                final String addr = getEthernetIpv4(i);
                if (!empty(addr)) return addr;
            }
            log.warn("getFirstEthernetIpv4: no ethernet IPv4 address found");
            return null;

        } catch (Exception e) {
            return die("getFirstPublicIpv4: "+e, e);
        }
    }

    public static String getInAddrArpa(String ip) {
        final String[] parts = ip.split("\\.");
        return new StringBuilder()
                .append(parts[3]).append('.')
                .append(parts[2]).append('.')
                .append(parts[1]).append('.')
                .append(parts[0]).append(".in-addr.arpa")
                .toString();
    }

    public static boolean ipEquals(String addr1, String addr2) {
        try {
            return InetAddress.getByName(addr1).equals(InetAddress.getByName(addr2));
        } catch (Exception e) {
            log.warn("ipEquals: "+e);
            return false;
        }
    }

    // adapted from https://stackoverflow.com/a/19238983/1251543
    public static Inet6Address big2ip6(BigInteger ipNumber) throws UnknownHostException {
        String ipString = "";
        final BigInteger a = new BigInteger("FFFF", 16);
        for (int i=0; i<8; i++) {
            ipString = ipNumber.and(a).toString(16)+":"+ipString;
            ipNumber = ipNumber.shiftRight(16);
        }
        return (Inet6Address) Inet6Address.getByName(ipString.substring(0, ipString.length()-1));
    }
    public static Inet4Address big2ip4(BigInteger ipNumber) throws UnknownHostException {
        String ipString = "";
        final BigInteger a = new BigInteger("FF", 16);
        for (int i=0; i<4; i++) {
            ipString = ipNumber.and(a).toString(10)+"."+ipString;
            ipNumber = ipNumber.shiftRight(8);
        }
        return (Inet4Address) Inet4Address.getByName(ipString.substring(0, ipString.length()-1));
    }

    public static Set<String> toHostSet(File file) throws IOException {
        return toHostSet(FileUtil.toStringList(file));
    }

    public static Set<String> toHostSet(Collection<String> vals) {
        return vals.stream()
                .map(NetworkUtil::normalizeHost)
                .collect(Collectors.toSet());
    }

    public static String normalizeHost(String s) {
        return isHttpOrHttps(s)
                ? toUri(s).getHost()
                : toUri(SCHEME_HTTPS + s).getHost();
    }

    public static String randomLocalIp4() {
        final StringBuilder addr = new StringBuilder("127");
        for (int i=0; i<3; i++) {
            addr.append(".").append(RandomUtils.nextInt(1, 255));
        }
        return addr.toString();
    }

    public static String randomLocalIp6() {
        final StringBuilder addr = new StringBuilder("fd00");
        for (int i=0; i<7; i++) {
            addr.append(":").append(Integer.toHexString(RandomUtils.nextInt(1, 65536)));
        }
        return addr.toString();
    }
}
