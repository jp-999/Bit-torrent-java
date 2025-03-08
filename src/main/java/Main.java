import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Map;

public class Main {
    private static final Gson gson = new Gson();
    private static final Bencode bencode = new Bencode();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Invalid command or missing arguments");
            return;
        }

        String command = args[0];

        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            Object decoded;
            try {
                decoded = bencode.decode(bencodedValue.getBytes(), bencode.type(bencodedValue.getBytes()));
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(gson.toJson(decoded));
        } else if ("info".equals(command)) {
            byte[] torrentFile = Files.readAllBytes(Paths.get(args[1]));
            Map<String, Object> torrentFileDecoded = bencode.decode(torrentFile, Type.DICTIONARY);

            // Extract Tracker URL
            if (torrentFileDecoded.containsKey("announce")) {
                String trackerUrl = (String) torrentFileDecoded.get("announce");
                System.out.printf("Tracker URL: %s\n", trackerUrl);
            }

            // Extract File Length
            if (torrentFileDecoded.containsKey("info")) {
                Map<String, Object> info = (Map<String, Object>) torrentFileDecoded.get("info");

                if (info.containsKey("length")) {
                    System.out.printf("Length: %s\n", info.get("length"));
                } else {
                    System.out.println("Length not found in torrent file.");
                }
            } else {
                System.out.println("Info dictionary not found.");
            }
        } else if ("info-hash".equals(command)) {
            byte[] torrentFile = Files.readAllBytes(Paths.get(args[1]));
            Map<String, Object> torrentFileDecoded = bencode.decode(torrentFile, Type.DICTIONARY);

            if (torrentFileDecoded.containsKey("info")) {
                Map<String, Object> info = (Map<String, Object>) torrentFileDecoded.get("info");

                // Re-bencode the `info` dictionary
                byte[] bencodedInfo = bencode.encode(info);

                // Compute SHA-1 hash
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] hashBytes = digest.digest(bencodedInfo);

                // Convert hash to hexadecimal format
                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    hexString.append(String.format("%02x", b));
                }

                // Print the info hash
                System.out.printf("Info Hash: %s\n", hexString.toString());
            } else {
                System.out.println("Info dictionary not found.");
            }
        } else {
            System.out.println("Unknown command: " + command);
        }
    }
}
