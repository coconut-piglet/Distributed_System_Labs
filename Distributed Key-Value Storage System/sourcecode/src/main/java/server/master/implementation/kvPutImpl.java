package server.master.implementation;

import common.KeyValuePair;
import common.Message;
import server.master.api.kvPut;
import server.storage.api.sysGet;
import server.storage.api.sysPut;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * PUT service
 * TODO:
 *   [√] implement basic logic
 *   [√] contact with storage server
 *   [ ] remove hard coded kvStorage
 */
public class kvPutImpl extends UnicastRemoteObject implements kvPut {

    public kvPutImpl() throws RemoteException {
    }

    @Override
    public Message put(KeyValuePair keyValuePair) throws RemoteException {

        String key = keyValuePair.getKey();

        /* check whether the key has been stored before */
        Message checkResult = checkExistence(key);

        /* if existed, send a warning to the client */
        if (checkResult.getType().equals("EXISTED")) {
            return new Message("WARNING", checkResult.getContent());
        }
        /* otherwise continue creating new key/value pair */
        else {
            return putData(keyValuePair);
        }
    }

    @Override
    public Message update(KeyValuePair keyValuePair) throws RemoteException {
        return putData(keyValuePair);
    }

    /* check whether the key provided is already in the database */
    private Message checkExistence (String key) {

        try {
            /* for now information about kvStorage is hard coded */
            /* TODO: get kvStorage server lists from kvMaster */
            sysGet getService = (sysGet) Naming.lookup("//192.168.31.167:10000/sysGet");
            String value = getService.get(key).getValue();
            if (value == null)
                return new Message("PASS", "this key has not value recorded");
            else {
                return new Message("EXISTED", value);
            }
        } catch (Exception e) {
            return new Message("ERROR", "internal error, failed to connect to kvStorage");
        }

    }

    /* insert new key/value pair to the database */
    private Message putData (KeyValuePair keyValuePair) {

        try {
            /* for now information about kvStorage is hard coded */
            /* TODO: get kvStorage server lists from kvMaster */
            sysPut putService = (sysPut) Naming.lookup("//192.168.31.167:10000/sysPut");
            putService.put(keyValuePair);
            return new Message("SUCCESS","OK");
        } catch (Exception e) {
            return new Message("ERROR", "internal error, failed to connect to kvStorage");
        }

    }
}
