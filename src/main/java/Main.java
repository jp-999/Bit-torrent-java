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
    return new BencodeDecoder(bencodedString).decode();
  }

  static class BencodeDecoder {
    private String encodedValue;
    private int current = 0;

    public BencodeDecoder(String encodedValue) {
      this.encodedValue = encodedValue;
    }

    public Object decode() {
      if (Character.isDigit(encodedValue.charAt(current))) {
        return decodeString();
      }
      if (encodedValue.charAt(current) == 'i') {
        return decodeInteger();
      }
      if (encodedValue.charAt(current) == 'l') {
        return decodeList();
      }
      throw new RuntimeException("Only strings and lists are supported at the moment");
    }

    private String decodeString() {
      int delimiterIndex = encodedValue.indexOf(':', current);
      int length = Integer.parseInt(encodedValue.substring(current, delimiterIndex));
      int start = delimiterIndex + 1;
      int end = start + length;
      current = end;
      return encodedValue.substring(start, end);
    }

    private Long decodeInteger() {
      int start = current + 1;
      int end = encodedValue.indexOf('e', start);
      current = end + 1;
      return Long.parseLong(encodedValue.substring(start, end));
    }

    private List<Object> decodeList() {
      List<Object> list = new ArrayList<>();
      current++; // Skip 'l'
      while (encodedValue.charAt(current) != 'e') {
        list.add(decode());
      }
      current++; // Skip 'e'
      return list;
    }
  }
}
