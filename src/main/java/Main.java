import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        System.err.println("Logs from your program will appear here!");

        String command = args[0];
        if ("info".equals(command)) {
            String filePath = args[1];
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

            Object decoded;
            try {
                decoded = decodeBencode(fileBytes, new int[]{0});
                if (!(decoded instanceof Map)) {
                    throw new RuntimeException("Invalid torrent file format.");
                }
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }

            Map<?, ?> torrentData = (Map<?, ?>) decoded;
            String announce = (String) torrentData.get("announce");

            Map<?, ?> info = (Map<?, ?>) torrentData.get("info");
            if (info == null) {
                System.out.println("Invalid torrent: Missing 'info' dictionary.");
                return;
            }
            Long length = (Long) info.get("length");

            // Expected output format
            System.out.println("Tracker URL: " + announce);
            System.out.println("File length: " + length);
        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    static Object decodeBencode(byte[] bencodedBytes, int[] index) {
        if (index[0] >= bencodedBytes.length) {
            throw new RuntimeException("Unexpected end of data");
        }

        char firstChar = (char) bencodedBytes[index[0]];

        if (firstChar == 'd') {
            index[0]++;
            Map<String, Object> map = new TreeMap<>();
            while (index[0] < bencodedBytes.length && (char) bencodedBytes[index[0]] != 'e') {
                Object key = decodeBencode(bencodedBytes, index);
                if (!(key instanceof String)) {
                    throw new RuntimeException("Dictionary keys must be strings.");
                }
                Object value = decodeBencode(bencodedBytes, index);
                map.put((String) key, value);
            }
            if (index[0] >= bencodedBytes.length) {
                throw new RuntimeException("Unexpected end of dictionary");
            }
            index[0]++;
            return map;
        } else if (firstChar == 'l') {
            index[0]++;
            List<Object> list = new ArrayList<>();
            while (index[0] < bencodedBytes.length && (char) bencodedBytes[index[0]] != 'e') {
                list.add(decodeBencode(bencodedBytes, index));
            }
            if (index[0] >= bencodedBytes.length) {
                throw new RuntimeException("Unexpected end of list");
            }
            index[0]++;
            return list;
        } else if (Character.isDigit(firstChar)) {
            int colonIndex = findColonIndex(bencodedBytes, index[0]);
            int length = parseInt(bencodedBytes, index[0], colonIndex);
            index[0] = colonIndex + 1;

            if (index[0] + length > bencodedBytes.length) {
                throw new RuntimeException("String length exceeds available data");
            }

            String value = new String(bencodedBytes, index[0], length);
            index[0] += length;
            return value;
        } else if (firstChar == 'i') {
            int endIndex = findEndIndex(bencodedBytes, index[0]);
            long value = parseInt(bencodedBytes, index[0] + 1, endIndex);
            index[0] = endIndex + 1;
            return value;
        } else {
            throw new RuntimeException("Unsupported bencode type at index " + index[0]);
        }
    }

    // Helper method to find the colon index for strings
    private static int findColonIndex(byte[] data, int start) {
        for (int i = start; i < data.length; i++) {
            if (data[i] == ':') {
                return i;
            }
        }
        throw new RuntimeException("Invalid bencoded string format: missing colon");
    }

    // Helper method to find the end index for integers
    private static int findEndIndex(byte[] data, int start) {
        for (int i = start; i < data.length; i++) {
            if (data[i] == 'e') {
                return i;
            }
        }
        throw new RuntimeException("Invalid bencoded integer format: missing 'e'");
    }

    // Helper method to safely parse an integer from bytes
    private static int parseInt(byte[] data, int start, int end) {
        return Integer.parseInt(new String(data, start, end - start));
    }
}
