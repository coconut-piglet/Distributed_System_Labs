package server.master.implementation;

import common.KeyValuePair;
import common.Message;
import server.master.api.kvPut;
import server.master.kvMaster;
import server.storage.api.sysGet;
import server.storage.api.sysPut;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/*
 * PUT service
 * TODO:
 *   [√] implement basic logic
 *   [√] contact with storage server
 *   [√] implement concurrency control
 *   [√] remove hard coded kvStorage
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
            return putData(keyValuePair, false);
        }
    }

    @Override
    public Message update(KeyValuePair keyValuePair) throws RemoteException {
        return putData(keyValuePair, true);
    }

    /* check whether the key provided is already in the database */
    private Message checkExistence (String key) {

        kvMaster.lockRead(key);
        try {
            String host = kvMaster.whereIsKey(key);
            if (host == null) {
                kvMaster.unlockRead(key);
                return new Message("PASS", "this key has no value recorded");
            }
            sysGet getService = (sysGet) Naming.lookup(host + "sysGet");
            String value = getService.get(key).getValue();
            kvMaster.unlockRead(key);
            if (value == null)
                return new Message("PASS", "this key has no value recorded");
            else {
                kvMaster.updateHostCache(key, host);
                return new Message("EXISTED", value);
            }
        } catch (Exception e) {
            kvMaster.unlockRead(key);
            kvMaster.removeHostCache(key);
            return new Message("ERROR", "internal error, failed to connect to kvStorage");
        }

    }

    /* insert new key/value pair to the database */
    private Message putData (KeyValuePair keyValuePair, boolean replace) {

        String key = keyValuePair.getKey();
        kvMaster.lockWrite(key);
        try {
            String host;
            if (replace) {
                host = kvMaster.whereIsKey(key);
                if (host == null) {
                    kvMaster.unlockWrite(key);
                    return new Message("ERROR", "key has been removed");
                }
            }
            else {
                host = kvMaster.getStorageHost();
                if (host == null) {
                    kvMaster.unlockWrite(key);
                    return new Message("ERROR", "kvStorage not available");
                }
            }

            sysPut putService = (sysPut) Naming.lookup(host + "sysPut");
            putService.put(keyValuePair);

            /* write the same data to all the replicas */
            List<String> replicas = kvMaster.getReplicas(host);
            for (String replica : replicas) {
                try {
                    sysPut putServiceReplica = (sysPut) Naming.lookup(replica + "sysPut");
                    putServiceReplica.put(keyValuePair);
                } catch (Exception e) {
                    /* do nothing here */
                }
            }

            kvMaster.unlockWrite(key);
            kvMaster.updateHostCache(key, host);
            return new Message("SUCCESS","OK");
        } catch (Exception e) {
            kvMaster.unlockWrite(key);
            kvMaster.removeHostCache(key);
            return new Message("ERROR", "internal error, failed to connect to kvStorage");
        }

    }
}
