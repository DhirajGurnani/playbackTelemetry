import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tracks the current state (last accepted command) of each user.
 * Unpausing must return to the state the user paused from.
 */
public class PlaybackStateMachine {

    private static final Map<CommandType, Set<CommandType>> ALLOWED = Map.of(
            CommandType.PLAY_REQUESTED, EnumSet.of(CommandType.PLAY, CommandType.PAUSE, CommandType.STOP),
            CommandType.PLAY, EnumSet.of(CommandType.PAUSE, CommandType.STOP),
            CommandType.STOP, EnumSet.of(CommandType.PLAY_REQUESTED)
    );

    private final Map<String, CommandType> stateByUser = new HashMap<>();
    private final Map<String, CommandType> pausedFromByUser = new HashMap<>();

    public void apply(Command command) {
        String user = command.userId();
        CommandType current = stateByUser.get(user);
        CommandType next = command.type();

        boolean allowed;
        if (current == null) {
            allowed = next == CommandType.PLAY_REQUESTED;
        } else if (current == CommandType.PAUSE) {
            allowed = next == CommandType.STOP || next == pausedFromByUser.get(user);
        } else {
            allowed = ALLOWED.get(current).contains(next);
        }
        if (!allowed) {
            throw new IllegalStateException("cannot go from " + (current == null ? "START" : current) + " to " + next);
        }

        if (next == CommandType.PAUSE) {
            pausedFromByUser.put(user, current);
        }
        stateByUser.put(user, next);
    }

    public CommandType stateOf(String userId) {
        return stateByUser.get(userId);
    }
}
