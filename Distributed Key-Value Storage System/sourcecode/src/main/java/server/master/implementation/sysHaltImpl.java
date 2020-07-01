package server.master.implementation;

import common.Message;
import server.master.api.sysHalt;
import server.master.kvMaster;
import server.storage.api.sysShutdown;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * HALT service
 * TODO:
 *   [√] implement basic logic
 *   [√] contact with master server
 *   [√] implement concurrency control
 */
public class sysHaltImpl extends UnicastRemoteObject implements sysHalt {

    public sysHaltImpl() throws RemoteException {
    }

    @Override
    public Message halt() throws RemoteException {

        kvMaster.lockSystem();
        try {
            /* for now information about kvStorage is hard coded */
            /* TODO: get kvStorage server lists from kvMaster */
            sysShutdown powerService = (sysShutdown) Naming.lookup("//192.168.31.168:10000/sysShutdown");
            powerService.shutdown();
        } catch (Exception e) {
            kvMaster.unlockSystem();
            return new Message("ERROR", "internal error, failed to connect to kvStorage");
        }

        kvMaster.shutdown();
        kvMaster.unlockSystem();

        return new Message("SUCCESS", "start shutting down");
    }
}
