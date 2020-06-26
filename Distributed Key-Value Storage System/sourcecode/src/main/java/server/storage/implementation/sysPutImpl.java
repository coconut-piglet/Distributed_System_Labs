package server.storage.implementation;

import server.storage.api.sysPut;
import server.storage.kvStorage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * sysPUT service
 * TODO:
 *   [√] implement basic logic
 */
public class sysPutImpl extends UnicastRemoteObject implements sysPut {

    public sysPutImpl() throws RemoteException {
    }

    @Override
    public void put(String key, String value) throws RemoteException {

        kvStorage.putValue(key, value);

    }

}
