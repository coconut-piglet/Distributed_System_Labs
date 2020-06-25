package server.master.implementation;

import common.KeyValuePair;
import common.Message;
import server.master.api.kvPut;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * PUT service
 * TODO:
 *   [√] implement basic logic
 *   [ ] contact with storage server
 */
public class kvPutImpl extends UnicastRemoteObject implements kvPut {

    public kvPutImpl() throws RemoteException {
    }

    @Override
    public Message put(KeyValuePair keyValuePair) throws RemoteException {

        /* check whether the key has been stored before */
        Message checkResult = checkExistence(keyValuePair);

        /* if existed, send a warning to the client */
        if (checkResult.getType().equals("EXISTED")) {
            return new Message("WARNING", "existing key");
        }
        /* otherwise continue creating new key/value pair */
        else {
            return createData(keyValuePair);
        }
    }

    /* check whether the key provided is already in the database */
    private Message checkExistence (KeyValuePair keyValuePair) {

        /* for now returns INFO by default */
        /* TODO: update here after storage is ready */
        return new Message("EXISTED", "old value");

    }

    /* insert new key/value pair to the database */
    private Message createData (KeyValuePair keyValuePair) {

        /* for now returns SUCCESS by default */
        /* TODO: update here after storage is ready */
        return new Message("SUCCESS","OK");

    }
}
