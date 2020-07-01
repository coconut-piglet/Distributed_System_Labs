package server.master.implementation;

import common.Message;
import server.master.api.kvRead;
import server.master.kvMaster;
import server.storage.api.sysGet;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * READ service
 * TODO:
 *   [√] implement basic logic
 *   [√] contact with storage server
 *   [√] implement concurrency control
 *   [ ] remove hard coded kvStorage
 */
public class kvReadImpl extends UnicastRemoteObject implements kvRead {

    public kvReadImpl() throws RemoteException {
    }

    @Override
    public Message read(String key) throws RemoteException {

        kvMaster.lockRead(key);
        try {
            /* for now information about kvStorage is hard coded */
            /* TODO: get kvStorage server lists from kvMaster */
            sysGet getService = (sysGet) Naming.lookup("//192.168.31.168:10000/sysGet");
            String value = getService.get(key).getValue();
            kvMaster.unlockRead(key);
            if (value == null)
                return new Message("NOTFOUND", "no value has been recorded for this key");
            else {
                return new Message("SUCCESS", value);
            }
        } catch (Exception e) {
            kvMaster.unlockRead(key);
            return new Message("ERROR", "internal error, failed to connect to kvStorage");
        }
    }
}
