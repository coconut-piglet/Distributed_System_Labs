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
 *   [ ] remove hard coded kvStorage
 */
public class kvDeleteImpl extends UnicastRemoteObject implements kvDelete {

    public kvDeleteImpl() throws RemoteException {
    }

    @Override
    public Message delete(String key) throws RemoteException {

        kvMaster.lockWrite(key);
        try {
            /* for now information about kvStorage is hard coded */
            /* TODO: get kvStorage server lists from kvMaster */
            sysPut putService = (sysPut) Naming.lookup("//192.168.31.167:10000/sysPut");
            putService.put(new KeyValuePair(key, null));
            kvMaster.unlockWrite(key);
            return new Message("SUCCESS","OK");
        } catch (Exception e) {
            kvMaster.unlockWrite(key);
            return new Message("ERROR", "internal error, failed to connect to kvStorage");
        }

    }
}
