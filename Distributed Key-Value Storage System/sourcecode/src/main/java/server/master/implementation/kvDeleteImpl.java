package server.master.implementation;

import common.KeyValuePair;
import common.Message;
import server.master.api.kvDelete;
import server.master.kvMaster;
import server.storage.api.sysPut;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * DELETE service
 * TODO:
 *   [√] implement basic logic
 *   [√] contact with storage server
 *   [√] implement concurrency control
 *   [√] remove hard coded kvStorage
 */
public class kvDeleteImpl extends UnicastRemoteObject implements kvDelete {

    public kvDeleteImpl() throws RemoteException {
    }

    @Override
    public Message delete(String key) throws RemoteException {

        kvMaster.lockWrite(key);
        try {
            String host = kvMaster.whereIsKey(key);
            if (host == null) {
                /* do not forget to unlock before return */
                kvMaster.unlockWrite(key);
                return new Message("SUCCESS","OK");
            }
            sysPut putService = (sysPut) Naming.lookup(host + "sysPut");
            putService.put(new KeyValuePair(key, null));
            kvMaster.unlockWrite(key);
            kvMaster.removeHostCache(key);
            return new Message("SUCCESS","OK");
        } catch (Exception e) {
            kvMaster.unlockWrite(key);
            /* in this case, if the cache still contains current key, it should be removed */
            kvMaster.removeHostCache(key);
            return new Message("ERROR", "internal error, failed to connect to kvStorage");
        }

    }
}
