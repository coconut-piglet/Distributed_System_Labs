package server.storage.api;

import common.KeyValuePair;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface sysPut extends Remote, Serializable {

    public void put(KeyValuePair keyValuePair) throws RemoteException;

}
