import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Invalid command or missing arguments");
            return;
        }

        String command = args[0];

        if (command.equals("decode")) {
            String bencodedValue = args[1];
            String decoded;
            switch (bencodedValue.charAt(0)) {
                case 'i' -> {
                    Bencode bencode = new Bencode(true);
                    decoded = "" + bencode.decode(bencodedValue.getBytes(), Type.NUMBER);
                }
                case 'l' -> {
                    Bencode bencode = new Bencode(false);
                    decoded = gson.toJson(bencode.decode(bencodedValue.getBytes(), Type.LIST));
                }
                case 'd' -> {
                    Bencode bencode = new Bencode(false);
                    decoded = gson.toJson(bencode.decode(bencodedValue.getBytes(), Type.DICTIONARY));
                }
                default -> {
                    try {
                        decoded = gson.toJson(decodeBencode(bencodedValue));
                    } catch (RuntimeException e) {
                        System.out.println(e.getMessage());
                        return;
                    }
                }
            }
            System.out.println(decoded);
        } else if (command.equals("info")) {
            String filePath = args[1];
            Torrent torrent = new Torrent(Files.readAllBytes(Path.of(filePath)));

            System.out.println("Tracker URL: " + torrent.announce);
            System.out.println("Length: " + torrent.length);
            System.out.println("Info Hash: " + bytesToHex(torrent.infoHash));

        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    private static String decodeBencode(String bencodedString) {
        if (Character.isDigit(bencodedString.charAt(0))) {
            int firstColonIndex = bencodedString.indexOf(':');
            int length = Integer.parseInt(bencodedString.substring(0, firstColonIndex));
            return bencodedString.substring(firstColonIndex + 1, firstColonIndex + 1 + length);
        } else {
            throw new RuntimeException("Only strings are supported at the moment");
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
        Bencode bencode = new Bencode(false);
        Bencode sortedBencode = new Bencode(true); // Ensure correct sorting for hashing

        Map<String, Object> root = bencode.decode(bytes, Type.DICTIONARY);
        Map<String, Object> info = (Map<String, Object>) root.get("info");

        announce = (String) root.get("announce");
        length = (long) info.get("length");

        // Compute SHA-1 hash of the bencoded `info` dictionary
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        infoHash = digest.digest(sortedBencode.encode(info));
    }
}