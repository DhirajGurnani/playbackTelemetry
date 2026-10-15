import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandLineListenerTest {

    /** Each parsed command is stamped one second after the previous one. */
    private static class TickingClock extends Clock {
        private Instant now = Instant.EPOCH;

        @Override
        public Instant instant() {
            now = now.plusSeconds(1);
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private static String cmd(String type) {
        return "{\"type\":\"" + type + "\",\"videoId\":\"v1\",\"userId\":\"u1\"}";
    }

    private static String[] run(String... lines) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedReader in = new BufferedReader(new StringReader(String.join("\n", lines)));
        new CommandLineListener(in, new PrintStream(bytes), new CommandParser(new TickingClock()),
                new PlaybackStateMachine(), new Telemetry()).run();
        String[] output = bytes.toString().split("\\R");
        // Drop the welcome line
        return Arrays.copyOfRange(output, 1, output.length);
    }

    @Test
    void acknowledgesValidCommandsAndReportsErrors() throws Exception {
        String[] output = run(cmd("PLAY_REQUESTED"), "", "not json", cmd("PLAY"));

        assertArrayEquals(new String[]{"PLAY_REQUESTED", "ERROR: Missing required field: type", "PLAY"}, output);
    }

    @Test
    void reportsInvalidTransition() throws Exception {
        String[] output = run(cmd("PAUSE"));

        assertEquals("ERROR: cannot go from START to PAUSE", output[0]);
    }

    @Test
    void stopsAtExit() throws Exception {
        String[] output = run(cmd("PLAY_REQUESTED"), "exit", cmd("PLAY"));

        assertEquals(1, output.length);
    }

    @Test
    void metricsBeforeAnyPlayback() throws Exception {
        String[] output = run("AVG_BUFFERING_TIME", "AVG_PLAY_TIME", "PLAY_STARTS");

        assertArrayEquals(new String[]{"n/a", "n/a", "0"}, output);
    }

    @Test
    void printsMetrics() throws Exception {
        // Commands are stamped at 1s..5s: buffer 1s (1->2), play 2s (2->3, 4->5)
        String[] output = run(cmd("PLAY_REQUESTED"), cmd("PLAY"), cmd("PAUSE"), cmd("PLAY"), cmd("STOP"),
                "avg_buffering_time", "AVG_PLAY_TIME", "PLAY_STARTS");

        assertArrayEquals(new String[]{"1.00s", "2.00s", "1"}, Arrays.copyOfRange(output, 5, 8));
    }

    @Test
    void rejectedCommandsDoNotAffectMetrics() throws Exception {
        String[] output = run(cmd("PLAY"), cmd("STOP"), "PLAY_STARTS");

        assertEquals("0", output[2]);
    }
}
