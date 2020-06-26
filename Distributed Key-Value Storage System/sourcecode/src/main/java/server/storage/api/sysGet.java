package server.storage.api;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface sysGet extends Remote, Serializable {

    public String get(String key) throws RemoteException;

}
