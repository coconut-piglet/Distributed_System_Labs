package server.master.implementation;

import common.Message;
import server.master.api.sysHalt;
import server.master.kvMaster;
import server.storage.api.sysShutdown;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/*
 * HALT service
 * TODO:
 *   [√] implement basic logic
 *   [√] contact with master server
 *   [√] implement concurrency control
 *   [√] remove hard coded kvStorage
 */
public class sysHaltImpl extends UnicastRemoteObject implements sysHalt {

    public sysHaltImpl() throws RemoteException {
    }

    @Override
    public Message halt() throws RemoteException {

        kvMaster.lockSystem();
        List<String> hosts = kvMaster.getAllStorageHost();
        if (hosts.size() != 0) {
            for (String host : hosts) {
                try {
                    sysShutdown powerService = (sysShutdown) Naming.lookup(host + "sysShutdown");
                    powerService.shutdown();
                } catch (Exception e) {
                    /* just ignore */
                }
            }
        }

        kvMaster.shutdown();
        kvMaster.unlockSystem();

        return new Message("SUCCESS", "start shutting down");
    }
}
