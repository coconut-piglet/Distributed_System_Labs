package server.storage.implementation;

import common.KeyValuePair;
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
    public void put(KeyValuePair keyValuePair) throws RemoteException {

        String key = keyValuePair.getKey();
        String value = keyValuePair.getValue();
        kvStorage.putValue(key, value);

    }

}
