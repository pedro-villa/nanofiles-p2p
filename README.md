# NanoFiles — Hybrid P2P File Sharing Network

[![CI](https://github.com/pedro-villa/nanofiles-p2p/actions/workflows/ci.yml/badge.svg)](https://github.com/pedro-villa/nanofiles-p2p/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A **hybrid Peer-to-Peer (P2P) file-sharing system** written in Java, combining a **centralized UDP directory service** (with application-level Stop-and-Wait ARQ retransmission) and **decentralized, multithreaded TCP peer-to-peer file transfers** with SHA-1 cryptographic integrity verification.

---

## Key Features

- **Hybrid P2P Topology**:
  - **Centralized Discovery (UDP : `6868`)**: Lightweight text-based `field:value` protocol (`DirMessage`) for session management, server registration, file indexing, and hash/nickname resolution.
  - **Direct Peer Transfer (TCP)**: Binary opcode-framed protocol (`PeerMessage`) for direct peer-to-peer file streaming without routing payloads through the central server.
- **Reliable UDP Transport (Stop-and-Wait ARQ)**:
  - Socket timeout (`1000 ms`) and automatic retransmission up to `5` attempts in `DirectoryConnector` to handle packet loss.
  - Built-in probabilistic datagram loss simulator (`-loss <probability>`) in `Directory` to test protocol resilience under degraded network conditions.
- **Multithreaded Background File Server**:
  - `NFServer` binds to an **ephemeral OS port** (`port 0`), allowing multiple peers to operate simultaneously on the same host without port collisions.
  - Spawns dedicated worker threads (`NFServerThread` / `NFServerComm`) per incoming client connection so the interactive CLI shell remains non-blocking.
- **Protocol Finite State Automaton (FSM)**:
  - `NFController` enforces strict state transitions (`LOGGED_OUT` $\leftrightarrow$ `LOGGED_IN` $\leftrightarrow$ `SERVING`), preventing invalid protocol operations (such as publishing while not serving or logging out while an active server is bound).
- **Cryptographic Integrity & Substring Lookup**:
  - Files are indexed by their 160-bit **SHA-1** digest (`FileDigest`).
  - Supports unambiguous prefix/substring hash matching across both directory searches (`search`) and direct peer downloads (`downloadfrom` / `download`), verifying the full SHA-1 checksum upon transfer completion.

---

## Project Structure

```text
nanofiles-p2p/
├── docs/                                # Protocol specifications and FSM architecture
│   ├── architecture.md                  # Topology, state automata, and concurrency
│   ├── directory_protocol.md            # UDP text protocol (27 message types)
│   └── transfer_protocol.md             # TCP binary protocol framing & opcodes
├── nf-shared/                           # Default local shared directory
│   └── sample.txt
├── src/main/java/es/um/redes/nanoFiles/
│   ├── application/
│   │   ├── Directory.java               # Central UDP directory server entry point
│   │   └── NanoFiles.java               # Interactive P2P peer node entry point
│   ├── logic/
│   │   ├── NFController.java            # Command dispatcher & client state automaton
│   │   ├── NFControllerLogicDir.java    # High-level directory operations
│   │   └── NFControllerLogicP2P.java    # High-level P2P server & download logic
│   ├── shell/
│   │   ├── NFCommands.java              # CLI command codes and help definitions
│   │   └── NFShell.java                 # Interactive standard-input command parser
│   ├── tcp/
│   │   ├── client/NFConnector.java      # TCP file download client & SHA-1 verifier
│   │   ├── message/PeerMessage.java     # Binary stream serializer/deserializer
│   │   ├── message/PeerMessageOps.java  # Binary transfer opcodes
│   │   └── server/                      # NFServer, NFServerThread, NFServerComm
│   ├── udp/
│   │   ├── client/DirectoryConnector.java # UDP client with Stop-and-Wait ARQ
│   │   ├── message/DirMessage.java      # Text-based field:value parser/formatter
│   │   ├── message/DirMessageOps.java   # Directory operation constants
│   │   └── server/NFDirectoryServer.java # In-memory directory state & request handler
│   └── util/
│       ├── FileDatabase.java            # Local shared folder index
│       ├── FileDigest.java              # SHA-1 checksum computation
│       └── FileInfo.java                # File metadata & hash substring search
├── .gitignore
├── LICENSE                              # MIT License
├── Makefile                             # Build and run orchestration
├── pom.xml                              # Maven build configuration
└── README.md
```

---

## Prerequisites

- **Java Development Kit (JDK)** (>= 11, tested with JDK 17 LTS)
- **Maven** (optional, or **GNU Make**)

---

## Build & Usage

### 1. Build Executable JARs

Using **Maven**:
```bash
mvn clean package
```

Or using **Make** (directly via `javac` / `jar`):
```bash
make
```

This generates two runnable archives in `target/`:
- `target/Directory.jar` (or `target/Directory-server.jar` via Maven) — Central UDP Directory Server
- `target/NanoFiles.jar` — Interactive Peer Client & Server

### 2. Start the Directory Server

```bash
# Standard mode (0% simulated packet loss):
java -jar target/Directory.jar

# Simulated unreliable link (e.g., 20% packet drop probability):
java -jar target/Directory.jar -loss 0.2
```

### 3. Launch Peer Nodes

Open two separate terminals to simulate two peers (`alice` and `bob`) with distinct shared folders:

**Terminal 1 — Peer Alice (Uploader):**
```bash
java -jar target/NanoFiles.jar nf-shared
```
```text
(nanoFiles@nf-shared) login localhost alice
Login accepted. Session key: 412
(nanoFiles@nf-shared) myfiles
Name                                 Size Hash
sample.txt                            168 3a7f...
(nanoFiles@nf-shared) bgserve
Server is listening to port 51234
(nanoFiles@nf-shared) publish
```

**Terminal 2 — Peer Bob (Downloader):**
```bash
java -jar target/NanoFiles.jar nf-shared-bob
```
```text
(nanoFiles@nf-shared-bob) login localhost bob
Login accepted. Session key: 809
(nanoFiles@nf-shared-bob) userlist
List of users logged in to Directory:
user: alice, isServingFiles: true
user: bob, isServingFiles: false
(nanoFiles@nf-shared-bob) filelist
List of files published in Directory:
Name                                 Size Hash
sample.txt                            168 3a7f...
(nanoFiles@nf-shared-bob) search 3a7f
List of nicks serving hash substring 3a7f:
alice
(nanoFiles@nf-shared-bob) downloadfrom alice 3a7f downloaded_sample.txt
Connected to ... /127.0.0.1:51234
Requesting to download file with the following hash: 3a7f
File succesfully downloaded from server
The file has been downloaded succesfully from server
(nanoFiles@nf-shared-bob) logout
(nanoFiles@nf-shared-bob) quit
Bye.
```

---

## CLI Command Reference

| Command | Arguments | Valid States | Description |
| :--- | :--- | :--- | :--- |
| `login` | `<dir_host> <nick>` | `LOGGED_OUT` | Authenticate with the Directory server and acquire a `sessionKey`. |
| `logout` | — | `LOGGED_IN` | Close the current directory session. |
| `userlist` | — | `LOGGED_IN`, `SERVING` | List all connected users and whether each is serving files. |
| `filelist` | — | `LOGGED_IN`, `SERVING` | List all files published to the Directory across the network. |
| `myfiles` | — | Any | List local files in the peer's shared directory along with sizes and SHA-1 hashes. |
| `fgserve` | — | `LOGGED_IN` | Run a blocking single-threaded file server in the foreground. |
| `bgserve` | — | `LOGGED_IN` | Start the multithreaded TCP file server in the background on an ephemeral port and register it with the Directory. |
| `publish` | — | `SERVING` | Advertise local shared files to the Directory index. |
| `stopserver` | — | `SERVING` | Stop the background TCP server and unregister published files from the Directory. |
| `search` | `<hash_substr>` | `LOGGED_IN`, `SERVING` | Query nicknames of peers serving a file matching `<hash_substr>`. |
| `downloadfrom` | `<nick\|IP:port> <hash> <local_name>` | `LOGGED_IN`, `SERVING` | Download a file matching `<hash>` from a specific peer and verify its SHA-1 checksum. |
| `download` | `<hash> <local_name>` | `LOGGED_IN`, `SERVING` | Automatically query serving peers for `<hash>` and download from the first available peer. |
| `sleep` | `<seconds>` | Any | Pause shell execution for `<seconds>` (useful for scripted test pipelines). |
| `help` | — | Any | Display command syntax and descriptions. |
| `quit` | — | `LOGGED_OUT` | Exit the NanoFiles application. |

---

## Documentation

- [System Architecture, State Automata & Concurrency Model](docs/architecture.md)
- [Directory Communication Protocol Specification (UDP)](docs/directory_protocol.md)
- [Peer-to-Peer File Transfer Protocol Specification (TCP)](docs/transfer_protocol.md)

---

## Authors

- **Marta Tomás**
- **Pedro Villa** ([@pedro-villa](https://github.com/pedro-villa))
