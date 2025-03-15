import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Main {
    private static final Gson gson = new Gson();
    private static final String PEER_ID = "-PC0001-123456789012"; // 20 bytes peer id

    public static void main(String[] args) throws Exception {
        String command = args[0];

        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            try {
                String decoded = gson.toJson(BencodeUtils.decodeBencode(new ByteArrayInputStream(bencodedValue.getBytes())));
                System.out.println(decoded);
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
            }
        } else if ("info".equals(command)) {
            TorrentProcessor.processTorrentFile(args[1]);
        } else if ("peers".equals(command)) {
            String torrentPath = args[1];
            getPeers(torrentPath);
        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    private static void getPeers(String torrentPath) throws Exception {
        byte[] torrentFileContents = Files.readAllBytes(Path.of(torrentPath));
        Map<?, ?> decodedResult = (Map<?, ?>) BencodeUtils.decodeBencode(new ByteArrayInputStream(torrentFileContents));
        
        String announceUrl = (String) decodedResult.get("announce");
        Map<?, ?> info = (Map<?, ?>) decodedResult.get("info");
        long fileLength = (long) info.get("length");
        
        // Get raw info hash (20 bytes)
        byte[] infoHash = TorrentProcessor.getInfoHash(info);
        
        // Build tracker URL with parameters
        String trackerUrl = buildTrackerUrl(announceUrl, infoHash, fileLength);
        
        // Make HTTP GET request
        byte[] response = makeGetRequest(trackerUrl);
        
        // Decode and parse response
        Map<?, ?> trackerResponse = (Map<?, ?>) BencodeUtils.decodeBencode(new ByteArrayInputStream(response));
        String peersData = (String) trackerResponse.get("peers");
        
        // Parse peers
        List<Peer> peers = parsePeers(peersData);
        
        // Print results
        for (Peer peer : peers) {
            System.out.println(peer.toString());
        }
    }

    private static String buildTrackerUrl(String announceUrl, byte[] infoHash, long fileLength) 
            throws UnsupportedEncodingException {
        StringBuilder url = new StringBuilder(announceUrl);
        url.append("?info_hash=").append(urlEncodeBytes(infoHash));
        url.append("&peer_id=").append(URLEncoder.encode(PEER_ID, "UTF-8"));
        url.append("&port=6881");
        url.append("&uploaded=0");
        url.append("&downloaded=0");
        url.append("&left=").append(fileLength);
        url.append("&compact=1");
        return url.toString();
    }

    private static String urlEncodeBytes(byte[] bytes) {
        StringBuilder encoded = new StringBuilder();
        for (byte b : bytes) {
            // URL encode each byte of the info hash
            String hex = String.format("%02x", b & 0xFF);
            encoded.append('%').append(hex);
        }
        return encoded.toString();
    }

    private static byte[] makeGetRequest(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "BitTorrent/7.10.3");
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (var in = conn.getInputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    private static List<Peer> parsePeers(String peersData) {
        List<Peer> peers = new ArrayList<>();
        byte[] peerBytes = peersData.getBytes(StandardCharsets.ISO_8859_1);
        
        // Each peer entry is exactly 6 bytes (4 for IP, 2 for port)
        for (int i = 0; i < peerBytes.length; i += 6) {
            if (i + 5 >= peerBytes.length) {
                break;  // Avoid buffer overflow
            }
            
            // Convert bytes to IP address components
            int b1 = peerBytes[i] & 0xFF;
            int b2 = peerBytes[i + 1] & 0xFF;
            int b3 = peerBytes[i + 2] & 0xFF;
            int b4 = peerBytes[i + 3] & 0xFF;
            
            // Build IP address string
            String ip = String.format("%d.%d.%d.%d", b1, b2, b3, b4);
            
            // Convert last two bytes to port number (big-endian)
            int port = ((peerBytes[i + 4] & 0xFF) << 8) | (peerBytes[i + 5] & 0xFF);
            
            // Add all peers to the list
            peers.add(new Peer(ip, port));
        }
        return peers;
    }
}

class Peer {
    private final String ip;
    private final int port;

    public Peer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", ip, port);
    }
}
