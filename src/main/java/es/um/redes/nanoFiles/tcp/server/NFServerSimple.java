package es.um.redes.nanoFiles.tcp.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class NFServerSimple {

	private static final int SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS = 1000;
	private static final String STOP_SERVER_COMMAND = "fgstop";
	private static final int DEFAULT_PORT = 10000;
	private ServerSocket serverSocket = null;

	private static final int MAX_BINDING_ATTEMPTS = 100; // Número arbitrario menor que una cierta cantidad
	private int port;

	public NFServerSimple() throws IOException {
		/*
		 * Crear una direción de socket a partir del puerto especificado
		 */

		/*
		 * Crear un socket servidor y ligarlo a la dirección de socket anterior
		 */

		/*
		 * Mejora: fgserver ampliado. Si el puerto predeterminado no está disponible
		 * busca uno que sí lo esté
		 */

		boolean success = false;
		int bindingAttempts = 0;
		port = DEFAULT_PORT;
		while (!success && bindingAttempts < MAX_BINDING_ATTEMPTS) {
			try {
				InetSocketAddress serverAddress = new InetSocketAddress(port);

				serverSocket = new ServerSocket();
				serverSocket.bind(serverAddress);
				serverSocket.setReuseAddress(true); // se puede volver a reutilizar el puerto de forma inmediata
				success = true;
			} catch (IOException e) {
				System.out.println("Could not bind to port " + port + "");
				bindingAttempts++;
				port++;
				success = false;
			}
		}

		if (!success) {
			throw new IOException("Could not find a valid port");
		} else {
			System.out.println("Connected to port " + port);
		}
	}

	/**
	 * Método para ejecutar el servidor de ficheros en primer plano. Sólo es capaz
	 * de atender una conexión de un cliente. Una vez se lanza, ya no es posible
	 * interactuar con la aplicación a menos que se implemente la funcionalidad de
	 * detectar el comando STOP_SERVER_COMMAND (opcional)
	 * 
	 */
	public void run() {
		/*
		 * Comprobar que el socket servidor está creado y ligado
		 */
		if (serverSocket == null) {
			System.err.println("* Error: Failed to run the file server, the server socket not bound to any port.");
			return;
		}
		/*
		 * Usar el socket servidor para esperar conexiones de otros peers que soliciten
		 * descargar ficheros
		 */
		Socket socket = null;
		boolean stopServer = false;
		while (!stopServer) {
			try {
				socket = serverSocket.accept();
			} catch (IOException e) {
				socket = null;
				System.err.println("** Error: Problem accepting a connection.");
			}
			/*
			 * Al establecerse la conexión con un peer, la comunicación con dicho cliente se
			 * hace en el método NFServerComm.serveFilesToClient(socket), al cual hay que
			 * pasarle el socket devuelto por accept
			 */
			NFServerComm.serveFilesToClient(socket);
		}

		System.out.println("NFServerSimple stopped. Returning to the nanoFiles shell...");
	}
}
