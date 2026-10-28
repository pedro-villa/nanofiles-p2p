package es.um.redes.nanoFiles.udp.message;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases DirectoryServer y
 * DirectoryConnector, y se codifican como texto en formato "campo:valor".
 * 
 * @author rtitos
 *
 */
public class DirMessage {
	public static final int PACKET_MAX_SIZE = 65507; // 65535 - 8 (UDP header) - 20 (IP header)

	private static final char DELIMITER = ':'; // Define el delimitador
	private static final char LIST_DELIMITER = ','; // Define el delimitador usado para listas
	private static final char ENCODED_DELIMITER = '&'; // Define el carácter usado para codificar ':'. También se
														// utiliza como segundo delimitador en listas compuestas
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea

	/**
	 * Nombre del campo que define el tipo de mensaje (primera línea)
	 */
	private static final String FIELDNAME_OPERATION = "operation";
	/*
	 * Definir de manera simbólica los nombres de todos los campos que pueden
	 * aparecer en los mensajes de este protocolo (formato campo:valor)
	 */
	private static final String FIELDNAME_NICKNAME = "nickname";
	private static final String FIELDNAME_SESSIONKEY = "sessionkey";
	private static final String FIELDNAME_USERLIST = "userlist";
	private static final String FIELDNAME_SERVERPORT = "serverport";
	private static final String FIELDNAME_ADDRESS = "address";
	private static final String FIELDNAME_FILES = "files";
	private static final String FIELDNAME_FILEHASH = "filehash";
	private static final String FIELDNAME_NICKS = "nicks";
	private static final String FIELDNAME_ERRORMESSAGE = "errormessage";

	/* Constantes de inicialización de los atributos */
	public static final int INVALID_SESSIONKEY = -1;
	public static final int INVALID_SERVERPORT = -1;

	/**
	 * Tipo del mensaje, de entre los tipos definidos en PeerMessageOps.
	 */
	private String operation = DirMessageOps.OPERATION_INVALID;

	/*
	 * Crear un atributo correspondiente a cada uno de los campos de los diferentes
	 * mensajes de este protocolo.
	 */
	private String nickname = null;
	private int sessionKey = INVALID_SESSIONKEY;

	private String[] userList = null;
	private String[] nicks = null;

	private String fileHash = null;
	private String errorMessage = null;

	private FileInfo[] files = null;

	private boolean[] serverList = null;
	private int serverPort = INVALID_SERVERPORT;
	private InetSocketAddress address = null;

	public DirMessage(String op) {
		operation = op;
	}

	/*
	 * Crear diferentes constructores adecuados para construir mensajes de
	 * diferentes tipos con sus correspondientes argumentos (campos del mensaje)
	 */

	public String getOperation() {
		return operation;
	}

	public void setNickname(String nick) {
		assert (operation.equals(DirMessageOps.OPERATION_LOGIN));
		nickname = nick;
	}

	public String getNickname() {
		return nickname;
	}

	public int getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(int sessionKey) {
		assert (operation.equals(DirMessageOps.OPERATION_LOGIN));
		this.sessionKey = sessionKey;
	}

	public void setUserList(String[] userList) {
		this.userList = userList;
	}

