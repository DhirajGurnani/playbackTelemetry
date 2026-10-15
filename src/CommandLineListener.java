import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Reads JSON playback commands from the CLI, one per line, until EXIT or end of input.
 * Metric commands (AVG_BUFFERING_TIME, AVG_PLAY_TIME, PLAY_STARTS) are typed as plain words.
 */
public class CommandLineListener {

    private final BufferedReader in;
    private final PrintStream out;
    private final CommandParser parser;
    private final PlaybackStateMachine stateMachine;
    private final Telemetry telemetry;

    public CommandLineListener(BufferedReader in, PrintStream out, CommandParser parser,
                               PlaybackStateMachine stateMachine, Telemetry telemetry) {
        this.in = in;
        this.out = out;
        this.parser = parser;
        this.stateMachine = stateMachine;
        this.telemetry = telemetry;
    }

    public void run() throws IOException {
        out.println("Enter commands as JSON, e.g. {\"type\":\"PLAY\",\"videoId\":\"v42\",\"userId\":\"u1\"}. Type EXIT to quit.");
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            switch (line.toUpperCase(Locale.ROOT)) {
                case "EXIT" -> {
                    return;
                }
                case "AVG_BUFFERING_TIME" -> out.println(format(telemetry.avgBufferingTime()));
                case "AVG_PLAY_TIME" -> out.println(format(telemetry.avgPlayTime()));
                case "PLAY_STARTS" -> out.println(telemetry.playStarts());
                default -> handleCommand(line);
            }
        }
    }

    private void handleCommand(String line) {
        try {
            Command command = parser.parse(line);
            stateMachine.apply(command);
            telemetry.record(command);
            out.println(command.type());
        } catch (IllegalArgumentException | IllegalStateException e) {
            out.println("ERROR: " + e.getMessage());
        }
    }

    private static String format(Optional<Duration> duration) {
        return duration.map(d -> String.format(Locale.ROOT, "%.2fs", d.toMillis() / 1000.0)).orElse("n/a");
    }
}
