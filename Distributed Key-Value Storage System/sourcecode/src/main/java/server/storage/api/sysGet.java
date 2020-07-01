package server.storage.api;

import common.KeyValuePair;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface sysGet extends Remote, Serializable {

    public KeyValuePair get(String key) throws RemoteException;

    public boolean ping(String key) throws RemoteException;

}
