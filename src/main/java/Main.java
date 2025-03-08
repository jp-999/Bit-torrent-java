import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws Exception {
        System.err.println("Logs from your program will appear here!");

        if (args.length < 2) {
            System.out.println("Invalid command or missing arguments");
            return;
        }

        String command = args[0];

        if ("info".equals(command)) {
            String filePath = args[1];
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

            Map<String, Object> torrentData = (Map<String, Object>) decodeBencode(new String(fileBytes));

            if (torrentData.containsKey("announce")) {
                String announce = (String) torrentData.get("announce");
                System.out.println("Tracker URL: " + announce);
            }

            if (torrentData.containsKey("info")) {
                Map<String, Object> infoDict = (Map<String, Object>) torrentData.get("info");

                if (infoDict.containsKey("length")) {
                    int length = ((Number) infoDict.get("length")).intValue();
                    System.out.println("Length: " + length);
                }
            }
        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    static Object decodeBencode(String bencodedString) {
        if (bencodedString.startsWith("d")) {
            return decodeDictionary(bencodedString);
        } else if (bencodedString.startsWith("l")) {
            return decodeList(bencodedString);
        } else if (bencodedString.startsWith("i")) {
            return decodeInteger(bencodedString);
        } else if (Character.isDigit(bencodedString.charAt(0))) {
            return decodeString(bencodedString);
        } else {
            throw new RuntimeException("Unsupported bencode type");
        }
    }

    private static Map<String, Object> decodeDictionary(String bencodedString) {
        Map<String, Object> map = new TreeMap<>();
        int index = 1;

        while (index < bencodedString.length() && bencodedString.charAt(index) != 'e') {
            Object key = decodeString(bencodedString.substring(index));
            int keyLength = getEncodedLength(bencodedString.substring(index));
            index += keyLength;

            if (index >= bencodedString.length()) break;

            Object value = decodeBencode(bencodedString.substring(index));
            int valueLength = getEncodedLength(bencodedString.substring(index));
            index += valueLength;

            map.put((String) key, value);
        }

        return map;
    }

    private static List<Object> decodeList(String bencodedString) {
        List<Object> list = new ArrayList<>();
        int index = 1;

        while (index < bencodedString.length() && bencodedString.charAt(index) != 'e') {
            Object element = decodeBencode(bencodedString.substring(index));
            int elementLength = getEncodedLength(bencodedString.substring(index));
            index += elementLength;
            list.add(element);
        }

        return list;
    }

    private static Long decodeInteger(String bencodedString) {
        int endIndex = bencodedString.indexOf("e");
        if (endIndex == -1) throw new RuntimeException("Invalid integer format");
        return Long.parseLong(bencodedString.substring(1, endIndex));
    }

    private static String decodeString(String bencodedString) {
        int colonIndex = bencodedString.indexOf(":");
        if (colonIndex == -1) throw new RuntimeException("Invalid string format");

        int length;
        try {
            length = Integer.parseInt(bencodedString.substring(0, colonIndex));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid length in bencoded string");
        }

        if (colonIndex + 1 + length > bencodedString.length()) {
            throw new RuntimeException("String length out of bounds");
        }

        return bencodedString.substring(colonIndex + 1, colonIndex + 1 + length);
    }

    private static int getEncodedLength(String bencodedString) {
        if (bencodedString.startsWith("d") || bencodedString.startsWith("l")) {
            int depth = 0;
            for (int i = 0; i < bencodedString.length(); i++) {
                if (bencodedString.charAt(i) == 'd' || bencodedString.charAt(i) == 'l') depth++;
                if (bencodedString.charAt(i) == 'e') depth--;
                if (depth == 0) return i + 1;
            }
        } else if (bencodedString.startsWith("i")) {
            return bencodedString.indexOf("e") + 1;
        } else if (Character.isDigit(bencodedString.charAt(0))) {
            int colonIndex = bencodedString.indexOf(":");
            if (colonIndex == -1) throw new RuntimeException("Invalid string format");

            int length = Integer.parseInt(bencodedString.substring(0, colonIndex));
            if (colonIndex + 1 + length > bencodedString.length()) {
                throw new RuntimeException("Encoded string length out of bounds");
            }

            return colonIndex + 1 + length;
        }

        throw new RuntimeException("Invalid encoded format");
    }
}
