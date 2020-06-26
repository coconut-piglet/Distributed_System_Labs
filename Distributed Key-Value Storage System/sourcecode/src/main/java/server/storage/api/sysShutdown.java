package server.storage.api;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface sysShutdown extends Remote, Serializable {

    public void shutdown() throws RemoteException;

}
