package eu.siacs.conversations.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import eu.siacs.conversations.Config;

public class SocksSocketFactory {

    public static void createSocksConnection(final Socket socket, final String destination, final int port) throws IOException {
        final InputStream proxyIs = socket.getInputStream();
        final OutputStream proxyOs = socket.getOutputStream();
        proxyOs.write(new byte[]{0x05, 0x01, 0x00});
        proxyOs.flush();
        final byte[] handshake = new byte[2];
        proxyIs.read(handshake);
        if (handshake[0] != 0x05 || handshake[1] != 0x00) {
            throw new SocksConnectionException("Socks 5 handshake failed");
        }
        final byte[] dest = destination.getBytes();
        final ByteBuffer request = ByteBuffer.allocate(7 + dest.length);
        request.put(new byte[]{0x05, 0x01, 0x00, 0x03});
        request.put((byte) dest.length);
        request.put(dest);
        request.putShort((short) port);
        proxyOs.write(request.array());
        proxyOs.flush();
        final byte[] response = new byte[7 + dest.length];
        proxyIs.read(response);
        if (response[1] != 0x00) {
            if (response[1] == 0x04) {
                throw new HostNotFoundException("Host unreachable");
            }
            if (response[1] == 0x05) {
                throw new HostNotFoundException("Connection refused");
            }
            throw new SocksConnectionException("Unable to connect to destination " + (int) (response[1]));
        }
    }

    public static boolean contains(byte needle, byte[] haystack) {
        for (byte hay : haystack) {
            if (hay == needle) {
                return true;
            }
        }
        return false;
    }

    private static Socket createSocket(InetSocketAddress address, String destination, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(address, Config.CONNECT_TIMEOUT * 1000);
        } catch (IOException e) {
            throw new SocksProxyNotFoundException();
        }
        createSocksConnection(socket, destination, port);
        return socket;
    }

    public static Socket createSocketOverTor(String destination, int port) throws IOException {
        return createSocket(new InetSocketAddress(InetAddress.getLocalHost(), 9050), destination, port);
    }

    private static class SocksConnectionException extends IOException {
        SocksConnectionException(String message) {
            super(message);
        }
    }

    public static class SocksProxyNotFoundException extends IOException {

    }

    public static class HostNotFoundException extends SocksConnectionException {
        HostNotFoundException(String message) {
            super(message);
        }
    }
}