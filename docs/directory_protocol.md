# Directory Communication Protocol Specification (UDP)

Communication between a NanoFiles peer (`DirectoryConnector`) and the central Directory Server (`NFDirectoryServer`) uses a custom **text-based protocol** encapsulated in UDP datagrams (default port `6868`, maximum payload `65507` bytes).

---

## 1. Framing, Syntax & Reliability

### 1.1. Wire Format
Every message consists of one or more `field:value` lines separated by `\n` and terminated by an additional blank line (`\n\n`):
```text
operation:<operation_code>\n
<field_1>:<value_1>\n
...
<field_k>:<value_k>\n
\n
```

### 1.2. List & Composite Delimiters
- **Field-Value Delimiter**: `:` (`DELIMITER`)
- **List Element Delimiter**: `,` (`LIST_DELIMITER`), e.g. `elem1,elem2,elem3`
- **Composite Sub-Element Delimiter**: `&` (`ENCODED_DELIMITER`), used to encode tuples or `IP:port` pairs without colliding with the primary `:` delimiter, e.g. `<sub1>&<sub2>,<sub1>&<sub2>`.

### 1.3. Stop-and-Wait ARQ over UDP
Because UDP provides no delivery guarantees, `DirectoryConnector.sendAndReceiveDatagrams()` implements a **Stop-and-Wait Automatic Repeat Request (ARQ)** mechanism:
- **Socket Timeout**: `TIMEOUT = 1000` ms (`socket.setSoTimeout(1000)`).
- **Retransmission Limit**: `MAX_NUMBER_OF_ATTEMPTS = 5`.
- **Simulated Link Loss**: The Directory server supports `-loss <p>` ($p \in [0.0, 1.0]$) to probabilistically drop incoming datagrams and validate client retransmission behavior.

---

## 2. Message Types Catalog

For every request operation initiated by the client, the server responds with either a positive confirmation (`*OK`) or a rejection (`*Fail`). All requests after `login` require a valid `sessionkey`.

### 2.1. Session Management (`login` / `logout`)

| # | Message | Direction | Wire Format | Description |
| :- | :--- | :--- | :--- | :--- |
| 1 | `login` | Client $\to$ Dir | `operation:login\nnickname:<nick>\n\n` | Requests registration under `<nick>`. |
| 2 | `loginOk` | Dir $\to$ Client | `operation:loginOk\nsessionkey:<key>\n\n` | Confirms login and assigns a unique `sessionKey` $\in [0, 999]$. |
| 3 | `loginFail` | Dir $\to$ Client | `operation:loginFail\n\n` | Rejects login (nickname is already in use). |
| 4 | `logout` | Client $\to$ Dir | `operation:logout\nsessionkey:<key>\n\n` | Requests session termination. |
| 5 | `logoutOK` | Dir $\to$ Client | `operation:logoutOK\n\n` | Confirms session removal. |
| 6 | `logoutFail` | Dir $\to$ Client | `operation:logoutFail\n\n` | Rejects logout (invalid `sessionKey`). |

### 2.2. User Discovery (`getuserlist`)

| # | Message | Direction | Wire Format | Description |
| :- | :--- | :--- | :--- | :--- |
| 7 | `getuserlist` | Client $\to$ Dir | `operation:getuserlist\nsessionkey:<key>\n\n` | Requests all online users and their server status. |
| 8 | `userlistOK` | Dir $\to$ Client | `operation:userlistOK\nuserlist:<nick1>&<bool1>,...,<nickN>&<boolN>\n\n` | Returns registered nicknames paired with `true`/`false` indicating whether each peer is actively serving files. |
| 9 | `userlistFail` | Dir $\to$ Client | `operation:userlistFail\n\n` | Rejects request (unauthenticated session). |

### 2.3. File Server Registration (`registerServer` / `unregisterServer`)

| # | Message | Direction | Wire Format | Description |
| :- | :--- | :--- | :--- | :--- |
| 10 | `registerServer` | Client $\to$ Dir | `operation:registerServer\nsessionkey:<key>\nserverport:<port>\n\n` | Registers the peer as an active TCP file server listening on `<port>`. |
| 11 | `registerServerOK` | Dir $\to$ Client | `operation:registerServerOK\n\n` | Confirms file server registration. |
| 12 | `registerServerFail` | Dir $\to$ Client | `operation:registerServerFail\n\n` | Rejects registration (invalid session or port $< 0$). |
| 13 | `unregisterServer` | Client $\to$ Dir | `operation:unregisterServer\nsessionkey:<key>\n\n` | Unregisters the peer's file server and purges its published files from the directory index. |
| 14 | `unregisterServerOK` | Dir $\to$ Client | `operation:unregisterServerOK\n\n` | Confirms server removal. |
| 15 | `unregisterServerFail` | Dir $\to$ Client | `operation:unregisterServerFail\n\n` | Rejects unregistration (peer was not serving). |

### 2.4. Peer Address Resolution (`getServerAddress`)

| # | Message | Direction | Wire Format | Description |
| :- | :--- | :--- | :--- | :--- |
| 16 | `getServerAddress` | Client $\to$ Dir | `operation:getServerAddress\nsessionkey:<key>\nnickname:<nick>\n\n` | Queries the TCP socket address of peer `<nick>`. |
| 17 | `serverAddressOK` | Dir $\to$ Client | `operation:serverAddressOK\nnickname:<nick>\naddress:<ip>&<port>\n\n` | Returns the peer server's IPv4 address and TCP port separated by `&`. |
| 18 | `serverAddressFail` | Dir $\to$ Client | `operation:serverAddressFail\n\n` | Target nickname does not exist or is not serving files. |

### 2.5. File Publication & Indexing (`publish` / `filelist` / `search`)

| # | Message | Direction | Wire Format | Description |
| :- | :--- | :--- | :--- | :--- |
| 19 | `publish` | Client $\to$ Dir | `operation:publish\nsessionkey:<key>\nfiles:<hash1>&<name1>&<size1>,...\n\n` | Publishes metadata of all files in the peer's local shared folder. |
| 20 | `publishOK` | Dir $\to$ Client | `operation:publishOK\n\n` | Confirms file list indexing. |
| 21 | `publishFail` | Dir $\to$ Client | `operation:publishFail\n\n` | Rejects publication (peer is not registered as a server). |
| 22 | `filelist` | Client $\to$ Dir | `operation:filelist\nsessionkey:<key>\n\n` | Requests the global catalog of published files across the network. |
| 23 | `filelistOK` | Dir $\to$ Client | `operation:filelistOK\nfiles:<hash1>&<name1>&<size1>,...\n\n` | Returns all published files (`hash`, `name`, `size`). |
| 24 | `filelistFail` | Dir $\to$ Client | `operation:filelistFail\n\n` | Rejects request. |
| 25 | `search` | Client $\to$ Dir | `operation:search\nsessionkey:<key>\nfilehash:<substr>\n\n` | Queries which peers are serving a file matching hash substring `<substr>`. |
| 26 | `searchOK` | Dir $\to$ Client | `operation:searchOK\nnicks:<nick1>,<nick2>,...\n\n` | Returns comma-separated list of nicknames serving the matched file. |
| 27 | `searchFail` | Dir $\to$ Client | `operation:searchFail\nerrormessage:<msg>\n\n` | No published file matches `<substr>`. |

## Collaborators

- **Marta Tomás** ([@MartaTR13](https://github.com/MartaTR13))
- **Pedro Villa** ([@pedro-villa](https://github.com/pedro-villa))
