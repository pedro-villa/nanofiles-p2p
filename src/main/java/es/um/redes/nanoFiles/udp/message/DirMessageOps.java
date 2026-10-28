package es.um.redes.nanoFiles.udp.message;

public class DirMessageOps {

	/*
	 * Añadir aquí todas las constantes que definen los diferentes tipos de
	 * mensajes del protocolo de comunicación con el directorio.
	 */
	public static final String OPERATION_INVALID = "invalidOperation";
	public static final String OPERATION_LOGIN = "login";

	public static final String OPERATION_LOGIN_FAIL = "loginFail";
	public static final String OPERATION_LOGIN_OK = "loginOk";

	public static final String OPERATION_LOGOUT = "logout";
	public static final String OPERATION_LOGOUT_OK = "logoutOK";
	public static final String OPERATION_LOGOUT_FAIL = "logoutFail";

	public static final String OPERATION_GETUSERLIST = "getuserlist";
	public static final String OPERATION_USERLIST_OK = "userlistOK";
	public static final String OPERATION_USERLIST_FAIL = "userlistFail";

	public static final String OPERATION_REGISTER_SERVER = "registerServer";
	public static final String OPERATION_REGISTER_SERVER_FAIL = "registerServerFail";
	public static final String OPERATION_REGISTER_SERVER_OK = "registerServerOK";

	public static final String OPERATION_UNREGISTER_SERVER = "unregisterServer";
	public static final String OPERATION_UNREGISTER_SERVER_FAIL = "unregisterServerFail";
	public static final String OPERATION_UNREGISTER_SERVER_OK = "unregisterServerOK";

	public static final String OPERATION_GETSERVERADDRESS = "getServerAddress";
	public static final String OPERATION_SERVERADDRESS_OK = "serverAddressOK";
	public static final String OPERATION_SERVERADDRESS_FAIL = "serverAddressFail";

	public static final String OPERATION_PUBLISH = "publish";
	public static final String OPERATION_PUBLISH_OK = "publishOK";
	public static final String OPERATION_PUBLISH_FAIL = "publishFail";

	public static final String OPERATION_FILELIST = "filelist";
	public static final String OPERATION_FILELIST_OK = "filelistOK";
	public static final String OPERATION_FILELIST_FAILED = "filelistFail";

	public static final String OPERATION_SEARCH = "search";
	public static final String OPERATION_SEARCH_OK = "searchOK";
	public static final String OPERATION_SEARCH_FAILED = "searchFail";

}
