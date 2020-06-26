package server.master.implementation;

import common.Message;
import server.master.api.kvRead;
import server.storage.api.sysGet;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * READ service
 * TODO:
 *   [√] implement basic logic
 *   [√] contact with storage server
 *   [ ] remove hard coded kvStorage
 */
public class kvReadImpl extends UnicastRemoteObject implements kvRead {

    public kvReadImpl() throws RemoteException {
    }

    @Override
    public Message read(String key) throws RemoteException {

        try {
            /* for now information about kvStorage is hard coded */
            /* TODO: get kvStorage server lists from kvMaster */
            sysGet getService = (sysGet) Naming.lookup("//192.168.31.167:10000/sysGet");
            String value = getService.get(key).getValue();
            if (value == null)
                return new Message("NOTFOUND", "no value has been recorded for this key");
            else {
                return new Message("SUCCESS", value);
            }
        } catch (Exception e) {
            return new Message("ERROR", "internal error, failed to connect to kvStorage");
        }
    }
}
