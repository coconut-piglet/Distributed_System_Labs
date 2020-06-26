package server.master.implementation;

import common.KeyValuePair;
import common.Message;
import server.master.api.kvRead;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * READ service
 * TODO:
 *   [√] implement basic logic
 *   [ ] contact with storage server
 */
public class kvReadImpl extends UnicastRemoteObject implements kvRead {

    public kvReadImpl() throws RemoteException {
    }

    @Override
    public Message read(KeyValuePair keyValuePair) throws RemoteException {

        /* for now returns SUCCESS by default */
        /* TODO: update here after storage is ready */
        return new Message("SUCCESS","OK");

    }
}
