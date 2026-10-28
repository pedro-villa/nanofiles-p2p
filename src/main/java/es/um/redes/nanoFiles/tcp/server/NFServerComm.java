package es.um.redes.nanoFiles.tcp.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFServerComm {

	public static void serveFilesToClient(Socket socket) {

		DataOutputStream dos = null;
		DataInputStream dis = null;

		int initialPos = -1;
		int finalPos = -1;
		String fileHash = null;

		PeerMessage messageFromClient = null;
		PeerMessage serverResponse = null;

		try {
			/*
			 * Crear dis/dos a partir del socket
			 */
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());

			/*
			 * Mientras el cliente esté conectado, leer mensajes de socket, convertirlo a un
			 * objeto PeerMessage y luego actuar en función del tipo de mensaje recibido,
			 * enviando los correspondientes mensajes de respuesta.
			 * 
			 * 
			 */
			messageFromClient = PeerMessage.readMessageFromInputStream(dis);

			boolean downloadCompleteFile = false;
			switch (messageFromClient.getOpcode()) {
			case PeerMessageOps.OPCODE_DOWNLOAD_REQUEST: {
				initialPos = messageFromClient.getInitialPos();
				finalPos = messageFromClient.getFinalPos();
				fileHash = messageFromClient.getFileHash();

				/*
				 * Si la posición final es 0, entonces se descarga el fichero completo, tal y
				 * como se especifica en el protocolo
				 */
				downloadCompleteFile = finalPos == 0;
				break;
			}
			default: {
				System.err.println("NFServerComm.serveFilesToClient found unexpected opcode in client request: "
						+ PeerMessageOps.opcodeToOperation(messageFromClient.getOpcode()));
				break;
			}
			}

			/*
			 * en nuestra base de datos de ficheros compartidos. Los ficheros compartidos se
			 * pueden obtener con NanoFiles.db.getFiles(). El método
			 * FileInfo.lookupHashSubstring es útil para buscar coincidencias de una
			 * subcadena del hash. El método NanoFiles.db.lookupFilePath(targethash)
			 * devuelve la ruta al fichero a partir de su hash completo.
			 */

			FileInfo[] matchingFiles = FileInfo.lookupHashSubstring(NanoFiles.db.getFiles(), fileHash);
			FileInfo fileInfo = null;
			byte opcodeResponse = 0;

			if (matchingFiles.length == 0) {
				System.out.println("No matching files for hash: " + fileHash + ". Sending response message "
						+ PeerMessageOps.opcodeToOperation(PeerMessageOps.OPCODE_FILE_NOT_FOUND));
				opcodeResponse = PeerMessageOps.OPCODE_FILE_NOT_FOUND;

			} else if (matchingFiles.length > 1) {
				System.out.println("Multiple files match for the hash provided (" + fileHash + "): ");
				for (FileInfo f : matchingFiles) {
					System.out.println(f.fileName);
				}

				System.out.println("Sending response message "
						+ PeerMessageOps.opcodeToOperation(PeerMessageOps.OPCODE_NON_SIGNIFICANT_HASH));
				opcodeResponse = PeerMessageOps.OPCODE_NON_SIGNIFICANT_HASH;
			} else if (matchingFiles.length == 1) {
				System.out.println("File succesfully found through hash. Sending response message: "
						+ PeerMessageOps.opcodeToOperation(PeerMessageOps.OPCODE_SEND_FILE));
				fileInfo = matchingFiles[0];
				opcodeResponse = PeerMessageOps.OPCODE_SEND_FILE;
			}

			serverResponse = new PeerMessage(opcodeResponse);
			switch (opcodeResponse) {
			case PeerMessageOps.OPCODE_FILE_NOT_FOUND: {
				break;
			}
			case PeerMessageOps.OPCODE_NON_SIGNIFICANT_HASH: {
				break;
			}
			case PeerMessageOps.OPCODE_SEND_FILE: {
				serverResponse.setFileHash(fileInfo.fileHash);
				serverResponse.setFileLength(fileInfo.fileSize);

				File fileToSend = new File(fileInfo.filePath);
				DataInputStream fileDis = new DataInputStream(new FileInputStream(fileToSend));
				byte[] fileToSendBytes = new byte[(int) fileInfo.fileSize];
				fileDis.readFully(fileToSendBytes);
				serverResponse.setFile(fileToSendBytes);

				fileDis.close();
				break;
			}
			default: {
				System.err.println("NFServerComm.serveFilesToClient found unexpected message opcode " + opcodeResponse
						+ "(" + PeerMessageOps.opcodeToOperation(opcodeResponse) + ")");
			}
			}
			serverResponse.writeMessageToOutputStream(dos);
		} catch (IOException e) {
			System.err.println("Error: IOException ocurred while processing client response");
			e.printStackTrace();
		}

	}

}
