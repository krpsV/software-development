# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java-based client-server socket communication project called "Jabber" that demonstrates basic network programming concepts. The architecture consists of:

- **JabberServer.java**: TCP server that accepts connections and echoes received messages
- **JabberClient.java**: TCP client that connects to server and sends test messages
- **tmux-based development environment**: Scripts for running server and client in split panes

## Build and Run Commands

### Compile Java sources
```bash
javac -d bin src/*.java
```

### Run server (default port 8080, or specify port)
```bash
java -cp bin JabberServer [port]
```

### Run client (connects to specified port)
```bash
java -cp bin JabberClient [port]
```

### Development workflow (recommended)
```bash
./run.sh
```
This script:
- Creates a tmux session with server in left pane
- Provides client startup prompt in right pane
- Automatically compiles and manages the development environment

## Architecture Notes

- Server accepts single client connection and echoes messages until "END" command
- Client sends 10 test messages ("howdy 0" through "howdy 9") then terminates
- Both components use BufferedReader/PrintWriter for line-based communication
- Port configuration is shared between client and server via JabberServer.PORT constant
- Compiled classes are output to `bin/` directory

## Claude Code Company Management System

### Prerequisites
```bash
# Set up Claude Code alias
alias cc="claude --dangerously-skip-permissions"
```

### Basic Setup

#### 1. Create tmux pane structure
```bash
# Split into 6 panes (1 main + 5 workers)
tmux split-window -h && tmux split-window -v && tmux select-pane -t 0 && tmux split-window -v && tmux select-pane -t 2 && tmux split-window -v && tmux select-pane -t 4 && tmux split-window -v
```

#### 2. Identify pane IDs
```bash
# Check actual pane IDs (these change per session)
tmux list-panes -F "#{pane_index}: #{pane_id} #{pane_current_command} #{pane_active}"
# Example output:
# 0: %22 zsh 1  (main)
# 1: %27 zsh 0  (worker1)
# 2: %28 zsh 0  (worker2)
# 3: %25 zsh 0  (worker3)
# 4: %29 zsh 0  (worker4)
# 5: %26 zsh 0  (worker5)
```

#### 3. Launch Claude Code sessions
```bash
# Start all workers in parallel (replace with actual pane IDs)
tmux send-keys -t %27 "cc" && sleep 0.1 && tmux send-keys -t %27 Enter & \
tmux send-keys -t %28 "cc" && sleep 0.1 && tmux send-keys -t %28 Enter & \
tmux send-keys -t %25 "cc" && sleep 0.1 && tmux send-keys -t %25 Enter & \
tmux send-keys -t %29 "cc" && sleep 0.1 && tmux send-keys -t %29 Enter & \
tmux send-keys -t %26 "cc" && sleep 0.1 && tmux send-keys -t %26 Enter & \
wait
```

### Task Assignment

#### Single task assignment
```bash
tmux send-keys -t %27 "cd '/Users/hasumiyuuta/Desktop/codebase/software-development' && You are worker1. [Task description]. Report back with: tmux send-keys -t %22 '[worker1] status update' && sleep 0.1 && tmux send-keys -t %22 Enter" && sleep 0.1 && tmux send-keys -t %27 Enter
```

#### Parallel task assignment
```bash
tmux send-keys -t %27 "Task 1 content" && sleep 0.1 && tmux send-keys -t %27 Enter & \
tmux send-keys -t %28 "Task 2 content" && sleep 0.1 && tmux send-keys -t %28 Enter & \
tmux send-keys -t %25 "Task 3 content" && sleep 0.1 && tmux send-keys -t %25 Enter & \
wait
```

### Communication System

#### Worker reporting format
Workers report to main using:
```bash
tmux send-keys -t %22 '[worker#] message' && sleep 0.1 && tmux send-keys -t %22 Enter
```

Examples:
- `tmux send-keys -t %22 '[worker1] Task completed' && sleep 0.1 && tmux send-keys -t %22 Enter`
- `tmux send-keys -t %22 '[worker3] Error: details' && sleep 0.1 && tmux send-keys -t %22 Enter`

### Token Management

#### Clear individual worker
```bash
tmux send-keys -t %27 "/clear" && sleep 0.1 && tmux send-keys -t %27 Enter
```

#### Clear all workers
```bash
tmux send-keys -t %27 "/clear" && sleep 0.1 && tmux send-keys -t %27 Enter & \
tmux send-keys -t %28 "/clear" && sleep 0.1 && tmux send-keys -t %28 Enter & \
tmux send-keys -t %25 "/clear" && sleep 0.1 && tmux send-keys -t %25 Enter & \
tmux send-keys -t %29 "/clear" && sleep 0.1 && tmux send-keys -t %29 Enter & \
tmux send-keys -t %26 "/clear" && sleep 0.1 && tmux send-keys -t %26 Enter & \
wait
```

### Monitoring Commands

#### Check worker status
```bash
# View last 10 lines from specific worker
tmux capture-pane -t %27 -p | tail -10

# Check all workers
for pane in %27 %28 %25 %29 %26; do
    echo "=== $pane ==="
    tmux capture-pane -t $pane -p | tail -5
done
```

### Best Practices

1. **Clear role assignment**: Always specify worker number and task
2. **Regular /clear**: Clear workers after task completion or high token usage
3. **Structured reporting**: Use [worker#] prefix for all communications
4. **Error handling**: Include detailed error messages in reports
5. **Token monitoring**: Use `ccusage` to track consumption