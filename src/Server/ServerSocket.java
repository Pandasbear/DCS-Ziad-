package Server;

import java.rmi.server.ExportException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.nio.file.Paths;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

public class ServerSocket {

    private static final String SERVER_IP = System.getProperty("crest.server.host", "localhost");
    private static final int PORT = Integer.parseInt(System.getProperty("crest.server.port", "1099"));


    private static final String KEYSTORE_PATH = System.getProperty(
            "crest.keystore.path",
            Paths.get("server.keystore").toAbsolutePath().toString()
    );
    private static final String KEYSTORE_PASS = System.getProperty(
            "crest.keystore.password",
            "888888"
    );


    private static final boolean SSL = Boolean.parseBoolean(
            System.getProperty("crest.ssl", "true")
    );

    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", SERVER_IP);

            System.out.println("java.version=" + System.getProperty("java.version"));
            System.out.println("java.home=" + System.getProperty("java.home"));
            System.out.println("SSL=" + SSL);

            Registry reg;

            if (SSL) {

                System.setProperty("javax.net.ssl.keyStore", KEYSTORE_PATH);
                System.setProperty("javax.net.ssl.keyStorePassword", KEYSTORE_PASS);
                System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");

   
                System.clearProperty("javax.net.ssl.trustStore");
                System.clearProperty("javax.net.ssl.trustStorePassword");

                var csf = new SslRMIClientSocketFactory();
                var ssf = new SslRMIServerSocketFactory(null, null, false);
                try {
                    reg = LocateRegistry.createRegistry(PORT, csf, ssf);
                } catch (ExportException alreadyRunning) {
                    reg = LocateRegistry.getRegistry(SERVER_IP, PORT, csf);
                    reg.list();
                }

                System.out.println("keyStore=" + System.getProperty("javax.net.ssl.keyStore"));
            } else {
   
                System.clearProperty("javax.net.ssl.keyStore");
                System.clearProperty("javax.net.ssl.keyStorePassword");
                System.clearProperty("javax.net.ssl.keyStoreType");
                System.clearProperty("javax.net.ssl.trustStore");
                System.clearProperty("javax.net.ssl.trustStorePassword");

                try {
                    reg = LocateRegistry.createRegistry(PORT);
                } catch (ExportException alreadyRunning) {
                    reg = LocateRegistry.getRegistry(SERVER_IP, PORT);
                    reg.list();
                }
            }

            Database.init();
            ServiceImpl svc = new ServiceImpl();

            reg.rebind("Authorization", svc);

            System.out.println((SSL ? "SSL" : "NON-SSL") + " RMI Server started on port " + PORT);

        } catch (Exception e) {
            System.out.println("Server failed: " + e);
            e.printStackTrace();
        }
    }
}
