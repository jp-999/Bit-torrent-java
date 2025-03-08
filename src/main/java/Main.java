import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        System.err.println("Logs from your program will appear here!");

        String command = args[0];
        if ("info".equals(command)) {  // Fix: changed from "parse_torrent" to "info"
            String filePath = args[1];
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

            Object decoded;
            try {
                decoded = decodeBencode(new String(fileBytes), new int[]{0});
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

            // Fix: Print expected format
            System.out.println("Tracker URL: " + announce);
            System.out.println("File length: " + length);
        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    static Object decodeBencode(String bencodedString, int[] index) {
        char firstChar = bencodedString.charAt(index[0]);

        if (firstChar == 'd') {
            index[0]++;
            Map<String, Object> map = new TreeMap<>();
            while (bencodedString.charAt(index[0]) != 'e') {
                Object key = decodeBencode(bencodedString, index);
                if (!(key instanceof String)) {
                    throw new RuntimeException("Dictionary keys must be strings.");
                }
                Object value = decodeBencode(bencodedString, index);
                map.put((String) key, value);
            }
            index[0]++;
            return map;
        } else if (firstChar == 'l') {
            index[0]++;
            List<Object> list = new ArrayList<>();
            while (bencodedString.charAt(index[0]) != 'e') {
                list.add(decodeBencode(bencodedString, index));
            }
            index[0]++;
            return list;
        } else if (Character.isDigit(firstChar)) {
            int colonIndex = bencodedString.indexOf(":", index[0]);
            int length = Integer.parseInt(bencodedString.substring(index[0], colonIndex));
            index[0] = colonIndex + 1;
            String value = bencodedString.substring(index[0], index[0] + length);
            index[0] += length;
            return value;
        } else if (firstChar == 'i') {
            int endIndex = bencodedString.indexOf("e", index[0]);
            long value = Long.parseLong(bencodedString.substring(index[0] + 1, endIndex));
            index[0] = endIndex + 1;
            return value;
        } else {
            throw new RuntimeException("Unsupported bencode type.");
        }
    }
}
