import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BencodeUtils {
    public static void encodeBencode(Object object, ByteArrayOutputStream out) throws IOException {
        switch (object) {
            case Map<?, ?> map -> encodeMap(map, out);
            case List<?> list -> encodeList(list, out);
            case String string -> encodeString(string, out);
            case Long number -> encodeInteger(number, out);
            default -> throw new RuntimeException("Unexpected Object Type: " + object);
        }
    }

    private static void encodeMap(Map<?, ?> map, ByteArrayOutputStream out) throws IOException {
        out.write('d');
        for (var entry : map.entrySet()) {
            encodeBencode(entry.getKey(), out);
            if ("pieces".equals(entry.getKey())) {
                encodeBytes((byte[]) entry.getValue(), out);
            } else {
                encodeBencode(entry.getValue(), out);
            }
        }
        out.write('e');
    }

    private static void encodeBytes(byte[] value, ByteArrayOutputStream out) throws IOException {
        out.write(Integer.toString(value.length).getBytes(StandardCharsets.UTF_8));
        out.write(':');
        out.write(value);
    }

    private static void encodeList(List<?> list, ByteArrayOutputStream out) throws IOException {
        out.write('l');
        for (var object : list) {
            encodeBencode(object, out);
        }
        out.write('e');
    }

    private static void encodeString(String string, ByteArrayOutputStream out) throws IOException {
        out.write(String.format("%s:%s", string.length(), string).getBytes(StandardCharsets.UTF_8));
    }

    private static void encodeInteger(Long integer, ByteArrayOutputStream out) throws IOException {
        out.write(String.format("i%de", integer).getBytes(StandardCharsets.UTF_8));
    }

    public static Object decodeBencode(ByteArrayInputStream input) throws IOException {
        input.mark(0);
        char firstChar = (char) input.read();
        return switch (findType(firstChar)) {
            case INTEGER -> parseInteger(input);
            case STRING -> {
                input.reset();
                yield parseString(input);
            }
            case LIST -> parseList(input);
            case DICTIONARY -> parseDictionary(input);
        };
    }

    private static BitTorrentType findType(char firstChar) {
        if (Character.isDigit(firstChar)) return BitTorrentType.STRING;
        return switch (firstChar) {
            case 'l' -> BitTorrentType.LIST;
            case 'i' -> BitTorrentType.INTEGER;
            case 'd' -> BitTorrentType.DICTIONARY;
            default -> throw new RuntimeException("Operation not supported");
        };
    }

    private static Long parseInteger(ByteArrayInputStream input) {
        return Long.parseLong(extractTillChar(input, 'e'));
    }

    private static String parseString(ByteArrayInputStream input) throws IOException {
        int length = Integer.parseInt(extractTillChar(input, ':'));
        byte[] strData = new byte[length];
        input.read(strData);
        return new String(strData, StandardCharsets.UTF_8);
    }

    private static List<?> parseList(ByteArrayInputStream input) throws IOException {
        List<Object> list = new ArrayList<>();
        input.mark(0);
        while ((char) input.read() != 'e') {
            input.reset();
            list.add(decodeBencode(input));
            input.mark(0);
        }
        return list;
    }

    private static String extractTillChar(ByteArrayInputStream input, char end) {
        StringBuilder sb = new StringBuilder();
        char c;
        while ((c = (char) input.read()) != end) sb.append(c);
        return sb.toString();
    }

    private static Map<?, ?> parseDictionary(ByteArrayInputStream input) throws IOException {
        Map<Object, Object> map = new LinkedHashMap<>();
        input.mark(0);
        while ((char) input.read() != 'e') {
            input.reset();
            Object key = decodeBencode(input);
            map.put(key, "pieces".equals(key) ? decodeByte(input) : decodeBencode(input));
            input.mark(0);
        }
        return map;
    }

    private static byte[] decodeByte(ByteArrayInputStream input) throws IOException {
        byte[] bytes = new byte[Integer.parseInt(extractTillChar(input, ':'))];
        input.read(bytes);
        return bytes;
    }
} 