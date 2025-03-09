import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Invalid command or missing arguments");
            return;
        }

        String command = args[0];

        if (command.equals("info")) {
            String filePath = args[1];
            Torrent torrent = new Torrent(Files.readAllBytes(Path.of(filePath)));

            System.out.println("Tracker URL: " + torrent.announce);
            System.out.println("Length: " + torrent.length);
            System.out.println("Info Hash: " + bytesToHex(torrent.infoHash));
        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

class Torrent {
    public String announce;
    public long length;
    public byte[] infoHash;

    public Torrent(byte[] bytes) throws NoSuchAlgorithmException {
        Bencode bencode = new Bencode(true);  // Use strict mode for consistent encoding

        Map<String, Object> root = bencode.decode(bytes, Type.DICTIONARY);
        Map<String, Object> info = (Map<String, Object>) root.get("info");

        announce = (String) root.get("announce");
        length = (long) info.get("length");

        // Calculate the info hash using the original bytes
        int startIndex = findInfoStart(bytes);
        int endIndex = findInfoEnd(bytes, startIndex);
        
        if (startIndex >= 0 && endIndex > startIndex) {
            byte[] infoBytes = new byte[endIndex - startIndex];
            System.arraycopy(bytes, startIndex, infoBytes, 0, infoBytes.length);
            
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            infoHash = digest.digest(infoBytes);
        } else {
            throw new RuntimeException("Could not find info dictionary in torrent file");
        }
    }

    private int findInfoStart(byte[] bytes) {
        // Find the start of the info dictionary
        String content = new String(bytes);
        return content.indexOf("4:info") + 6; // Skip "4:info" to get to the dictionary start
    }

    private int findInfoEnd(byte[] bytes, int startIndex) {
        // Find the end of the info dictionary by counting nested dictionaries
        int depth = 1;
        for (int i = startIndex; i < bytes.length; i++) {
            if (bytes[i] == 'd') {
                depth++;
            } else if (bytes[i] == 'e') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
