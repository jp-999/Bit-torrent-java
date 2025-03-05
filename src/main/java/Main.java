import com.google.gson.Gson;
// import com.dampcake.bencode.Bencode; - available if you need it!
import java.util.ArrayList;
import java.util.List;

public class Main {
  private static final Gson gson = new Gson();

  public static void main(String[] args) throws Exception {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");

    String command = args[0];
    if("decode".equals(command)) {
      //  Uncomment this block to pass the first stage
       String bencodedValue = args[1];
       String decoded;
       try {
         decoded = String.valueOf(decodeBencode(bencodedValue));
       } catch(RuntimeException e) {
         System.out.println(e.getMessage());
         return;
       }
       System.out.println(decoded);

    } else {
      System.out.println("Unknown command: " + command);
    }

  }

  static Object decodeBencode(String bencodedString) {
    if (bencodedString.startsWith("l")) {
        // Handling a list: l<element1><element2>...e
        List<Object> list = new ArrayList<>();
        StringBuilder currentInput = new StringBuilder(bencodedString.substring(1)); // Remove the 'l'
        
        while (currentInput.length() > 0 && currentInput.charAt(0) != 'e') {
            // Decode the next element
            if (Character.isDigit(currentInput.charAt(0))) {
                // String element
                int colonIndex = currentInput.indexOf(":");
                int length = Integer.parseInt(currentInput.substring(0, colonIndex));
                String value = currentInput.substring(colonIndex + 1, colonIndex + 1 + length);
                list.add("\"" + value + "\"");
                
                // Remove the processed element
                currentInput.delete(0, colonIndex + 1 + length);
            } else if (currentInput.charAt(0) == 'i') {
                // Integer element
                int endIndex = currentInput.indexOf("e");
                Long value = Long.parseLong(currentInput.substring(1, endIndex));
                list.add(value);
                
                // Remove the processed element
                currentInput.delete(0, endIndex + 1);
            } else {
                throw new RuntimeException("Unsupported element type in list");
            }
        }
        
        // Remove the ending 'e'
        if (currentInput.length() > 0 && currentInput.charAt(0) == 'e') {
            currentInput.deleteCharAt(0);
        } else {
            throw new RuntimeException("Invalid list format, missing ending 'e'");
        }
        
        return list;
    } else if (Character.isDigit(bencodedString.charAt(0))) {
        // Handling a string: <length>:<string>
        int colonIndex = bencodedString.indexOf(":");
        if (colonIndex == -1) {
            throw new RuntimeException("Invalid bencoded string format");
        }
        int length = Integer.parseInt(bencodedString.substring(0, colonIndex));
        if (colonIndex + 1 + length > bencodedString.length()) {
            throw new RuntimeException("Invalid string length");
        }
        return "\"" + bencodedString.substring(colonIndex + 1, colonIndex + 1 + length) + "\"";
    } else if (bencodedString.charAt(0) == 'i') {
        // Handling an integer: i<number>e
        int endIndex = bencodedString.indexOf("e");
        if (endIndex == -1) {
            throw new RuntimeException("Invalid bencoded integer format");
        }
        return Long.parseLong(bencodedString.substring(1, endIndex));
    } else {
        throw new RuntimeException("Unsupported bencode type");
    }
  }

}