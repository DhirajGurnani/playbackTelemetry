import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Clock;

public class Main {

    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        new CommandLineListener(in, System.out, new CommandParser(Clock.systemUTC()), new PlaybackStateMachine(), new Telemetry()).run();
    }
}
