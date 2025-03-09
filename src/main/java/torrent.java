import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
public class Torrent {
  public String announce;
  public long length;
  public byte[] infoHash;
  public Torrent(byte[] bytes) throws NoSuchAlgorithmException {
    Bencode bencode = new Bencode(false);
    Bencode bencode2 = new Bencode(true);
    Map<String, Object> root = bencode.decode(bytes, Type.DICTIONARY);
    Map<String, Object> info = (Map<String, Object>)root.get("info");
    announce = (String)root.get("announce");
    length = (long)info.get("length");
    MessageDigest digest2 = MessageDigest.getInstance("SHA-1");
    infoHash = digest2.digest(bencode2.encode(
        (Map<String, Object>)bencode2.decode(bytes, Type.DICTIONARY)
            .get("info")));
  }
}