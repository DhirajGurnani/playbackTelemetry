import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandParserTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final CommandParser parser = new CommandParser(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void parsesAllFields() {
        Command command = parser.parse("{\"type\":\"PLAY_REQUESTED\",\"videoId\":\"v42\",\"userId\":\"u1\"}");

        assertEquals(new Command(CommandType.PLAY_REQUESTED, "v42", "u1", NOW), command);
    }

    @Test
    void ignoresWhitespaceAndTypeCase() {
        Command command = parser.parse("{ \"type\" : \"pause\", \"videoId\" : \"v42\", \"userId\" : \"u1\" }");

        assertEquals(CommandType.PAUSE, command.type());
    }

    @Test
    void rejectsUnknownType() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{\"type\":\"REWIND\",\"videoId\":\"v42\",\"userId\":\"u1\"}"));

        assertEquals("Unknown command type: REWIND", e.getMessage());
    }

    @Test
    void rejectsMissingField() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{\"type\":\"PLAY\",\"videoId\":\"v42\"}"));

        assertEquals("Missing required field: userId", e.getMessage());
    }

    @Test
    void rejectsNonJson() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("PLAY"));
    }
}
