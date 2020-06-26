package server.storage.api;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface sysPut extends Remote, Serializable {

    public void put(String key, String value) throws RemoteException;

}
