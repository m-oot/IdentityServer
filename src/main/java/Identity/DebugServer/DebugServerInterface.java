package Identity.DebugServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DebugServerInterface extends Remote {
    public int log(int serverId, String message) throws RemoteException;
    public int connect() throws RemoteException;
    public int logClient(String message) throws RemoteException;
}

