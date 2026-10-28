
package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import es.um.redes.nanoFiles.util.FileInfo;

public class PeerMessage {

	private byte opcode;

	/*
	 * Añadir atributos y crear otros constructores específicos para crear mensajes
	 * con otros campos (tipos de datos)
	 * 
	 */
	private int initialPos;
	private int finalPos;
	private int hashLength;
	private String fileHash;

	private long fileLength;
	private byte[] file;

	public PeerMessage() {
		opcode = PeerMessageOps.OPCODE_INVALID_CODE;
	}

	public PeerMessage(byte op) {
		opcode = op;
	}

	/*
	 * Crear métodos getter y setter para obtener valores de nuevos atributos,
	 * comprobando previamente que dichos atributos han sido establecidos por el
	 * constructor (sanity checks)
	 */

	public byte getOpcode() {
		return opcode;
	}

	public void setOpcode(byte opcode) {
		this.opcode = opcode;
	}

	public int getInitialPos() {
		return initialPos;
	}

	public void setInitialPos(int initialPos) {
		this.initialPos = initialPos;
	}

	public int getFinalPos() {
		return finalPos;
	}

	public void setFinalPos(int finalPos) {
		this.finalPos = finalPos;
	}

	public int getHashLength() {
		return hashLength;
	}

	public void setHashLength(int hashLength) {
		this.hashLength = hashLength;
	}

	public String getFileHash() {
		return fileHash;
	}

	public void setFileHash(String fileHash) {
		this.fileHash = fileHash;
		this.hashLength = fileHash.length();
	}

	private void setFileHash(byte[] fileHash) {
		this.fileHash = new String(fileHash);
	}

	public long getFileLength() {
		return fileLength;
	}

	public void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}

	/**
	 * Método de clase para parsear los campos de un mensaje y construir el objeto
	 * DirMessage que contiene los datos del mensaje recibido
	 * 
	 * @param data El array de bytes recibido
	 * @return Un objeto de esta clase cuyos atributos contienen los datos del
	 *         mensaje recibido.
	 * @throws IOException
	 */
	public static PeerMessage readMessageFromInputStream(DataInputStream dis) throws IOException {
		/*
		 * TODO: En función del tipo de mensaje, leer del socket a través del "dis" el
		 * resto de campos para ir extrayendo con los valores y establecer los atributos
		 * del un objeto DirMessage que contendrá toda la información del mensaje, y que
		 * será devuelto como resultado. NOTA: Usar dis.readFully para leer un array de
		 * bytes, dis.readInt para leer un entero, etc.
		 */
		PeerMessage message = new PeerMessage(dis.readByte());
		byte opcode = message.getOpcode();

		switch (opcode) {
		case PeerMessageOps.OPCODE_DOWNLOAD_REQUEST: {
			message.setInitialPos(dis.readInt());
			message.setFinalPos(dis.readInt());
			message.setHashLength(dis.readInt());

			byte[] fileHashBytes = new byte[message.getHashLength()];
			dis.read(fileHashBytes);
			message.setFileHash(fileHashBytes);
			break;
		}
		case PeerMessageOps.OPCODE_FILE_NOT_FOUND: {
			break;
		}
		case PeerMessageOps.OPCODE_NON_SIGNIFICANT_HASH: {
			break;
		}
		case PeerMessageOps.OPCODE_SEND_FILE: {
			int hashLength = dis.readInt();
			message.setHashLength(hashLength);
			byte[] hash = new byte[hashLength];
			dis.readFully(hash);
			message.setFileHash(hash);

			long fileLength = dis.readLong();
			message.setFileLength(fileLength);
			byte[] data = new byte[(int) fileLength];
			dis.readFully(data);
			message.setFile(data);
			break;
		}

		default:
			System.err.println("PeerMessage.readMessageFromInputStream doesn't know how to parse this message opcode: "
					+ PeerMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return message;
	}

	public void writeMessageToOutputStream(DataOutputStream dos) throws IOException {
		/*
		 * Escribir los bytes en los que se codifica el mensaje en el socket a través
		 * del "dos", teniendo en cuenta opcode del mensaje del que se trata y los
		 * campos relevantes en cada caso. NOTA: Usar dos.write para leer un array de
		 * bytes, dos.writeInt para escribir un entero, etc.
		 */

		dos.writeByte(opcode);
		switch (opcode) {
		case PeerMessageOps.OPCODE_DOWNLOAD_REQUEST: {
			dos.writeInt(initialPos);
			dos.writeInt(finalPos);
			dos.writeInt(hashLength);
			dos.write(fileHash.getBytes());
			break;
		}
		case PeerMessageOps.OPCODE_FILE_NOT_FOUND: {
			break;
		}
		case PeerMessageOps.OPCODE_NON_SIGNIFICANT_HASH: {
			break;
		}
		case PeerMessageOps.OPCODE_SEND_FILE: {
			dos.writeInt(this.hashLength);
			byte[] bytes = this.fileHash.getBytes();
			dos.write(bytes);
			dos.writeLong(this.fileLength);
			dos.write(this.file);
			break;
		}

		default:
			System.err.println("PeerMessage.writeMessageToOutputStream found unexpected message opcode " + opcode + "("
					+ PeerMessageOps.opcodeToOperation(opcode) + ")");
		}
	}

}
