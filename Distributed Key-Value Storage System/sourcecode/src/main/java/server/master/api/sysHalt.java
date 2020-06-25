package server.master.api;

import common.KeyValuePair;
import common.Message;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface sysHalt extends Remote, Serializable {

    public Message halt() throws RemoteException;

}
