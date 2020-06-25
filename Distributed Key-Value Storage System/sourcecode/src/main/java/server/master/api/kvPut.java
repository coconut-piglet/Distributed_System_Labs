package server.master.api;

import common.KeyValuePair;
import common.Message;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface kvPut extends Remote, Serializable {

    public Message put(KeyValuePair keyValuePair) throws RemoteException;

}
