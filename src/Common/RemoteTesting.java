package Common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteTesting extends Remote {
    String ping() throws RemoteException;
}
