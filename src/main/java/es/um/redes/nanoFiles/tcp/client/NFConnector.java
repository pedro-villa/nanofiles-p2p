package es.um.redes.nanoFiles.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileDigest;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor
public class NFConnector {
	private Socket socket;
	private InetSocketAddress serverAddr;

	protected DataOutputStream dos = null; // puede ser privada
	protected DataInputStream dis = null;

	public NFConnector(InetSocketAddress fserverAddr) throws UnknownHostException, IOException {
		serverAddr = fserverAddr;
		/*
		 * Se crea el socket a partir de la dirección del servidor (IP, puerto). La
		 * creación exitosa del socket significa que la conexión TCP ha sido
		 * establecida.
		 */
		socket = new Socket(serverAddr.getAddress(), serverAddr.getPort());
		System.out.println("Connected to ... " + serverAddr.getAddress().toString() + ":"
				+ Integer.toString(serverAddr.getPort())); // esto es informativo

		/*
		 * Se crean los DataInputStream/DataOutputStream a partir de los streams de
		 * entrada/salida del socket creado. Se usarán para enviar (dos) y recibir (dis)
		 * datos del servidor.
		 */

		dos = new DataOutputStream(socket.getOutputStream());
		dis = new DataInputStream(socket.getInputStream());

	}

	/**
	 * Método para descargar un fichero a través del socket mediante el que estamos
	 * conectados con un peer servidor.
	 * 
	 * @param targetFileHashSubstr Subcadena del hash del fichero a descargar
	 * @param file                 El objeto File que referencia el nuevo fichero
	 *                             creado en el cual se escribirán los datos
	 *                             descargados del servidor
	 * @return Verdadero si la descarga se completa con éxito, falso en caso
	 *         contrario.
	 * @throws IOException Si se produce algún error al leer/escribir del socket.
	 */
	public boolean downloadFile(String targetFileHashSubstr, File file) throws IOException {
		boolean success = false;
		boolean downloaded = false;
		/*
		 * Construir objetos PeerMessage que modelen mensajes con los valores adecuados
		 * en sus campos (atributos), según el protocolo diseñado, y enviarlos al
		 * servidor a través del "dos" del socket mediante el método
		 * writeMessageToOutputStream.
		 */
		PeerMessage messageToSend = new PeerMessage(PeerMessageOps.OPCODE_DOWNLOAD_REQUEST);
		messageToSend.setInitialPos(0);
		messageToSend.setFinalPos(0);

		messageToSend.setFileHash(targetFileHashSubstr);

		messageToSend.writeMessageToOutputStream(dos);
		System.out.println("Requesting to download file with the following hash: " + targetFileHashSubstr);
		/*
		 * Recibir mensajes del servidor a través del "dis" del socket usando
		 * PeerMessage.readMessageFromInputStream, y actuar en función del tipo de
		 * mensaje recibido, extrayendo los valores necesarios de los atributos del
		 * objeto (valores de los campos del mensaje).
		 */
		PeerMessage messageFromServer = PeerMessage.readMessageFromInputStream(dis);
		byte opcode = messageFromServer.getOpcode();
		byte[] fileBytes = null;
		String fileHash = null;
		switch (opcode) {
		case PeerMessageOps.OPCODE_FILE_NOT_FOUND: {
			System.err.println("Server could not find the file requested");
			break;
		}
		case PeerMessageOps.OPCODE_NON_SIGNIFICANT_HASH: {
			System.err.println("Hash is non significant, more than one file with the same hash were found");
			break;
		}
		case PeerMessageOps.OPCODE_SEND_FILE: {
			downloaded = true;
			System.out.println("File succesfully downloaded from server");

			fileBytes = messageFromServer.getFile();
			fileHash = messageFromServer.getFileHash();
			break;
		}

		default: {
			System.err.println("NFConnector.downloadFile found unexpected opcode in server response: "
					+ PeerMessageOps.opcodeToOperation(opcode));
			break;
		}
		}
		if (downloaded) {
			/*
			 * Para escribir datos de un fichero recibidos en un mensaje, se puede crear un
			 * FileOutputStream a partir del parámetro "file" para escribir cada fragmento
			 * recibido (array de bytes) en el fichero mediante el método "write". Cerrar el
			 * FileOutputStream una vez se han escrito todos los fragmentos.
			 */
			/*
			 * NOTA: Hay que tener en cuenta que puede que la subcadena del hash pasada como
			 * parámetro no identifique unívocamente ningún fichero disponible en el
			 * servidor (porque no concuerde o porque haya más de un fichero coincidente con
			 * dicha subcadena)
			 */
			FileOutputStream ffos = new FileOutputStream(file);
			ffos.write(fileBytes);
			ffos.close();

			/*
			 * Finalmente, comprobar la integridad del fichero creado para comprobar que es
			 * idéntico al original, calculando el hash a partir de su contenido con
			 * FileDigest.computeFileChecksumString y comparándolo con el hash completo del
			 * fichero solicitado. Para ello, es necesario obtener del servidor el hash
			 * completo del fichero descargado, ya que quizás únicamente obtuvimos una
			 * subcadena del mismo como parámetro.
			 */
			String checkSum = FileDigest.computeFileChecksumString(file.getCanonicalPath());
			if (!checkSum.equals(fileHash)) {
				System.err.println("Checksum error. Downloaded file is corrupt");
			} else {
				success = true;
			}

		}
		socket.close();

		return success;
	}

	public InetSocketAddress getServerAddr() {
		return serverAddr;
	}

}
