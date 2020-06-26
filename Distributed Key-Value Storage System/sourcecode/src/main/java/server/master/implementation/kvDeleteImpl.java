package server.master.implementation;

import common.KeyValuePair;
import common.Message;
import server.master.api.kvDelete;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * DELETE service
 * TODO:
 *   [√] implement basic logic
 *   [ ] contact with storage server
 */
public class kvDeleteImpl extends UnicastRemoteObject implements kvDelete {

    public kvDeleteImpl() throws RemoteException {
    }

    @Override
    public Message delete(KeyValuePair keyValuePair) throws RemoteException {

        /* for now returns SUCCESS by default */
        /* TODO: update here after storage is ready */
        return new Message("SUCCESS","OK");

    }
}
