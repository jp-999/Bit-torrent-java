import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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

            // Using a stream-based approach
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            Map<String, Object> torrentData = (Map<String, Object>) decodeBencode(inputStream);

            // Extracting and printing the required information
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

    // Decode function that uses InputStream
    private static Object decodeBencode(ByteArrayInputStream input) throws IOException {
        int firstByte = input.read();
        if (firstByte == 'd') {
            return decodeDictionary(input);
        } else if (firstByte == 'l') {
            return decodeList(input);
        } else if (firstByte == 'i') {
            return decodeInteger(input);
        } else if (Character.isDigit(firstByte)) {
            return decodeString(input, firstByte);
        } else {
            throw new RuntimeException("Unsupported bencode type");
        }
    }

    private static Map<String, Object> decodeDictionary(ByteArrayInputStream input) throws IOException {
        Map<String, Object> map = new TreeMap<>();
        while (true) {
            int nextByte = input.read();
            if (nextByte == 'e') break; // End of dictionary

            input.reset();
            String key = (String) decodeBencode(input);
            Object value = decodeBencode(input);
            map.put(key, value);
        }
        return map;
    }

    private static List<Object> decodeList(ByteArrayInputStream input) throws IOException {
        List<Object> list = new ArrayList<>();
        while (true) {
            int nextByte = input.read();
            if (nextByte == 'e') break; // End of list

            input.reset();
            list.add(decodeBencode(input));
        }
        return list;
    }

    private static long decodeInteger(ByteArrayInputStream input) throws IOException {
        StringBuilder number = new StringBuilder();
        while (true) {
            int nextByte = input.read();
            if (nextByte == 'e') break; // End of integer
            number.append((char) nextByte);
        }
        return Long.parseLong(number.toString());
    }

    private static String decodeString(ByteArrayInputStream input, int firstByte) throws IOException {
        // Read length of the string
        StringBuilder lengthBuilder = new StringBuilder();
        lengthBuilder.append((char) firstByte); // First byte already read

        while (true) {
            int nextByte = input.read();
            if (nextByte == ':') break; // End of length specifier
            lengthBuilder.append((char) nextByte);
        }

        int length = Integer.parseInt(lengthBuilder.toString());
        byte[] strBytes = new byte[length];

        // Ensure we read exactly 'length' bytes
        int bytesRead = input.read(strBytes);
        if (bytesRead != length) {
            throw new IOException("Unexpected end of stream");
        }

        return new String(strBytes);
    }
}
