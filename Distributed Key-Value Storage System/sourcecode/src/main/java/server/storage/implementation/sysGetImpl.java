package server.storage.implementation;

import common.KeyValuePair;
import server.storage.api.sysGet;
import server.storage.kvStorage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * sysGET service
 * TODO:
 *   [√] implement basic logic
 */
public class sysGetImpl  extends UnicastRemoteObject implements sysGet {

    public sysGetImpl() throws RemoteException {
    }

    @Override
    public KeyValuePair get(KeyValuePair keyValuePair) throws RemoteException {

        String key = keyValuePair.getKey();
        String value = kvStorage.getValue(key);
        return new KeyValuePair(key, value);

    }

}
