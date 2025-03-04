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
        List<Object> list = new ArrayList<>();
        int currentIndex = 1; // Start after 'l'
        while (currentIndex < bencodedString.length() && bencodedString.charAt(currentIndex) != 'e') {
            Object element = decodeBencode(bencodedString.substring(currentIndex));
            list.add(element);
            // Update currentIndex to the end of the decoded element
            if (element instanceof String) {
                currentIndex += ((String) element).length() + 2; // +2 for the quotes
            } else if (element instanceof Long) {
                currentIndex += String.valueOf(element).length() + 2; // +2 for 'i' and 'e'
            }
        }
        // Move past the 'e' character
        currentIndex++;
        return list;
    } else if (Character.isDigit(bencodedString.charAt(0))) {
        int firstColonIndex = 0;
        for (int i = 0; i < bencodedString.length(); i++) {
            if (bencodedString.charAt(i) == ':') {
                firstColonIndex = i;
                break;
            }
        }
        int length = Integer.parseInt(bencodedString.substring(0, firstColonIndex));
        return "\"" + bencodedString.substring(firstColonIndex + 1, firstColonIndex + 1 + length) + "\"";
    } else if (bencodedString.startsWith("i")) {
        return Long.parseLong(bencodedString.substring(1, bencodedString.indexOf("e")));
    } else {
        throw new RuntimeException("Only strings and lists are supported at the moment");
    }
  }

}