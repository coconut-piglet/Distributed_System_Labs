package server.storage.implementation;

import server.storage.api.sysShutdown;
import server.storage.kvStorage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class sysShutdownImpl extends UnicastRemoteObject implements sysShutdown {

    public sysShutdownImpl() throws RemoteException {
    }

    @Override
    public void shutdown() throws RemoteException {
        kvStorage.shutdown();
    }

}
