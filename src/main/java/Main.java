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
import java.util.stream.Collectors;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        System.err.println("Logs from your program will appear here!");
        String command = args[0];

        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            try {
                String decoded = gson.toJson(decodeBencode(new ByteArrayInputStream(bencodedValue.getBytes())));
                System.out.println(decoded);
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
            }
        } else if ("info".equals(command)) {
            processTorrentFile(args[1]);
        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    private static void processTorrentFile(String torrentFile) throws IOException, NoSuchAlgorithmException {
        byte[] torrentFileContents = Files.readAllBytes(Path.of(torrentFile));
        Map<?, ?> decodedResult = (Map<?, ?>) decodeBencode(new ByteArrayInputStream(torrentFileContents));
        String announceString = (String) decodedResult.get("announce");
        Map<?, ?> infoMap = (Map<?, ?>) decodedResult.get("info");

        Number length = (infoMap.get("length") instanceof Integer)
                ? ((Integer) infoMap.get("length")).longValue()
                : (Number) infoMap.get("length");

        Long pieceLength = (Long) infoMap.get("piece length");
        byte[] pieces = (byte[]) infoMap.get("pieces");
        List<String> piecesHash = parsePieces(pieces);

        String torrentSha = calculateInfoHash(infoMap);

        System.out.println("Tracker URL: " + announceString);
        System.out.println("Length: " + length);
        System.out.println("Info Hash: " + torrentSha);
        System.out.println("Piece Length: " + pieceLength);
        System.out.println("Piece Hashes:");
        System.out.println(String.join("\n", piecesHash));
    }

    private static List<String> parsePieces(byte[] pieces) {
        List<String> piecesHash = new ArrayList<>();
        for (int i = 0; i < pieces.length; i += 20) {
            byte[] hash = new byte[20];
            System.arraycopy(pieces, i, hash, 0, 20);
            piecesHash.add(bytesToHex(hash));
        }
        return piecesHash;
    }

    private static String calculateInfoHash(Map<?, ?> infoMap) throws NoSuchAlgorithmException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        encodeBencode(infoMap, byteArrayOutputStream);
        byte[] infoBytes = byteArrayOutputStream.toByteArray();
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return bytesToHex(digest.digest(infoBytes));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static void encodeBencode(Object object, ByteArrayOutputStream out) throws IOException {
        switch (object) {
            case Map<?, ?> map -> encodeMap(map, out);
            case List<?> list -> encodeList(list, out);
            case String string -> encodeString(string, out);
            case Long number -> encodeInteger(number, out);
            default -> throw new RuntimeException("Unexpected Object Type: " + object);
        }
    }

    private static void encodeMap(Map<?, ?> map, ByteArrayOutputStream out) throws IOException {
        out.write('d');
        for (var entry : map.entrySet()) {
            encodeBencode(entry.getKey(), out);
            if ("pieces".equals(entry.getKey())) {
                encodeBytes((byte[]) entry.getValue(), out);
            } else {
                encodeBencode(entry.getValue(), out);
            }
        }
        out.write('e');
    }

    private static void encodeBytes(byte[] value, ByteArrayOutputStream out) throws IOException {
        out.write(Integer.toString(value.length).getBytes(StandardCharsets.UTF_8));
        out.write(':');
        out.write(value);
    }

    private static void encodeList(List<?> list, ByteArrayOutputStream out) throws IOException {
        out.write('l');
        for (var object : list) {
            encodeBencode(object, out);
        }
        out.write('e');
    }

    private static void encodeString(String string, ByteArrayOutputStream out) throws IOException {
        out.write(String.format("%s:%s", string.length(), string).getBytes(StandardCharsets.UTF_8));
    }

    private static void encodeInteger(Long integer, ByteArrayOutputStream out) throws IOException {
        out.write(String.format("i%de", integer).getBytes(StandardCharsets.UTF_8));
    }

    private static Object decodeBencode(ByteArrayInputStream input) throws IOException {
        input.mark(0);
        char firstChar = (char) input.read();
        return switch (findType(firstChar)) {
            case INTEGER -> parseInteger(input);
            case STRING -> {
                input.reset();
                yield parseString(input);
            }
            case LIST -> parseList(input);
            case DICTIONARY -> parseDictionary(input);
        };
    }

    private static BitTorrentType findType(char firstChar) {
        if (Character.isDigit(firstChar)) return BitTorrentType.STRING;
        return switch (firstChar) {
            case 'l' -> BitTorrentType.LIST;
            case 'i' -> BitTorrentType.INTEGER;
            case 'd' -> BitTorrentType.DICTIONARY;
            default -> throw new RuntimeException("Operation not supported");
        };
    }

    private static Long parseInteger(ByteArrayInputStream input) {
        return Long.parseLong(extractTillChar(input, 'e'));
    }

    private static String parseString(ByteArrayInputStream input) throws IOException {
        int length = Integer.parseInt(extractTillChar(input, ':'));
        byte[] strData = new byte[length];
        input.read(strData);
        return new String(strData, StandardCharsets.UTF_8);
    }

    private static List<?> parseList(ByteArrayInputStream input) throws IOException {
        List<Object> list = new ArrayList<>();
        input.mark(0);
        while ((char) input.read() != 'e') {
            input.reset();
            list.add(decodeBencode(input));
            input.mark(0);
        }
        return list;
    }

    private static String extractTillChar(ByteArrayInputStream input, char end) {
        StringBuilder sb = new StringBuilder();
        char c;
        while ((c = (char) input.read()) != end) sb.append(c);
        return sb.toString();
    }

    private static Map<?, ?> parseDictionary(ByteArrayInputStream input) throws IOException {
        Map<Object, Object> map = new LinkedHashMap<>();
        input.mark(0);
        while ((char) input.read() != 'e') {
            input.reset();
            Object key = decodeBencode(input);
            map.put(key, "pieces".equals(key) ? decodeByte(input) : decodeBencode(input));
            input.mark(0);
        }
        return map;
    }

    private static byte[] decodeByte(ByteArrayInputStream input) throws IOException {
        byte[] bytes = new byte[Integer.parseInt(extractTillChar(input, ':'))];
        input.read(bytes);
        return bytes;
    }
}
