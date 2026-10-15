import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelemetryTest {

    private final Telemetry telemetry = new Telemetry();

    private void at(int second, String user, CommandType type) {
        telemetry.record(new Command(type, "v1", user, Instant.EPOCH.plusSeconds(second)));
    }

    @Test
    void noDataYet() {
        assertEquals(0, telemetry.playStarts());
        assertEquals(Optional.empty(), telemetry.avgBufferingTime());
        assertEquals(Optional.empty(), telemetry.avgPlayTime());
    }

    @Test
    void bufferingIsPlayRequestedToFirstPlay() {
        at(0, "u1", CommandType.PLAY_REQUESTED);
        at(3, "u1", CommandType.PLAY);

        assertEquals(1, telemetry.playStarts());
        assertEquals(Optional.of(Duration.ofSeconds(3)), telemetry.avgBufferingTime());
    }

    @Test
    void bufferingExcludesPausedTime() {
        at(0, "u1", CommandType.PLAY_REQUESTED);
        at(2, "u1", CommandType.PAUSE);
        at(10, "u1", CommandType.PLAY_REQUESTED);
        at(11, "u1", CommandType.PLAY);

        assertEquals(Optional.of(Duration.ofSeconds(3)), telemetry.avgBufferingTime());
    }

    @Test
    void playTimeIsPerSessionExcludingPauses() {
        at(0, "u1", CommandType.PLAY_REQUESTED);
        at(2, "u1", CommandType.PLAY);
        at(10, "u1", CommandType.PAUSE);
        at(15, "u1", CommandType.PLAY);
        at(20, "u1", CommandType.STOP);

        assertEquals(Optional.of(Duration.ofSeconds(13)), telemetry.avgPlayTime());
    }

    @Test
    void playTimeOnlyCountsFinishedSessions() {
        at(0, "u1", CommandType.PLAY_REQUESTED);
        at(1, "u1", CommandType.PLAY);

        assertEquals(Optional.empty(), telemetry.avgPlayTime());
    }

    @Test
    void resumeIsNotAPlayStart() {
        at(0, "u1", CommandType.PLAY_REQUESTED);
        at(1, "u1", CommandType.PLAY);
        at(2, "u1", CommandType.PAUSE);
        at(3, "u1", CommandType.PLAY);

        assertEquals(1, telemetry.playStarts());
    }

    @Test
    void sessionStoppedBeforePlayIsNotCounted() {
        at(0, "u1", CommandType.PLAY_REQUESTED);
        at(5, "u1", CommandType.STOP);

        assertEquals(0, telemetry.playStarts());
        assertEquals(Optional.empty(), telemetry.avgBufferingTime());
        assertEquals(Optional.empty(), telemetry.avgPlayTime());
    }

    @Test
    void averagesAcrossSessionsAndUsers() {
        // u1, session 1: buffer 2s, play 10s
        at(0, "u1", CommandType.PLAY_REQUESTED);
        at(2, "u1", CommandType.PLAY);
        at(12, "u1", CommandType.STOP);
        // u1, session 2: buffer 4s, play 20s
        at(20, "u1", CommandType.PLAY_REQUESTED);
        at(24, "u1", CommandType.PLAY);
        at(44, "u1", CommandType.STOP);
        // u2, interleaved: buffer 6s, play 30s
        at(5, "u2", CommandType.PLAY_REQUESTED);
        at(11, "u2", CommandType.PLAY);
        at(41, "u2", CommandType.STOP);

        assertEquals(3, telemetry.playStarts());
        assertEquals(Optional.of(Duration.ofSeconds(4)), telemetry.avgBufferingTime());
        assertEquals(Optional.of(Duration.ofSeconds(20)), telemetry.avgPlayTime());
    }
}
