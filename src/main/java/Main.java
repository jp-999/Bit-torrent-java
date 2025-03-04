import com.google.gson.Gson;
// import com.dampcake.bencode.Bencode; - available if you need it!

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
    if (Character.isDigit(bencodedString.charAt(0))) {
      int firstColonIndex = 0;
      for (int i = 0; i < bencodedString.length(); i++) {
        if (bencodedString.charAt(i) == ':') {
          firstColonIndex = i;
          break;
        }
      }
      int length = Integer.parseInt(bencodedString.substring(0, firstColonIndex));
      return "\\\"" + bencodedString.substring(firstColonIndex + 1, firstColonIndex + 1 + length) + "\\\"";
    } else if (bencodedString.startsWith("i")) {
      return Long.parseLong(bencodedString.substring(1, bencodedString.indexOf("e")));
    } else {
      throw new RuntimeException("Only strings are supported at the moment");
    }
  }

}