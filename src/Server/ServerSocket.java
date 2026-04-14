package Server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

public class ServerSocket {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 1099;


    private static final String KEYSTORE_PATH =
            "C:\\Users\\User\\Documents\\NetBeansProjects\\DCS\\server-keystore.p12";
    private static final String KEYSTORE_PASS = "888888";


    private static final boolean SSL = true;

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
                var ssf = new SslRMIServerSocketFactory(null, null, false); // no client auth
                reg = LocateRegistry.createRegistry(PORT, csf, ssf);

                System.out.println("keyStore=" + System.getProperty("javax.net.ssl.keyStore"));
            } else {
   
                System.clearProperty("javax.net.ssl.keyStore");
                System.clearProperty("javax.net.ssl.keyStorePassword");
                System.clearProperty("javax.net.ssl.keyStoreType");
                System.clearProperty("javax.net.ssl.trustStore");
                System.clearProperty("javax.net.ssl.trustStorePassword");

                reg = LocateRegistry.createRegistry(PORT);
            }

            Database.init();
            ServiceImpl svc = new ServiceImpl();

            reg.rebind("Authorization", svc);

            System.out.println((SSL ? "✅ SSL" : "⚠️ NON-SSL") + " RMI Server started on port " + PORT);

        } catch (Exception e) {
            System.out.println("❌ Server failed: " + e);
            e.printStackTrace();
        }
    }
}
