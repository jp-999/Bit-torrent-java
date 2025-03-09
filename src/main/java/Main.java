import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

class DecodeResult {
    Object value;
    int nextIndex;

    DecodeResult(Object value, int nextIndex) {
        this.value = value;
        this.nextIndex = nextIndex;
    }
}

public class BencodeDecoder {
    public static Gson gson = new Gson();

    static DecodeResult decodeBencodeHelper(String bencodedString, int startIndex) {
        if (startIndex >= bencodedString.length()) {
            throw new RuntimeException("Unexpected end of input");
        }

        char firstChar = bencodedString.charAt(startIndex);
        if (Character.isDigit(firstChar)) {
            return decodeString(bencodedString, startIndex);
        } else if (firstChar == 'i') {
            return decodeInteger(bencodedString, startIndex);
        } else if (firstChar == 'l') {
            return decodeList(bencodedString, startIndex);
        } else if (firstChar == 'd') {
            return decodeDictionary(bencodedString, startIndex);
        } else {
            throw new RuntimeException("Unsupported type");
        }
    }

    static DecodeResult decodeString(String bencodedString, int startIndex) {
        int colonIndex = bencodedString.indexOf(':', startIndex);
        if (colonIndex == -1) {
            throw new RuntimeException("Invalid string encoding");
        }
        int length = Integer.parseInt(bencodedString.substring(startIndex, colonIndex));
        String value = bencodedString.substring(colonIndex + 1, colonIndex + 1 + length);
        return new DecodeResult(value, colonIndex + 1 + length);
    }

    static DecodeResult decodeInteger(String bencodedString, int startIndex) {
        int endIndex = bencodedString.indexOf('e', startIndex);
        if (endIndex == -1) {
            throw new RuntimeException("Invalid integer encoding");
        }
        long value = Long.parseLong(bencodedString.substring(startIndex + 1, endIndex));
        return new DecodeResult(value, endIndex + 1);
    }

    static DecodeResult decodeList(String bencodedString, int startIndex) {
        List<Object> list = new ArrayList<>();
        int index = startIndex + 1;
        while (index < bencodedString.length() && bencodedString.charAt(index) != 'e') {
            DecodeResult result = decodeBencodeHelper(bencodedString, index);
            list.add(result.value);
            index = result.nextIndex;
        }
        if (index >= bencodedString.length()) {
            throw new RuntimeException("Unterminated list");
        }
        return new DecodeResult(list, index + 1);
    }

    static DecodeResult decodeDictionary(String bencodedString, int startIndex) {
        Map<Object, Object> hashMap = new HashMap<>();
        int index = startIndex + 1;
        while (index < bencodedString.length() && bencodedString.charAt(index) != 'e') {
            DecodeResult key = decodeBencodeHelper(bencodedString, index);
            index = key.nextIndex;
            DecodeResult value = decodeBencodeHelper(bencodedString, index);
            index = value.nextIndex;
            hashMap.put(key.value, value.value);
        }
        return new DecodeResult(hashMap, index + 1);
    }

    static JsonElement decodeBencode(String bencodedString) {
        return gson.toJsonTree(decodeBencodeHelper(bencodedString, 0).value);
    }

    static byte[] findInfoHash(byte[] bencodedBytes) {
        Bencode encoder = new Bencode(true);
        Map<String, Object> decodedString = encoder.decode(bencodedBytes, Type.DICTIONARY);

        if (!decodedString.containsKey("info")) {
            throw new RuntimeException("Invalid torrent file: missing 'info' dictionary.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) decodedString.get("info");

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error: " + e);
            return null;
        }
        return digest.digest(encoder.encode(info, Type.DICTIONARY));
    }

    static List<byte[]> findPiecesHash(byte[] bencodedBytes) {
        Bencode encoder = new Bencode(true);
        Map<String, Object> decodedString = encoder.decode(bencodedBytes, Type.DICTIONARY);

        if (!decodedString.containsKey("info")) {
            throw new RuntimeException("Invalid torrent file: missing 'info' dictionary.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) decodedString.get("info");

        if (!info.containsKey("pieces")) {
            throw new RuntimeException("Invalid torrent file: missing 'pieces' field.");
        }

        // ✅ Correct extraction of 'pieces' as a byte array
        String piecesString = (String) info.get("pieces");
        byte[] pieces = piecesString.getBytes(StandardCharsets.ISO_8859_1);

        List<byte[]> res = new ArrayList<>();
        for (int i = 0; i < pieces.length; i += 20) {
            res.add(Arrays.copyOfRange(pieces, i, Math.min(i + 20, pieces.length)));
        }
        return res;
    }
}
