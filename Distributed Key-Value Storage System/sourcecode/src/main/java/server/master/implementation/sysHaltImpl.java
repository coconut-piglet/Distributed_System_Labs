package server.master.implementation;

import common.Message;
import server.master.api.sysHalt;
import server.master.kvMaster;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * HALT service
 * TODO:
 *   [√] implement basic logic
 *   [√] contact with master server
 */
public class sysHaltImpl extends UnicastRemoteObject implements sysHalt {

    public sysHaltImpl() throws RemoteException {
    }

    @Override
    public Message halt() throws RemoteException {
        kvMaster.shutdown();
        return new Message("SUCCESS", "start shutting down");
    }
}
