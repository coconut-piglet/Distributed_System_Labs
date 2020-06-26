package server.master.implementation;

import common.KeyValuePair;
import common.Message;
import server.master.api.kvUpdate;
import server.storage.api.sysPut;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * UPDATE service
 * TODO:
 *   [√] implement basic logic
 *   [ ] contact with storage server
 */
public class kvUpdateImpl extends UnicastRemoteObject implements kvUpdate {

    public kvUpdateImpl() throws RemoteException {
    }

    @Override
    public Message update(KeyValuePair keyValuePair) throws RemoteException {

        String key = keyValuePair.getKey();
        String value = keyValuePair.getValue();

        try {
            /* for now information about kvStorage is hard coded */
            /* TODO: get kvStorage server lists from kvMaster */
            sysPut putService = (sysPut) Naming.lookup("//192.168.31.167:10000/sysPut");
            putService.put(key, value);
            return new Message("SUCCESS","OK");
        } catch (Exception e) {
            e.printStackTrace();
            return new Message("ERROR", "internal error, failed to connect to kvStorage");
        }

    }
}
