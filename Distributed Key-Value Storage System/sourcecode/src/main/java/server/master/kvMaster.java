package server.master;

import server.master.implementation.*;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/*
 * Master Server of Distributed Key-Value Storage System
 * TODO:
 *   [√] add RPC support for client
 *   [ ] add RPC to storage server
 *   [ ] add zookeeper
 *   [ ] add node management
 */
public class kvMaster {

    private static boolean powerOn;

    private static void printMessage(String msg) {
        System.out.print("kvServer: " + msg);
    }

    private static void printMessageln(String msg) {
        System.out.println("kvServer: " + msg);
    }

    public static void main(String[] argv) {
        System.out.println(".d8888. d88888b d8888b.   j88D  d88888D      db   dD db    db .d8888. d88888b d8888b. db    db d88888b d8888b. ");
        System.out.println("88'  YP 88'     VP  `8D  j8~88  VP  d8'      88 ,8P' 88    88 88'  YP 88'     88  `8D 88    88 88'     88  `8D ");
        System.out.println("`8bo.   88ooooo   oooY' j8' 88     d8'       88,8P   Y8    8P `8bo.   88ooooo 88oobY' Y8    8P 88ooooo 88oobY' ");
        System.out.println("  `Y8b. 88~~~~~   ~~~b. V88888D   d8'        88`8b   `8b  d8'   `Y8b. 88~~~~~ 88`8b   `8b  d8' 88~~~~~ 88`8b   ");
        System.out.println("db   8D 88.     db   8D     88   d8'         88 `88.  `8bd8'  db   8D 88.     88 `88.  `8bd8'  88.     88 `88. ");
        System.out.println("`8888Y' Y88888P Y8888P'     VP  d8'          YP   YD    YP    `8888Y' Y88888P 88   YD    YP    Y88888P 88   YD ");
        System.out.println("_______________________________________________________________________________________________________________");
        System.out.println("Welcome To Distributed Key-Value Storage System By YUEQI ZHAO");

        powerOn = true;

        printMessageln("initializing service");
        try {
            /* start RMI registry on the default port */
            printMessage("launch RMI registry...");
            Registry registry = LocateRegistry.createRegistry(1099);
            System.out.println("done");

            /* bind PUT service */
            printMessage("binding PUT service...");
            kvPutImpl kvPut = new kvPutImpl();
            Naming.rebind("kvPut", kvPut);
            System.out.println("done");

            /* bind UPDATE service */
            printMessage("binding UPDATE service...");
            kvUpdateImpl kvUpdate = new kvUpdateImpl();
            Naming.rebind("kvUpdate", kvUpdate);
            System.out.println("done");

            /* bind READ service */
            printMessage("binding READ service...");
            kvReadImpl kvRead = new kvReadImpl();
            Naming.rebind("kvRead", kvRead);
            System.out.println("done");

            /* bind DELETE service */
            printMessage("binding DELETE service...");
            kvDeleteImpl kvDelete = new kvDeleteImpl();
            Naming.rebind("kvDelete", kvDelete);
            System.out.println("done");

            /* bind HALT service */
            printMessage("binding HALT service...");
            sysHaltImpl sysHalt = new sysHaltImpl();
            Naming.rebind("sysHalt", sysHalt);
            System.out.println("done");

            printMessageln("service initialized");

            while (powerOn) {
                /* TODO: add node management routine */
                printMessageln("running routine");
            }

            printMessageln("shutting down");

            /* unbind PUT service */
            printMessage("unbinding PUT service...");
            Naming.unbind("kvPut");
            UnicastRemoteObject.unexportObject(kvPut, true);
            System.out.println("done");

            /* unbind UPDATE service */
            printMessage("unbinding UPDATE service...");
            Naming.unbind("kvUpdate");
            UnicastRemoteObject.unexportObject(kvUpdate, true);
            System.out.println("done");

            /* unbind UPDATE service */
            printMessage("unbinding READ service...");
            Naming.unbind("kvRead");
            UnicastRemoteObject.unexportObject(kvRead, true);
            System.out.println("done");

            /* unbind UPDATE service */
            printMessage("unbinding DELETE service...");
            Naming.unbind("kvDelete");
            UnicastRemoteObject.unexportObject(kvDelete, true);
            System.out.println("done");

            /* unbind HALT service */
            printMessage("unbinding HALT service...");
            Naming.unbind("sysHalt");
            UnicastRemoteObject.unexportObject(sysHalt, true);
            System.out.println("done");

            /* stop RMI registry */
            printMessage("closing RMI registry...");
            UnicastRemoteObject.unexportObject(registry, true);
            System.out.println("done");

        } catch (Exception e) {
            System.out.println("failed");
            e.printStackTrace();
            return;
        }
        printMessageln("goodbye");
    }

    private static boolean getPowerStat() {
        return powerOn;
    }

    public static void shutdown() {
        printMessageln("received shutdown command");
        powerOn = false;
    }
}
