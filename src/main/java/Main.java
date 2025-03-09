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
        Bencode bencode = new Bencode(false);  // Normal decoding
        Bencode strictBencode = new Bencode(true);  // Strict encoding

        // Decode entire .torrent file
        Map<String, Object> root = bencode.decode(bytes, Type.DICTIONARY);
        
        // Extract "announce" and "info" fields
        announce = (String) root.get("announce");
        Map<String, Object> info = (Map<String, Object>) root.get("info");
        length = (long) info.get("length");

        // Encode "info" dictionary exactly as it appears in the file
        byte[] bencodedInfo = strictBencode.encode(info);

        // Compute SHA-1 hash of the bencoded "info" dictionary
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        infoHash = digest.digest(bencodedInfo);
    }
}
