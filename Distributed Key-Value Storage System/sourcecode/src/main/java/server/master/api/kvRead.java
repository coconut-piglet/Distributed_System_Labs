package server.master.api;

import common.KeyValuePair;
import common.Message;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface kvRead extends Remote, Serializable {

    public Message read(KeyValuePair keyValuePair) throws RemoteException;

}
