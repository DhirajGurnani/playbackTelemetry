import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaybackStateMachineTest {

    private final PlaybackStateMachine machine = new PlaybackStateMachine();

    private void apply(String user, CommandType... types) {
        for (CommandType type : types) {
            machine.apply(new Command(type, "v1", user, Instant.EPOCH));
        }
    }

    private void assertRejected(String user, CommandType type, String message) {
        var e = assertThrows(IllegalStateException.class, () -> apply(user, type));
        assertEquals(message, e.getMessage());
    }

    @Test
    void mustStartWithPlayRequested() {
        assertRejected("u1", CommandType.PLAY, "cannot go from START to PLAY");
        assertNull(machine.stateOf("u1"));

        apply("u1", CommandType.PLAY_REQUESTED);
        assertEquals(CommandType.PLAY_REQUESTED, machine.stateOf("u1"));
    }

    @Test
    void followsTypicalFlow() {
        assertDoesNotThrow(() -> apply("u1",
                CommandType.PLAY_REQUESTED, CommandType.PLAY, CommandType.PAUSE, CommandType.PLAY,
                CommandType.STOP, CommandType.PLAY_REQUESTED));
    }

    @Test
    void unpauseReturnsToPlayWhenPausedFromPlay() {
        apply("u1", CommandType.PLAY_REQUESTED, CommandType.PLAY, CommandType.PAUSE);

        assertRejected("u1", CommandType.PLAY_REQUESTED, "cannot go from PAUSE to PLAY_REQUESTED");
        apply("u1", CommandType.PLAY);
        assertEquals(CommandType.PLAY, machine.stateOf("u1"));
    }

    @Test
    void unpauseReturnsToPlayRequestedWhenPausedFromPlayRequested() {
        apply("u1", CommandType.PLAY_REQUESTED, CommandType.PAUSE);

        assertRejected("u1", CommandType.PLAY, "cannot go from PAUSE to PLAY");
        apply("u1", CommandType.PLAY_REQUESTED);
        assertEquals(CommandType.PLAY_REQUESTED, machine.stateOf("u1"));
    }

    @Test
    void canStopFromPause() {
        apply("u1", CommandType.PLAY_REQUESTED, CommandType.PLAY, CommandType.PAUSE, CommandType.STOP);

        assertEquals(CommandType.STOP, machine.stateOf("u1"));
    }

    @Test
    void invalidTransitionKeepsState() {
        apply("u1", CommandType.PLAY_REQUESTED, CommandType.PLAY);

        assertRejected("u1", CommandType.PLAY_REQUESTED, "cannot go from PLAY to PLAY_REQUESTED");
        assertEquals(CommandType.PLAY, machine.stateOf("u1"));
    }

    @Test
    void stopOnlyAllowsNewPlayRequest() {
        apply("u1", CommandType.PLAY_REQUESTED, CommandType.STOP);

        assertRejected("u1", CommandType.PLAY, "cannot go from STOP to PLAY");
        assertRejected("u1", CommandType.PAUSE, "cannot go from STOP to PAUSE");
    }

    @Test
    void tracksEachUserSeparately() {
        apply("u1", CommandType.PLAY_REQUESTED, CommandType.PLAY);
        apply("u2", CommandType.PLAY_REQUESTED);

        assertEquals(CommandType.PLAY, machine.stateOf("u1"));
        assertEquals(CommandType.PLAY_REQUESTED, machine.stateOf("u2"));
    }
}
