import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Collects playback metrics from accepted commands.
 * A session runs from PLAY_REQUESTED to STOP; time spent in PAUSE counts as neither buffering nor playing.
 */
public class Telemetry {

    private static class Session {
        CommandType state;
        Instant since;
        Duration buffered = Duration.ZERO;
        Duration played = Duration.ZERO;
        boolean started;
    }

    private final Map<String, Session> sessionByUser = new HashMap<>();

    private int playStarts;
    private Duration totalBuffering = Duration.ZERO;
    private int finishedSessions;
    private Duration totalPlay = Duration.ZERO;

    public void record(Command command) {
        Session session = sessionByUser.computeIfAbsent(command.userId(), id -> new Session());
        Instant now = command.timestamp();

        if (session.state == CommandType.PLAY_REQUESTED) {
            session.buffered = session.buffered.plus(Duration.between(session.since, now));
        } else if (session.state == CommandType.PLAY) {
            session.played = session.played.plus(Duration.between(session.since, now));
        }

        switch (command.type()) {
            case PLAY_REQUESTED -> {
                if (session.state == null || session.state == CommandType.STOP) {
                    session = new Session();
                    sessionByUser.put(command.userId(), session);
                }
            }
            case PLAY -> {
                if (!session.started) {
                    session.started = true;
                    playStarts++;
                    totalBuffering = totalBuffering.plus(session.buffered);
                }
            }
            case STOP -> {
                if (session.started) {
                    finishedSessions++;
                    totalPlay = totalPlay.plus(session.played);
                }
            }
            default -> {
            }
        }

        session.state = command.type();
        session.since = now;
    }

    public int playStarts() {
        return playStarts;
    }

    public Optional<Duration> avgBufferingTime() {
        return playStarts == 0 ? Optional.empty() : Optional.of(totalBuffering.dividedBy(playStarts));
    }

    public Optional<Duration> avgPlayTime() {
        return finishedSessions == 0 ? Optional.empty() : Optional.of(totalPlay.dividedBy(finishedSessions));
    }
}
