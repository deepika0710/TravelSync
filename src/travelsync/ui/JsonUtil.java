package travelsync.ui;

public class JsonUtil {

    // turns a Java string into a safe JSON string, e.g. He said "hi" -> "He said \"hi\""
    public static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                default: sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }

    // turns a list of strings into a JSON array, e.g. ["Goa","Paris"]
    public static String arrayOfStrings(java.util.List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(quote(items.get(i)));
        }
        return sb.append("]").toString();
    }
}