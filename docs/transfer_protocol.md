# Peer-to-Peer File Transfer Protocol Specification (TCP)

Direct file transfers between a downloading peer (`NFConnector`) and an uploading peer (`NFServerComm`) use a compact **multi-format binary protocol** streamed over TCP via `DataInputStream` and `DataOutputStream` (`PeerMessage`).

---

## 1. Binary Opcodes (`PeerMessageOps`)

The first byte of every TCP message encodes its operation type (`opcode`):

| Opcode (1 Byte) | Constant | Mnemonic | Direction |
| :---: | :--- | :--- | :--- |
| `0x00` | `OPCODE_INVALID_CODE` | `INVALID_OPCODE` | Internal sentinel |
| `0x01` | `OPCODE_DOWNLOAD_REQUEST` | `DOWNLOAD_REQUEST` | Client Peer $\to$ Server Peer |
| `0x02` | `OPCODE_FILE_NOT_FOUND` | `FILE_NOT_FOUND` | Server Peer $\to$ Client Peer |
| `0x03` | `OPCODE_SEND_FILE` | `SEND_FILE` | Server Peer $\to$ Client Peer |
| `0x04` | `OPCODE_NON_SIGNIFICANT_HASH` | `NON_SIGNIFICANT_HASH` | Server Peer $\to$ Client Peer |

---

## 2. Binary Frame Formats

All multi-byte integer fields (`int` 4 bytes, `long` 8 bytes) are transmitted in big-endian network byte order (`DataOutputStream`).

### 2.1. `DownloadRequest` (`opcode = 1`)

Sent by the downloading client to request a file identified by its full SHA-1 hash or a unique hash substring.

```text
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Opcode (1)   |              InitialPos (4 bytes)             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|   InitialPos  |               FinalPos (4 bytes)              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|    FinalPos   |              HashLength (4 bytes)             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|   HashLength  |            FileHash (n bytes, UTF-8)         ...
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

| Field | Size | Type | Description |
| :--- | :---: | :---: | :--- |
| `Opcode` | 1 byte | `byte` | `1` (`OPCODE_DOWNLOAD_REQUEST`) |
| `InitialPos` | 4 bytes | `int` | Starting byte offset (`0` for start of file). |
| `FinalPos` | 4 bytes | `int` | Ending byte offset (`0` indicates full file transfer). |
| `HashLength` | 4 bytes | `int` | Length $n$ in bytes of the `FileHash` string. |
| `FileHash` | $n$ bytes | `byte[]` | Full 40-char SHA-1 hex string or identifying substring. |

---

### 2.2. `FileNotFound` (`opcode = 2`)

Returned by the serving peer when no local file in `NanoFiles.db` matches the requested `FileHash` substring.

```text
 0
 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+
|  Opcode (2)   |
+-+-+-+-+-+-+-+-+
```

---

### 2.3. `NonSignificantHash` (`opcode = 4`)

Returned by the serving peer when the requested `FileHash` substring is ambiguous (matches $\ge 2$ files in the server's shared folder).

```text
 0
 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+
|  Opcode (4)   |
+-+-+-+-+-+-+-+-+
```

---

### 2.4. `SendFile` (`opcode = 3`)

Returned by the serving peer when the requested `FileHash` uniquely identifies a single local file. Includes the authoritative 40-byte SHA-1 hex string alongside the raw file payload so the client can verify end-to-end cryptographic integrity.

```text
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Opcode (3)   |              HashLength (4 bytes)             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|   HashLength  |         FullFileHash (m bytes, SHA-1)        ...
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                     FileLength (8 bytes, long)                |
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        FileData (n bytes)                    ...
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

| Field | Size | Type | Description |
| :--- | :---: | :---: | :--- |
| `Opcode` | 1 byte | `byte` | `3` (`OPCODE_SEND_FILE`) |
| `HashLength` | 4 bytes | `int` | Length $m$ of the full SHA-1 hex digest (`40` bytes). |
| `FileHash` | $m$ bytes | `byte[]` | Complete 40-character hexadecimal SHA-1 hash of the file. |
| `FileLength` | 8 bytes | `long` | Exact file size $n$ in bytes. |
| `File` | $n$ bytes | `byte[]` | Raw binary content of the requested file. |

---

## 3. End-to-End Integrity Verification (`FileDigest`)

Upon receiving a `SendFile` message, `NFConnector.downloadFile()`:
1. Writes the $n$ payload bytes to the local destination file via `FileOutputStream`.
2. Recomputes the 160-bit **SHA-1** digest over the written disk file using `FileDigest.computeFileChecksumString(file.getCanonicalPath())`.
3. Compares the computed digest against the authoritative `FileHash` header received in the `SendFile` frame. Any mismatch flags corruption and rejects the transfer.