	public String[] getUserList() {
		return userList;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void setServerList(boolean[] serverList) {
		this.serverList = serverList;
	}

	public boolean[] getServerList() {
		return serverList;
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}

	public FileInfo[] getFiles() {
		return files;
	}

	public void setFiles(FileInfo[] files) {
		this.files = files;
	}

	public void setFiles(String[] files) throws NumberFormatException {
		assert (files != null);
		FileInfo[] fileInfos = new FileInfo[files.length];
		for (int i = 0; i < files.length; i++) {
			String[] fileData = files[i].split(ENCODED_DELIMITER + "");
			fileInfos[i] = new FileInfo(fileData[0], fileData[1], Long.parseLong(fileData[2]), "");
		}
		this.files = fileInfos;
	}

	public String getFileHash() {
		return fileHash;
	}

	public void setFileHash(String fileHash) {
		this.fileHash = fileHash;
	}

	public String[] getNicks() {
		return nicks;
	}

	public void setNicks(String[] nicks) {
		this.nicks = nicks;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * Método que convierte un mensaje codificado como una cadena de caracteres, a
	 * un objeto de la clase PeerMessage, en el cual los atributos correspondientes
	 * han sido establecidos con el valor de los campos del mensaje.
	 * 
	 * @param message El mensaje recibido por el socket, como cadena de caracteres
	 * @return Un objeto PeerMessage que modela el mensaje recibido (tipo, valores,
	 *         etc.)
	 */
	public static DirMessage fromString(String message) {
		/*
		 * Usar un bucle para parsear el mensaje línea a línea, extrayendo para cada
		 * línea el nombre del campo y el valor, usando el delimitador DELIMITER, y
		 * guardarlo en variables locales.
		 */
		if (NanoFiles.debugMode) {
			System.out.println("DirMessage read from socket:");
			System.out.println(message);
		}

		String[] lines = message.split(END_LINE + ""); // la cadena vacía se le suma para convertirlo a cadena
														// directamente
		// Local variables to save data during parsing
		DirMessage m = null;

		for (String line : lines) {
			int idx = line.indexOf(DELIMITER); // Posición del delimitador
			String fieldName = line.substring(0, idx).toLowerCase(); // minúsculas
			String value = line.substring(idx + 1).trim();

			switch (fieldName) {
			case FIELDNAME_OPERATION: {
				assert (m == null);
				m = new DirMessage(value);
				break;
			}
			case FIELDNAME_NICKNAME: {
				assert (m != null);
				m.setNickname(value);
				break;
			}
			case FIELDNAME_SESSIONKEY: {
				m.setSessionKey(Integer.parseInt(value));
				break;
			}
			case FIELDNAME_USERLIST: {
				m.setUserList(value.split(LIST_DELIMITER + ""));
				break;
			}
			case FIELDNAME_SERVERPORT: {
				m.setServerPort(Integer.parseInt(value));
				break;
			}
			case FIELDNAME_ADDRESS: {
				String[] strAddress = value.split(ENCODED_DELIMITER + "");
				if (strAddress.length == 2) {
					m.address = new InetSocketAddress(strAddress[0], Integer.parseInt(strAddress[1]));
				}
				break;
			}
			case FIELDNAME_FILES: {
				if (value.equals("")) {
					m.setFiles(new FileInfo[0]);
				} else {
					try {
						m.setFiles(value.split(","));
					} catch (NumberFormatException e) {
						m.setFiles(new FileInfo[0]);
					}
				}
				break;
			}
			case FIELDNAME_FILEHASH: {
				m.setFileHash(value);
				break;
			}
			case FIELDNAME_NICKS: {
				m.setNicks(value.split(LIST_DELIMITER + ""));
				break;
			}
			case FIELDNAME_ERRORMESSAGE: {
				m.setErrorMessage(value);
				break;
			}
			default:
				System.err.println("PANIC: DirMessage.fromString - message with unknown field name " + fieldName);
				System.err.println("Message was:\n" + message);
				System.exit(-1);
			}
		}
		return m;
	}

	/**
	 * Método que devuelve una cadena de caracteres con la codificación del mensaje
	 * según el formato campo:valor, a partir del tipo y los valores almacenados en
	 * los atributos.
	 * 
	 * @return La cadena de caracteres con el mensaje a enviar por el socket.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(FIELDNAME_OPERATION + DELIMITER + operation + END_LINE); // Construimos el campo

		/*
		 * En función del tipo de mensaje, crear una cadena con el tipo y
		 * concatenar el resto de campos necesarios usando los valores de los atributos
		 * del objeto.
		 */
		switch (operation) {
		case DirMessageOps.OPERATION_LOGIN:
			sb.append(FIELDNAME_NICKNAME + DELIMITER + nickname + END_LINE);
			break;
		case DirMessageOps.OPERATION_LOGIN_OK:
			sb.append(FIELDNAME_SESSIONKEY + DELIMITER + sessionKey + END_LINE);
			break;
		case DirMessageOps.OPERATION_LOGOUT:
			sb.append(FIELDNAME_SESSIONKEY + DELIMITER + sessionKey + END_LINE);
			break;
		case DirMessageOps.OPERATION_GETUSERLIST:
			sb.append(FIELDNAME_SESSIONKEY + DELIMITER + sessionKey + END_LINE);
			break;
		case DirMessageOps.OPERATION_USERLIST_OK:
			sb.append(FIELDNAME_USERLIST + DELIMITER);
			for (int i = 0; i < userList.length - 1; i++) {
				sb.append(userList[i] + ENCODED_DELIMITER + serverList[i] + LIST_DELIMITER);
			}
			sb.append(userList[userList.length - 1] + ENCODED_DELIMITER + serverList[serverList.length - 1] + END_LINE);
			break;
		case DirMessageOps.OPERATION_REGISTER_SERVER:
			sb.append(FIELDNAME_SESSIONKEY + DELIMITER + sessionKey + END_LINE);
			sb.append(FIELDNAME_SERVERPORT + DELIMITER + serverPort);
			break;
		case DirMessageOps.OPERATION_UNREGISTER_SERVER:
			sb.append(FIELDNAME_SESSIONKEY + DELIMITER + sessionKey + END_LINE);
			break;
		case DirMessageOps.OPERATION_GETSERVERADDRESS:
			sb.append(FIELDNAME_SESSIONKEY + DELIMITER + sessionKey + END_LINE);
			sb.append(FIELDNAME_NICKNAME + DELIMITER + nickname + END_LINE);
			break;
		case DirMessageOps.OPERATION_SERVERADDRESS_OK:
			sb.append(FIELDNAME_NICKNAME + DELIMITER + nickname + END_LINE);
			sb.append(FIELDNAME_ADDRESS + DELIMITER + address.getAddress().getHostAddress() + ENCODED_DELIMITER
					+ address.getPort());
			break;
		case DirMessageOps.OPERATION_PUBLISH:
			sb.append(FIELDNAME_SESSIONKEY + DELIMITER + sessionKey + END_LINE);

			sb.append(FIELDNAME_FILES + DELIMITER);
			for (int i = 0; i < files.length - 1; i++) {
				sb.append(files[i].fileHash + ENCODED_DELIMITER + files[i].fileName + ENCODED_DELIMITER
						+ files[i].fileSize + LIST_DELIMITER);
			}
			sb.append(files[files.length - 1].fileHash + ENCODED_DELIMITER + files[files.length - 1].fileName
					+ ENCODED_DELIMITER + files[files.length - 1].fileSize + END_LINE);
			break;
		case DirMessageOps.OPERATION_FILELIST: {
			sb.append(FIELDNAME_SESSIONKEY + DELIMITER + sessionKey + END_LINE);
			break;
		}
		case DirMessageOps.OPERATION_FILELIST_OK: {
			sb.append(FIELDNAME_FILES + DELIMITER);
			if (files.length == 0) {
				sb.append(END_LINE);
			} else {
				for (int i = 0; i < files.length - 1; i++) {
					sb.append(files[i].fileHash + ENCODED_DELIMITER + files[i].fileName + ENCODED_DELIMITER
							+ files[i].fileSize + LIST_DELIMITER);
				}
				sb.append(files[files.length - 1].fileHash + ENCODED_DELIMITER + files[files.length - 1].fileName
						+ ENCODED_DELIMITER + files[files.length - 1].fileSize + END_LINE);
			}
			break;
		}
		case DirMessageOps.OPERATION_SEARCH: {
			sb.append(FIELDNAME_SESSIONKEY + DELIMITER + sessionKey + END_LINE);
			sb.append(FIELDNAME_FILEHASH + DELIMITER + fileHash + END_LINE);
			break;
		}
		case DirMessageOps.OPERATION_SEARCH_OK: {
			sb.append(FIELDNAME_NICKS + DELIMITER);
			if (nicks.length == 0) {
				sb.append(END_LINE);
			} else {
				for (int i = 0; i < nicks.length - 1; i++) {
					sb.append(nicks[i] + LIST_DELIMITER);
				}
				sb.append(nicks[nicks.length - 1] + END_LINE);
			}
			break;
		}
		case DirMessageOps.OPERATION_SEARCH_FAILED: {
			sb.append(FIELDNAME_ERRORMESSAGE + DELIMITER + errorMessage);
		}

		default:
			break;
		}
		sb.append(END_LINE); // Marcamos el final del mensaje
		
		String message = sb.toString();
		
		if(NanoFiles.debugMode) {
			System.out.println("Message sent: ");
			System.out.println(message);
		}
		return message;
	}
}
