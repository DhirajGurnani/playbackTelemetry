import java.time.Instant;

public record Command(CommandType type, String videoId, String userId, Instant timestamp) {
}
