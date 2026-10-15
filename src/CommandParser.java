import java.time.Clock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses one JSON command line, e.g. {"type":"PLAY","videoId":"v42","userId":"u1"}.
 */
public class CommandParser {

    private final Clock clock;

    public CommandParser(Clock clock) {
        this.clock = clock;
    }

    public Command parse(String line) {
        String typeName = field(line, "type");
        CommandType type;
        try {
            type = CommandType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown command type: " + typeName);
        }
        return new Command(type, field(line, "videoId"), field(line, "userId"), clock.instant());
    }

    private static String field(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return m.group(1);
    }
}
