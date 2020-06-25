package server.master.implementation;

import common.KeyValuePair;
import common.Message;
import server.master.api.kvUpdate;

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

        /* for now returns SUCCESS by default */
        /* TODO: update here after storage is ready */
        return new Message("SUCCESS","OK");

    }
}
