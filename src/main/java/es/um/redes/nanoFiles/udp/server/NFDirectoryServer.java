package es.um.redes.nanoFiles.udp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFDirectoryServer {
	/**
	 * Número de puerto UDP en el que escucha el directorio
	 */
	public static final int DIRECTORY_PORT = 6868;

	/**
	 * Socket de comunicación UDP con el cliente UDP (DirectoryConnector)
	 */
	private DatagramSocket socket = null;
	/**
	 * Estructura para guardar los nicks de usuarios registrados, y clave de sesión
	 * 
	 */
	private HashMap<String, Integer> nicks;
	/**
	 * Estructura para guardar las claves de sesión y sus nicks de usuario asociados
	 * 
	 */
	private HashMap<Integer, String> sessionKeys;
	/*
	 * TODO: Añadir aquí como atributos las estructuras de datos que sean necesarias
	 * para mantener en el directorio cualquier información necesaria para la
	 * funcionalidad del sistema nanoFilesP2P: ficheros publicados, servidores
	 * registrados, etc.
	 */

	/*
	 * HashMap que indica si el peer asociado a una sessionKey está sirviendo
	 * ficheros
	 */
	private HashMap<Integer, Boolean> servingFiles;
	/*
	 * HashMap que dada una sessionKey que esté sirviendo ficheros devuelve la
	 * InetSocketAddress donde está escuchando el peer servidor
	 */
	private HashMap<Integer, InetSocketAddress> serverAddresses;

	/*
	 * HashMap que dada una sessionKey que esté sirviendo ficheros devuelve la lista
	 * de FileInfo con los ficheros que está sirviendo (los FileInfo tienen el campo
	 * filePath vacío
	 */
	private HashMap<Integer, LinkedList<FileInfo>> publishedFiles;

	/*
	 * HashMap que dado el hash de un fichero devuelve una lista de nicks, los
	 * usuarios que lo están compartiendo
	 */
	private HashMap<String, LinkedList<String>> hashes;

	/**
	 * Generador de claves de sesión aleatorias (sessionKeys)
	 */
	Random random = new Random();
	/**
	 * Probabilidad de descartar un mensaje recibido en el directorio (para simular
	 * enlace no confiable y testear el código de retransmisión)
	 */
	private double messageDiscardProbability;

	/**
	 * Función que devuelve un array de FileInfo con todos los ficheros que están
	 * compartiendo los peers conectados al servidor
	 * 
	 * @return el array de FileInfo con los ficheros
	 */
	private FileInfo[] getTotalFiles() {
		int totalSize = 0;
		for (LinkedList<FileInfo> list : publishedFiles.values()) {
			totalSize += list.size();
		}

		FileInfo[] filelist = new FileInfo[totalSize];

		int index = 0;
		for (LinkedList<FileInfo> list : publishedFiles.values()) {
			for (FileInfo fileInfo : list) {
				filelist[index++] = fileInfo;
			}
		}
		return filelist;
	}

	public NFDirectoryServer(double corruptionProbability) throws SocketException {
		/*
		 * Guardar la probabilidad de pérdida de datagramas (simular enlace no
		 * confiable)
		 */
		messageDiscardProbability = corruptionProbability;
		/*
		 * TODO: (Boletín UDP) Inicializar el atributo socket: Crear un socket UDP
		 * ligado al puerto especificado por el argumento directoryPort en la máquina
		 * local,
		 */
		InetSocketAddress serverAddress = new InetSocketAddress(DIRECTORY_PORT);
		socket = new DatagramSocket(serverAddress);
		/*
		 * TODO: (Boletín UDP) Inicializar el resto de atributos de esta clase
		 * (estructuras de datos que mantiene el servidor: nicks, sessionKeys, etc.)
		 */
		nicks = new HashMap<String, Integer>();
		sessionKeys = new HashMap<Integer, String>();
		servingFiles = new HashMap<Integer, Boolean>();
		serverAddresses = new HashMap<Integer, InetSocketAddress>();
		publishedFiles = new HashMap<Integer, LinkedList<FileInfo>>();
		hashes = new HashMap<String, LinkedList<String>>();

		if (NanoFiles.testMode) {
			if (socket == null || nicks == null || sessionKeys == null) {
				System.err.println("[testMode] NFDirectoryServer: code not yet fully functional.\n"
						+ "Check that all TODOs in its constructor and 'run' methods have been correctly addressed!");
				System.exit(-1);
			}
		}
	}

	public void run() throws IOException {
		byte[] receptionBuffer = null;
		InetSocketAddress clientAddr = null;
		int dataLength = -1;
		/*
		 * TODO: (Boletín UDP) Crear un búfer para recibir datagramas y un datagrama
		 * asociado al búfer
		 */
		receptionBuffer = new byte[DirMessage.PACKET_MAX_SIZE];
		DatagramPacket requestPacket = new DatagramPacket(receptionBuffer, receptionBuffer.length);

		System.out.println("Directory starting...");

		while (true) { // Bucle principal del servidor de directorio

			// TODO: (Boletín UDP) Recibimos a través del socket un datagrama
			socket.receive(requestPacket);

			// TODO: (Boletín UDP) Establecemos dataLength con longitud del datagrama
			// recibido
			dataLength = requestPacket.getLength();

			// TODO: (Boletín UDP) Establecemos 'clientAddr' con la dirección del cliente,
			// obtenida del datagrama recibido
			clientAddr = (InetSocketAddress) requestPacket.getSocketAddress();

			if (NanoFiles.testMode) {
				if (receptionBuffer == null || clientAddr == null || dataLength < 0) {
					System.err.println("NFDirectoryServer.run: code not yet fully functional.\n"
							+ "Check that all TODOs have been correctly addressed!");
					System.exit(-1);
				}
			}
			if (NanoFiles.debugMode) {
				System.out.println(
						"Directory received datagram from " + clientAddr + " of size " + dataLength + " bytes");
			}
			// Analizamos la solicitud y la procesamos
			if (dataLength > 0) {
				String messageFromClient = null;
				/*
				 * TODO: (Boletín UDP) Construir una cadena a partir de los datos recibidos en
				 * el buffer de recepción
				 */
				messageFromClient = new String(receptionBuffer, 0, dataLength);

				// Vemos si el mensaje debe ser ignorado por la probabilidad de descarte
				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println("Directory DISCARDED datagram from " + clientAddr);
					continue;
				}

				if (NanoFiles.testMode) { // En modo de prueba (mensajes en "crudo", boletín UDP)
					System.out.println("[testMode] Contents interpreted as " + dataLength + "-byte String: \""
							+ messageFromClient + "\"");
					/*
					 * (Boletín UDP) Comprobar que se ha recibido un datagrama con la cadena "login"
					 * y en ese caso enviar como respuesta un mensaje al cliente con la cadena
					 * "loginok". Si el mensaje recibido no es "login", se informa del error y no se
					 * envía ninguna respuesta.
					 */
					if (messageFromClient.equals("login")) {
						String messageToClient = new String("loginok");
						byte[] dataToClient = messageToClient.getBytes();
						DatagramPacket packetToClient = new DatagramPacket(dataToClient, dataToClient.length);
						socket.send(packetToClient);
					} else {
						System.err.println(" Error: no login received ");
					}

				} else { // Servidor funcionando en modo producción (mensajes bien formados)

					/*
					 * Construir String partir de los datos recibidos en el datagrama. A
					 * continuación, imprimir por pantalla dicha cadena a modo de depuración.
					 * Después, usar la cadena para construir un objeto DirMessage que contenga en
					 * sus atributos los valores del mensaje (fromString).
					 */
					DirMessage message = DirMessage.fromString(messageFromClient);

					/*
					 * Llamar a buildResponseFromRequest para construir, a partir del objeto
					 * DirMessage con los valores del mensaje de petición recibido, un nuevo objeto
					 * DirMessage con el mensaje de respuesta a enviar. Los atributos del objeto
					 * DirMessage de respuesta deben haber sido establecidos con los valores
					 * adecuados para los diferentes campos del mensaje (operation, etc.)
					 */
					DirMessage responseMessage = buildResponseFromRequest(message, clientAddr);
					/*
					 * Convertir en string el objeto DirMessage con el mensaje de respuesta a
					 * enviar, extraer los bytes en que se codifica el string (getBytes), y
					 * finalmente enviarlos en un datagrama
					 */
					String str = responseMessage.toString();

					byte[] strBytes = str.getBytes();
					DatagramPacket strBytesSend = new DatagramPacket(strBytes, str.length(), clientAddr);
					socket.send(strBytesSend);
				}
			} else {
				System.err.println("Directory ignores EMPTY datagram from " + clientAddr);
			}

		}
	}

	private DirMessage buildResponseFromRequest(DirMessage msg, InetSocketAddress clientAddr) {
		/*
		 * Construir un DirMessage con la respuesta en función del tipo de mensaje
		 * recibido, leyendo/modificando según sea necesario los atributos de esta clase
		 * (el "estado" guardado en el directorio: nicks, sessionKeys, servers,
		 * files...)
		 */
		String operation = msg.getOperation();
		DirMessage response = null;

		int sessionKey = -1;

		switch (operation) {
		// LOGIN
		case DirMessageOps.OPERATION_LOGIN: {
			String username = msg.getNickname();
			/*
			 * Comprobamos si tenemos dicho usuario registrado (atributo "nicks"). Si no
			 * está, generamos su sessionKey (número aleatorio entre 0 y 1000) y añadimos el
			 * nick y su sessionKey asociada. NOTA: Puedes usar random.nextInt(10000) para
			 * generar la session key
			 */
			/*
			 * Construimos un mensaje de respuesta que indique el éxito/fracaso del login y
			 * contenga la sessionKey en caso de éxito, y lo devolvemos como resultado del
			 * método.
			 */
			/*
			 * Imprimimos por pantalla el resultado de procesar la petición recibida (éxito
			 * o fracaso) con los datos relevantes, a modo de depuración en el servidor
			 */
			sessionKey = -1;
			if (nicks.containsKey(username)) {
				response = new DirMessage(DirMessageOps.OPERATION_LOGIN_FAIL);
			} else {
				do {
					sessionKey = random.nextInt(1000);
				} while (sessionKeys.containsKey(sessionKey)); // para generarle uno que no esté usado

				nicks.put(username, sessionKey);
				sessionKeys.put(sessionKey, username);
				servingFiles.put(sessionKey, false);

				System.out.println("New user added. Username: " + username + ". SessionKey: " + sessionKey);

				response = new DirMessage(DirMessageOps.OPERATION_LOGIN_OK);
				response.setSessionKey(sessionKey);
			}
			break;
		}

		case DirMessageOps.OPERATION_LOGOUT: {
			sessionKey = msg.getSessionKey();
			if (!this.sessionKeys.containsKey(sessionKey)) { // no tengo esa sessionkey
				response = new DirMessage(DirMessageOps.OPERATION_LOGOUT_FAIL);
				System.out.println("Not in sessionKeys: " + sessionKey);
			} else {
				response = new DirMessage(DirMessageOps.OPERATION_LOGOUT_OK);
				String nick = sessionKeys.get(sessionKey);
				System.out.println("User " + nick + " logged out. Session key was " + sessionKey);

				this.nicks.remove(nick);
				this.servingFiles.remove(sessionKey);
				this.sessionKeys.remove(sessionKey);
			}
			break;
		}

		case DirMessageOps.OPERATION_GETUSERLIST: {
			sessionKey = msg.getSessionKey();
			if (!this.sessionKeys.containsKey(sessionKey)) {
				response = new DirMessage(DirMessageOps.OPERATION_USERLIST_FAIL);
			} else {
				response = new DirMessage(DirMessageOps.OPERATION_USERLIST_OK);
				response.setSessionKey(msg.getSessionKey());

				/*
				 * Para cada usuario en las claves de nicks comprueba si está sirviendo ficheros
				 * (valor de servingFiles obtenido a través de su sessionKey)
				 */
				String[] nicksArray = this.nicks.keySet().toArray(new String[0]);
				boolean[] isServingFiles = new boolean[nicksArray.length];
				for (int i = 0; i < nicksArray.length; i++) {
					int peerSessionKey = nicks.get(nicksArray[i]);
					if (servingFiles.keySet().contains(peerSessionKey)) {
						isServingFiles[i] = servingFiles.get(peerSessionKey);
					}
				}

				System.out.println("User " + sessionKeys.get(msg.getSessionKey())
						+ " requested the userlist. Sending back to user...");

				response.setUserList(nicksArray);
				response.setServerList(isServingFiles);
			}
			break;
		}

		case DirMessageOps.OPERATION_REGISTER_SERVER:
			sessionKey = msg.getSessionKey();
			int port = msg.getServerPort();
			if (!this.sessionKeys.containsKey(sessionKey) || port < 0) {
				response = new DirMessage(DirMessageOps.OPERATION_REGISTER_SERVER_FAIL);
			} else {
				servingFiles.put(sessionKey, true);

				String ipOfClientAddr = clientAddr.getAddress().getHostAddress();

				InetSocketAddress address = new InetSocketAddress(ipOfClientAddr, port);
				serverAddresses.put(sessionKey, address);

				System.out.println(
						"New server added. Username: " + sessionKeys.get(sessionKey) + ". Server address: " + address);

				response = new DirMessage(DirMessageOps.OPERATION_REGISTER_SERVER_OK);
			}
			break;

		case DirMessageOps.OPERATION_UNREGISTER_SERVER: {
			sessionKey = msg.getSessionKey();
			if (!this.sessionKeys.containsKey(sessionKey) || !servingFiles.get(sessionKey)) {
				response = new DirMessage(DirMessageOps.OPERATION_UNREGISTER_SERVER_FAIL);
			} else {
				System.out.println("User " + sessionKeys.get(sessionKey) + " with serverAddress "
						+ serverAddresses.get(sessionKey) + " is not serving files anymore");

				servingFiles.put(sessionKey, false);
				serverAddresses.remove(sessionKey);
				if (publishedFiles.get(sessionKey) != null) {
					for (FileInfo file : publishedFiles.get(sessionKey)) {
						LinkedList<String> nicksSharingFile = hashes.get(file.fileHash);
						if (nicksSharingFile.size() == 1) {
							hashes.remove(file.fileHash);
						} else {
							nicksSharingFile.remove(sessionKeys.get(sessionKey));
						}
					}
					publishedFiles.remove(sessionKey);
				}

				response = new DirMessage(DirMessageOps.OPERATION_UNREGISTER_SERVER_OK);
			}
			break;
		}

		case DirMessageOps.OPERATION_GETSERVERADDRESS: {
			sessionKey = msg.getSessionKey();
			String nickname = msg.getNickname();
			if (!this.sessionKeys.containsKey(sessionKey) || !this.nicks.containsKey(nickname)
					|| !this.servingFiles.get(nicks.get(nickname))) {
				response = new DirMessage(DirMessageOps.OPERATION_SERVERADDRESS_FAIL);
			} else {
				int targetSessionKey = nicks.get(nickname);
				response = new DirMessage(DirMessageOps.OPERATION_SERVERADDRESS_OK);
				response.setNickname(nickname);
				response.setAddress(serverAddresses.get(targetSessionKey));

				System.out.println("User " + sessionKeys.get(sessionKey) + " resquested " + nickname
						+ "'s address, which is " + serverAddresses.get(targetSessionKey) + ". Sending it to user");
			}
			break;
		}
		case DirMessageOps.OPERATION_PUBLISH: {
			sessionKey = msg.getSessionKey();
			FileInfo[] files = msg.getFiles();
			if (!this.sessionKeys.containsKey(sessionKey) || !this.servingFiles.get(sessionKey) || files == null) {
				response = new DirMessage(DirMessageOps.OPERATION_PUBLISH_FAIL);
			} else {
				LinkedList<FileInfo> fileList = new LinkedList<>();
				Collections.addAll(fileList, msg.getFiles());
				publishedFiles.put(sessionKey, fileList);

				String nick = sessionKeys.get(sessionKey);
				for (FileInfo file : fileList) {
					if (!hashes.containsKey(file.fileHash)) {
						LinkedList<String> newList = new LinkedList<>();
						newList.add(nick);
						hashes.put(file.fileHash, newList);
					} else if (!hashes.get(file.fileHash).contains(nick)) {
						hashes.get(file.fileHash).add(nick);
					}
				}

				System.out.println("User " + sessionKeys.get(sessionKey) + " published their files");
				response = new DirMessage(DirMessageOps.OPERATION_PUBLISH_OK);
			}
			break;
		}

		case DirMessageOps.OPERATION_FILELIST: {
			sessionKey = msg.getSessionKey();
			if (!sessionKeys.containsKey(sessionKey) || publishedFiles == null) {
				response = new DirMessage(DirMessageOps.OPERATION_FILELIST_FAILED);
			} else {
				FileInfo[] filelist = getTotalFiles();

				System.out.println("User " + sessionKeys.get(sessionKey) + " resquested the filelist. Sending to user");
				response = new DirMessage(DirMessageOps.OPERATION_FILELIST_OK);
				response.setFiles(filelist);
			}
			break;
		}
		case DirMessageOps.OPERATION_SEARCH: {
			sessionKey = msg.getSessionKey();
			String hashSubstr = msg.getFileHash();

			if (!sessionKeys.containsKey(sessionKey) || publishedFiles == null || hashes == null) {
				response = new DirMessage(DirMessageOps.OPERATION_SEARCH_FAILED);
			} else {

				FileInfo[] matchingFiles = FileInfo.lookupHashSubstring(getTotalFiles(), hashSubstr);
				if (matchingFiles.length == 0 || !hashes.containsKey(matchingFiles[0].fileHash)) {
					response = new DirMessage(DirMessageOps.OPERATION_SEARCH_FAILED);
					response.setErrorMessage("No matching files for hash provided");

				} else {
					String hash = matchingFiles[0].fileHash;
					LinkedList<String> nicksList = hashes.get(hash);
					String[] servingNicks = new String[nicksList.size()];
					int index = 0;
					for (String nick : nicksList) {
						servingNicks[index++] = nick;
					}

					System.out.println("User " + sessionKeys.get(sessionKey)
							+ " resquested nicks serving file with hash " + hash + ". Sending list to user");
					response = new DirMessage(DirMessageOps.OPERATION_SEARCH_OK);
					response.setFileHash(hash);
					response.setNicks(servingNicks);
				}
			}
			break;
		}

		default:
			System.out.println("Unexpected message operation: \"" + operation + "\"");
		}
		return response;

	}

}
