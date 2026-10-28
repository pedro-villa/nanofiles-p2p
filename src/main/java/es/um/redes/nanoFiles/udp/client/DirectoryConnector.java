package es.um.redes.nanoFiles.udp.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	/**
	 * Puerto en el que atienden los servidores de directorio
	 */
	private static final int DIRECTORY_PORT = 6868;
	/**
	 * Tiempo máximo en milisegundos que se esperará a recibir una respuesta por el
	 * socket antes de que se deba lanzar una excepción SocketTimeoutException para
	 * recuperar el control
	 */
	private static final int TIMEOUT = 1000;
	/**
	 * Número de intentos máximos para obtener del directorio una respuesta a una
	 * solicitud enviada. Cada vez que expira el timeout sin recibir respuesta se
	 * cuenta como un intento.
	 */
	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;

	/**
	 * Valor inválido de la clave de sesión, antes de ser obtenida del directorio al
	 * loguearse
	 */
	public static final int INVALID_SESSION_KEY = -1;

	/**
	 * Socket UDP usado para la comunicación con el directorio
	 */
	private DatagramSocket socket;
	/**
	 * Dirección de socket del directorio (IP:puertoUDP)
	 */
	private InetSocketAddress directoryAddress;

	private int sessionKey = INVALID_SESSION_KEY;
	private boolean successfulResponseStatus;
	private String errorDescription;

	public DirectoryConnector(String address) throws IOException {
		/*
		 * Convertir el nombre de host 'address' a InetAddress y guardar la dirección de
		 * socket (address:DIRECTORY_PORT) del directorio en el atributo
		 * directoryAddress, para poder enviar datagramas a dicho destino.
		 */
		InetAddress adressAsInetAdress = InetAddress.getByName(address);
		directoryAddress = new InetSocketAddress(adressAsInetAdress, DIRECTORY_PORT);
		/*
		 * Crea el socket UDP en cualquier puerto para enviar datagramas al directorio
		 *
		 */
		this.socket = new DatagramSocket();
	}

	/**
	 * Método para enviar y recibir datagramas al/del directorio
	 * 
	 * @param requestData los datos a enviar al directorio (mensaje de solicitud)
	 * @return los datos recibidos del directorio (mensaje de respuesta)
	 * @throws IOException
	 */
	private byte[] sendAndReceiveDatagrams(byte[] requestData) throws IOException {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];
		byte response[] = null;
		if (directoryAddress == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP server destination address is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"directoryAddress\"");
			System.exit(-1);

		}
		if (socket == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP socket is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"socket\"");
			System.exit(-1);
		}
		/*
		 * Enviar datos en un datagrama al directorio y recibir una respuesta. El array
		 * devuelto debe contener únicamente los datos recibidos, *NO* el búfer de
		 * recepción al completo.
		 */
		/*
		 * Las excepciones que puedan lanzarse al leer/escribir en el socket deben ser
		 * capturadas y tratadas en este método. Si se produce una excepción de
		 * entrada/salida (error del que no es posible recuperarse), se debe informar y
		 * terminar el programa.
		 */
		/*
		 * NOTA: Las excepciones deben tratarse de la más concreta a la más genérica.
		 * SocketTimeoutException es más concreta que IOException.
		 * socket.setSoTimeout(TIMEOUT)
		 */
		DatagramPacket packetToServer = new DatagramPacket(requestData, requestData.length, directoryAddress);
		DatagramPacket packetFromServer = new DatagramPacket(responseData, responseData.length);

		int i = 0;
		while (i < MAX_NUMBER_OF_ATTEMPTS) {
			try {
				socket.send(packetToServer);
				socket.setSoTimeout(TIMEOUT);// el tiempo que hay para que lleguen datos
				socket.receive(packetFromServer);
			} catch (SocketTimeoutException e) {// cuando el tiempo de espera se expira
				i++;
				System.err.println("* Server timeout triggered. Trying again...");
				continue;
			} catch (IOException e) {
				System.err.println(" Could not communicate with server, Input/Output Exception");
				System.exit(1);
			}
			break;
		}

		/* Si todo va bien actualizamos response con los datos recibidos */
		if (i < MAX_NUMBER_OF_ATTEMPTS) {
			int respDataLen = packetFromServer.getLength();
			response = new byte[respDataLen];
			System.arraycopy(responseData, 0, response, 0, respDataLen);
		} else {
			System.err.println("Error: no response from server");
		}

		if (response != null && response.length == responseData.length) {
			System.err.println("Your response is as large as the datagram reception buffer!!\n"
					+ "You must extract from the buffer only the bytes that belong to the datagram!");
		}
		return response;
	}

	/**
	 * Método para probar la comunicación con el directorio mediante el envío y
	 * recepción de mensajes sin formatear ("en crudo")
	 * 
	 * @return verdadero si se ha enviado un datagrama y recibido una respuesta
	 */
	public boolean testSendAndReceive() {
		/*
		 * Probar el correcto funcionamiento de sendAndReceiveDatagrams. Se debe enviar
		 * un datagrama con la cadena "login" y comprobar que la respuesta recibida es
		 * "loginok". En tal caso, devuelve verdadero, falso si la respuesta no contiene
		 * los datos esperados.
		 */
		boolean success = false;

		String strToSend = new String("login");
		byte[] dataToSend = strToSend.getBytes();
		byte[] receivedData = null;
		boolean communicationOk = true;

		try {
			receivedData = sendAndReceiveDatagrams(dataToSend);
		} catch (IOException e) {
			communicationOk = false;
			System.err.println(" Error in testSendAndReceive ");
		}

		if (communicationOk && receivedData != null) {
			int receivedDataLen = receivedData.length;
			if (receivedDataLen > 0) {
				String strFromServer = new String(receivedData, 0, receivedDataLen);
				if (strFromServer.equals("loginok")) {
					success = true;
				}
			}
		}
		return success;
	}

	public InetSocketAddress getDirectoryAddress() {
		return directoryAddress;
	}

	public int getSessionKey() {
		return sessionKey;
	}

	/**
	 * Método para "iniciar sesión" en el directorio, comprobar que está operativo y
	 * obtener la clave de sesión asociada a este usuario.
	 * 
	 * @param nickname El nickname del usuario a registrar
	 * @return La clave de sesión asignada al usuario que acaba de loguearse, o -1
	 *         en caso de error
	 */
	public boolean logIntoDirectory(String nickname) {
		assert (sessionKey == INVALID_SESSION_KEY);
		boolean success = false;
		byte[] dataToSend = null;
		byte[] receivedData = null;
		// 1.Crear el mensaje a enviar (objeto DirMessage) con atributos adecuados
		// (operation, etc.) NOTA: Usar como operaciones las constantes definidas en la
		// clase DirMessageOps
		DirMessage messageToSend = new DirMessage(DirMessageOps.OPERATION_LOGIN);
		messageToSend.setNickname(nickname);

		// 2.Convertir el objeto DirMessage a enviar a un string (método toString)
		String strToSend = messageToSend.toString();

		// 3.Crear un datagrama con los bytes en que se codifica la cadena
		dataToSend = strToSend.getBytes();

		// 4.Enviar datagrama y recibir una respuesta (sendAndReceiveDatagrams).

		try {
			receivedData = sendAndReceiveDatagrams(dataToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (receivedData == null) {
			return false;
		}
		// 5.Convertir respuesta recibida en un objeto DirMessage (método
		// DirMessage.fromString)
		DirMessage messageReceived = DirMessage.fromString(new String(receivedData));

		// 6.Extraer datos del objeto DirMessage y procesarlos (p.ej., sessionKey)
		sessionKey = messageReceived.getSessionKey();

		// 7.Devolver éxito/fracaso de la operación
		success = messageReceived.getOperation().equals(DirMessageOps.OPERATION_LOGIN_OK);
		return success;

	}

	/**
	 * Método para obtener la lista de "nicknames" registrados en el directorio.
	 * Opcionalmente, la respuesta puede indicar para cada nickname si dicho peer
	 * está sirviendo ficheros en este instante.
	 * 
	 * @return La lista de nombres de usuario registrados, o null si el directorio
	 *         no pudo satisfacer nuestra solicitud
	 */
	public String[] getUserList() {
		String[] userlist = null;

		// Ver TODOs en logIntoDirectory y seguir esquema similar

		DirMessage messageToSend = new DirMessage(DirMessageOps.OPERATION_GETUSERLIST);
		messageToSend.setSessionKey(this.sessionKey);

		byte[] receivedData = null;
		try {
			receivedData = sendAndReceiveDatagrams(messageToSend.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		DirMessage responseMessage = DirMessage.fromString(new String(receivedData));
		userlist = responseMessage.getUserList();
		return userlist;
	}

	/**
	 * Método para "cerrar sesión" en el directorio
	 * 
	 * @return Verdadero si el directorio eliminó a este usuario exitosamente
	 */
	public boolean logoutFromDirectory() {
		// TODO: Ver TODOs en logIntoDirectory y seguir esquema similar
		boolean success = false;
		byte[] dataToSend = null;
		byte[] receivedData = null;

		DirMessage messageToSend = new DirMessage(DirMessageOps.OPERATION_LOGOUT);
		messageToSend.setSessionKey(sessionKey);

		try {
			receivedData = sendAndReceiveDatagrams(messageToSend.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		String receivedDataString = new String(receivedData);
		DirMessage messageReceived = DirMessage.fromString(receivedDataString);

		success = messageReceived.getOperation().equals(DirMessageOps.OPERATION_LOGOUT_OK);

		if (success) {
			sessionKey = INVALID_SESSION_KEY;
		}
		return success;
	}

	/**
	 * Método para dar de alta como servidor de ficheros en el puerto indicado a
	 * este peer.
	 * 
	 * @param serverPort El puerto TCP en el que este peer sirve ficheros a otros
	 * @return Verdadero si el directorio acepta que este peer se convierta en
	 *         servidor.
	 */
	public boolean registerServerPort(int serverPort) {
		// TODO: Ver TODOs en logIntoDirectory y seguir esquema similar
		boolean success = false;
		byte[] dataReceived = null;

		DirMessage messageToSend = new DirMessage(DirMessageOps.OPERATION_REGISTER_SERVER);
		messageToSend.setSessionKey(this.sessionKey);
		messageToSend.setServerPort(serverPort);

		try {
			dataReceived = sendAndReceiveDatagrams(messageToSend.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		String receivedDataString = new String(dataReceived);
		DirMessage messageReceived = DirMessage.fromString(receivedDataString);

		success = messageReceived.getOperation().equals(DirMessageOps.OPERATION_REGISTER_SERVER_OK);

		return success;
	}

	/**
	 * Método para dar de baja como servidor de ficheros a este peer.
	 * 
	 * @return Verdadero si el directorio acepta darlo de baja
	 */
	public boolean unregisterFileServer() {
		boolean success = false;
		DirMessage messageToSend = new DirMessage(DirMessageOps.OPERATION_UNREGISTER_SERVER);
		messageToSend.setSessionKey(sessionKey);

		byte[] dataReceived = null;
		try {
			dataReceived = sendAndReceiveDatagrams(messageToSend.toString().getBytes());
			success = true;
		} catch (IOException e) {
			e.printStackTrace();
			success = false;
		}
		String receivedDataString = new String(dataReceived);
		DirMessage responseMessage = DirMessage.fromString(receivedDataString);

		success = responseMessage.getOperation().equals(DirMessageOps.OPERATION_UNREGISTER_SERVER_OK);

		return success;
	}

	/**
	 * Método para obtener del directorio la dirección de socket (IP:puerto)
	 * asociada a un determinado nickname.
	 * 
	 * @param nick El nickname del servidor de ficheros por el que se pregunta
	 * @return La dirección de socket del servidor en caso de que haya algún
	 *         servidor dado de alta en el directorio con ese nick, o null en caso
	 *         contrario.
	 */
	public InetSocketAddress lookupServerAddrByUsername(String nick) {
		InetSocketAddress serverAddr = null;

		boolean success = false;
		DirMessage messageToSend = new DirMessage(DirMessageOps.OPERATION_GETSERVERADDRESS);
		messageToSend.setSessionKey(sessionKey);
		messageToSend.setNickname(nick);

		byte[] dataReceived = null;
		try {
			dataReceived = sendAndReceiveDatagrams(messageToSend.toString().getBytes());
			success = true;
		} catch (IOException e) {
			e.printStackTrace();
			success = false;
		}
		String receivedDataString = new String(dataReceived);
		DirMessage responseMessage = DirMessage.fromString(receivedDataString);

		success = responseMessage.getOperation().equals(DirMessageOps.OPERATION_SERVERADDRESS_OK);
		if (success) {
			serverAddr = responseMessage.getAddress();
		}

		return serverAddr;
	}

	/**
	 * Método para publicar ficheros que este peer servidor de ficheros están
	 * compartiendo.
	 * 
	 * @param files La lista de ficheros que este peer está sirviendo.
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y acepta la lista de ficheros, falso en caso contrario.
	 */
	public boolean publishLocalFiles(FileInfo[] files) {
		boolean success = false;
		if(files == null || files.length == 0) {
			System.err.println("There are no local files to publish");
			return false;
		}
		DirMessage messageToSend = new DirMessage(DirMessageOps.OPERATION_PUBLISH);
		messageToSend.setSessionKey(sessionKey);
		messageToSend.setFiles(files);

		byte[] dataReceived = null;
		try {
			dataReceived = sendAndReceiveDatagrams(messageToSend.toString().getBytes());
			success = true;
		} catch (IOException e) {
			e.printStackTrace();
			success = false;
		}
		String receivedDataString = new String(dataReceived);
		DirMessage responseMessage = DirMessage.fromString(receivedDataString);

		success = responseMessage.getOperation().equals(DirMessageOps.OPERATION_PUBLISH_OK);

		return success;
	}

	/**
	 * Método para obtener la lista de ficheros que los peers servidores han
	 * publicado al directorio. Para cada fichero se debe obtener un objeto FileInfo
	 * con nombre, tamaño y hash. Opcionalmente, puede incluirse para cada fichero,
	 * su lista de peers servidores que lo están compartiendo.
	 * 
	 * @return Los ficheros publicados al directorio, o null si el directorio no
	 *         pudo satisfacer nuestra solicitud
	 */
	public FileInfo[] getFileList() {
		FileInfo[] filelist = null;

		DirMessage messageToSend = new DirMessage(DirMessageOps.OPERATION_FILELIST);
		messageToSend.setSessionKey(sessionKey);

		byte[] dataReceived = null;
		try {
			dataReceived = sendAndReceiveDatagrams(messageToSend.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		String receivedDataString = new String(dataReceived);
		DirMessage responseMessage = DirMessage.fromString(receivedDataString);

		if (responseMessage.getOperation().equals(DirMessageOps.OPERATION_FILELIST_OK)) {
			filelist = responseMessage.getFiles();
		}

		return filelist;
	}

	/**
	 * Método para obtener la lista de nicknames de los peers servidores que tienen
	 * un fichero identificado por su hash. Opcionalmente, puede aceptar también
	 * buscar por una subcadena del hash, en vez de por el hash completo.
	 * 
	 * @return La lista de nicknames de los servidores que han publicado al
	 *         directorio el fichero indicado. Si no hay ningún servidor, devuelve
	 *         una lista vacía.
	 */
	public String[] getServerNicknamesSharingThisFile(String fileHash) {
		String[] nickslist = null;
		DirMessage messageToSend = new DirMessage(DirMessageOps.OPERATION_SEARCH);
		messageToSend.setSessionKey(sessionKey);
		messageToSend.setFileHash(fileHash);

		byte[] dataReceived = null;
		try {
			dataReceived = sendAndReceiveDatagrams(messageToSend.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		String receivedDataString = new String(dataReceived);
		DirMessage responseMessage = DirMessage.fromString(receivedDataString);

		if (responseMessage.getOperation().equals(DirMessageOps.OPERATION_SEARCH_OK)) {
			nickslist = responseMessage.getNicks();
		}

		return nickslist;
	}

}
