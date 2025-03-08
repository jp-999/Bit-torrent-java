import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        System.err.println("Logs from your program will appear here!");

        String command = args[0];

        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            String decoded;
            try {
                decoded = String.valueOf(decodeBencode(bencodedValue));
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(decoded);

        } else if ("info".equals(command)) {
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
                    System.out.println("Length: " + length);  // Corrected output format
                }
            }

        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    static Object decodeBencode(String bencodedString) {
        if (bencodedString.startsWith("d")) {
            // Handle dictionaries
            return decodeDictionary(bencodedString);
        } else if (bencodedString.startsWith("l")) {
            // Handle lists
            return decodeList(bencodedString);
        } else if (bencodedString.startsWith("i")) {
            // Handle integers
            return decodeInteger(bencodedString);
        } else if (Character.isDigit(bencodedString.charAt(0))) {
            // Handle strings
            return decodeString(bencodedString);
        } else {
            throw new RuntimeException("Unsupported bencode type");
        }
    }

    private static Object decodeDictionary(String bencodedString) {
        Map<String, Object> map = new java.util.TreeMap<>();
        int index = 1;  // Skip the initial 'd'

        while (index < bencodedString.length() && bencodedString.charAt(index) != 'e') {
            Object key = decodeString(bencodedString.substring(index));
            index += getEncodedLength(bencodedString.substring(index));

            Object value = decodeBencode(bencodedString.substring(index));
            index += getEncodedLength(bencodedString.substring(index));

            map.put((String) key, value);
        }

        return map;
    }

    private static Object decodeList(String bencodedString) {
        java.util.List<Object> list = new java.util.ArrayList<>();
        int index = 1;  // Skip 'l'

        while (index < bencodedString.length() && bencodedString.charAt(index) != 'e') {
            Object element = decodeBencode(bencodedString.substring(index));
            index += getEncodedLength(bencodedString.substring(index));
            list.add(element);
        }

        return list;
    }

    private static Object decodeInteger(String bencodedString) {
        int endIndex = bencodedString.indexOf("e");
        if (endIndex == -1) throw new RuntimeException("Invalid integer format");
        return Long.parseLong(bencodedString.substring(1, endIndex));
    }

    private static Object decodeString(String bencodedString) {
        int colonIndex = bencodedString.indexOf(":");
        if (colonIndex == -1) throw new RuntimeException("Invalid string format");
        int length = Integer.parseInt(bencodedString.substring(0, colonIndex));
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
            int length = Integer.parseInt(bencodedString.substring(0, colonIndex));
            return colonIndex + 1 + length;
        }
        throw new RuntimeException("Invalid encoded format");
    }
}
