import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
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
            System.out.println("Piece Length: " + torrent.pieceLength);
            System.out.println("Piece Hashes:");
            for (String pieceHash : torrent.pieceHashes) {
                System.out.println(pieceHash);
            }
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
    public int pieceLength;
    public List<String> pieceHashes = new ArrayList<>();

    public Torrent(byte[] bytes) throws NoSuchAlgorithmException {
        Bencode bencode = new Bencode(false); // Standard decoder
        Bencode strictBencode = new Bencode(true); // Strict encoder (preserves ordering)

        // Parse .torrent file
        Map<String, Object> root = bencode.decode(bytes, Type.DICTIONARY);

        // Extract announce URL and info dictionary
        announce = (String) root.get("announce");
        Map<String, Object> info = (Map<String, Object>) root.get("info");
        length = (long) info.get("length");

        // Extract piece length
        pieceLength = (int) info.get("piece length");

        // Extract and process pieces (20-byte SHA-1 hashes)
        byte[] pieces = (byte[]) info.get("pieces");
        for (int i = 0; i < pieces.length; i += 20) {
            byte[] pieceHash = new byte[20];
            System.arraycopy(pieces, i, pieceHash, 0, 20);
            pieceHashes.add(bytesToHex(pieceHash));
        }

        // Correctly bencode the "info" dictionary before hashing
        byte[] bencodedInfo = strictBencode.encode(info);

        // Compute SHA-1 hash of the bencoded "info" dictionary
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        infoHash = digest.digest(bencodedInfo);
    }
}
