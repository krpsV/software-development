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