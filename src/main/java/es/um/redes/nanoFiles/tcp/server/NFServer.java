package es.um.redes.nanoFiles.tcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Servidor que se ejecuta en un hilo propio. Creará objetos
 * {@link NFServerThread} cada vez que se conecte un cliente.
 */
public class NFServer implements Runnable {

	private ServerSocket serverSocket = null;
	private boolean stopServer = false;
	private static final int SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS = 1000;

	private final int port;

	public NFServer() throws IOException {
		/*
		 * Crear un socket servidor y ligarlo a cualquier puerto disponible
		 */
		serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress(0));
		port = serverSocket.getLocalPort();

	}

	/**
	 * Método que crea un socket servidor y ejecuta el hilo principal del servidor,
	 * esperando conexiones de clientes.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (!stopServer) {
			try {
				/*
				 * Usar el socket servidor para esperar conexiones de otros peers que soliciten
				 * descargar ficheros
				 */
				Socket socket = serverSocket.accept();
				System.out.println(
						"New client connected: " + socket.getInetAddress().toString() + ":" + socket.getPort());
				/*
				 * Al establecerse la conexión con un peer, la comunicación con dicho cliente se
				 * hace en el método NFServerComm.serveFilesToClient(socket), al cual hay que
				 * pasarle el socket devuelto por accept
				 */
				/*
				 * (Opcional) Crear un hilo nuevo de la clase NFServerThread, que llevará a cabo
				 * la comunicación con el cliente que se acaba de conectar, mientras este hilo
				 * vuelve a quedar a la escucha de conexiones de nuevos clientes (para soportar
				 * múltiples clientes). Si este hilo es el que se encarga de atender al cliente
				 * conectado, no podremos tener más de un cliente conectado a este servidor.
				 */
				NFServerThread serverThread = new NFServerThread(socket);
				serverThread.start();

			} catch (IOException e) {
				if (!stopServer) {
					System.err.println("Server exception: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		System.out.println("Server succesfully closed");

	}

	/**
	 * Añadir métodos a esta clase para: 1) Arrancar el servidor en un hilo nuevo
	 * que se ejecutará en segundo plano 2) Detener el servidor (stopserver) 3)
	 * Obtener el puerto de escucha del servidor etc.
	 */

	public int getPort() {
		return port;
	}

	public static NFServer startBackgroundServer() throws IOException {

		NFServer backgroundServer = new NFServer();
		Thread serverThread = new Thread(backgroundServer);
		serverThread.start();
		return backgroundServer;
	}

	public synchronized void stopBackgroundServer() {
		stopServer = true;
		try {
			// Cierra el socket del servidor para que serverSocket.accept() arroje una
			// SocketException
			if (serverSocket != null) {
				serverSocket.close();
			}
		} catch (SocketException e) {
			// No se hace nada
		} catch (IOException e) {
			System.err.println("Error al cerrar el socket del servidor: " + e.getMessage());
		}
	}

}
