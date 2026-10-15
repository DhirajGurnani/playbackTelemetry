# Prime Video Telemetry project

Step 1
Create a class to accept command line commands from CLI
Commands will be playback commands like
1 PLAY_REQUESTED
2 PLAY
3 PAUSE
4 STOP
etc it will have metadata with it.

Step 2
Each command will be a part of a state machine current command means current state.

Step 3
Implement states such that we can track telemetry and logistics like avg buffer time, avg play time, number play starts, etc/