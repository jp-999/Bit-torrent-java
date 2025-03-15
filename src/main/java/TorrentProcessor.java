import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class TorrentProcessor {
    public static void processTorrentFile(String torrentFile) throws IOException, NoSuchAlgorithmException {
        byte[] torrentFileContents = Files.readAllBytes(Path.of(torrentFile));
        Map<?, ?> decodedResult = (Map<?, ?>) BencodeUtils.decodeBencode(new ByteArrayInputStream(torrentFileContents));
        String announceString = (String) decodedResult.get("announce");
        Map<?, ?> infoMap = (Map<?, ?>) decodedResult.get("info");

        long length = (long) infoMap.get("length");
        byte[] infoHash = getInfoHash(infoMap);

        System.out.println("Tracker URL: " + announceString);
        System.out.println("Length: " + length);
        System.out.println("Info Hash: " + bytesToHex(infoHash));
    }

    public static byte[] getInfoHash(Map<?, ?> info) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BencodeUtils.encodeBencode(info, byteArrayOutputStream);
        byte[] infoBytes = byteArrayOutputStream.toByteArray();
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return digest.digest(infoBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
} 