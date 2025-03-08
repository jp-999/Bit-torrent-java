import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        System.err.println("Logs from your program will appear here!");

        String command = args[0];
        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            Object decoded;
            try {
                decoded = decodeBencode(bencodedValue, new int[]{0});
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(gson.toJson(decoded));
        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    static Object decodeBencode(String bencodedString, int[] index) {
        char firstChar = bencodedString.charAt(index[0]);

        if (firstChar == 'l') {
            // Handling a list: l<element1><element2>...e
            index[0]++; // Skip 'l'
            List<Object> list = new ArrayList<>();
            while (bencodedString.charAt(index[0]) != 'e') {
                list.add(decodeBencode(bencodedString, index));
            }
            index[0]++; // Skip 'e'
            return list;
        } else if (Character.isDigit(firstChar)) {
            // Handling a string: <length>:<string>
            int colonIndex = bencodedString.indexOf(":", index[0]);
            int length = Integer.parseInt(bencodedString.substring(index[0], colonIndex));
            index[0] = colonIndex + 1;
            String value = bencodedString.substring(index[0], index[0] + length);
            index[0] += length;
            return value;
        } else if (firstChar == 'i') {
            // Handling an integer: i<number>e
            int endIndex = bencodedString.indexOf("e", index[0]);
            long value = Long.parseLong(bencodedString.substring(index[0] + 1, endIndex));
            index[0] = endIndex + 1;
            return value;
        } else {
            throw new RuntimeException("Unsupported bencode type");
        }
    }
}
