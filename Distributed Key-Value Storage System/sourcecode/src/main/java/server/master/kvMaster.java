package server.master;

import server.master.implementation.*;
import server.master.zookeeper.nodeExecutor;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
 * Master Server of Distributed Key-Value Storage System
 * TODO:
 *   [√] add RPC support for client
 *   [√] add RPC to storage server
 *   [√] add mutex
 *   [ ] add zookeeper
 *   [ ] add node management
 */
public class kvMaster {

    private static boolean powerOn;

    private static String hostAddress;

    private static int hostPort;

    private static HashMap<String, ReentrantReadWriteLock> mutex;

    private static ReentrantReadWriteLock systemLock;

    private static void printMessage(String msg) {
        System.out.print("kvServer: " + msg);
    }

    private static void printMessageln(String msg) {
        System.out.println("kvServer: " + msg);
    }

    private static String constructName(String service) {
        return "//" + hostAddress + ":" + hostPort + "/" + service;
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

        printMessageln("initializing mutex...");
        systemLock = new ReentrantReadWriteLock();
        mutex = new HashMap<String, ReentrantReadWriteLock>();
        System.out.println("done");

        try {
            lockSystem();
            /* print ip address information */
            InetAddress inetAddress = Inet4Address.getLocalHost();
            hostAddress = inetAddress.getHostAddress();
            System.setProperty("java.rmi.server.hostname", hostAddress);
            printMessageln("current ip address..." + hostAddress);

            /* start RMI registry on the default port */
            printMessage("launch RMI registry...");
            hostPort = 1099;
            Registry registry = LocateRegistry.createRegistry(hostPort);
            System.out.println("done");

            /* bind PUT service */
            printMessage("binding PUT service...");
            kvPutImpl kvPut = new kvPutImpl();
            Naming.rebind(constructName("kvPut"), kvPut);
            System.out.println("done");

            /* bind READ service */
            printMessage("binding READ service...");
            kvReadImpl kvRead = new kvReadImpl();
            Naming.rebind(constructName("kvRead"), kvRead);
            System.out.println("done");

            /* bind DELETE service */
            printMessage("binding DELETE service...");
            kvDeleteImpl kvDelete = new kvDeleteImpl();
            Naming.rebind(constructName("kvDelete"), kvDelete);
            System.out.println("done");

            /* bind HALT service */
            printMessage("binding POWER service...");
            sysHaltImpl sysHalt = new sysHaltImpl();
            Naming.rebind(constructName("sysHalt"), sysHalt);
            System.out.println("done");

            /* start zookeeper node listener */
            nodeExecutor nodeExec = new nodeExecutor("127.0.0.1:2181", "/kvStorage");
            nodeExec.run();

            printMessageln("service initialized");
            unlockSystem();

            while (powerOn) {
                /* TODO: add node management routine */
                Thread.sleep(1000);
            }

            lockSystem();
            printMessageln("shutting down");

            /* unbind PUT service */
            printMessage("unbinding PUT service...");
            Naming.unbind(constructName("kvPut"));
            UnicastRemoteObject.unexportObject(kvPut, true);
            System.out.println("done");

            /* unbind UPDATE service */
            printMessage("unbinding READ service...");
            Naming.unbind(constructName("kvRead"));
            UnicastRemoteObject.unexportObject(kvRead, true);
            System.out.println("done");

            /* unbind UPDATE service */
            printMessage("unbinding DELETE service...");
            Naming.unbind(constructName("kvDelete"));
            UnicastRemoteObject.unexportObject(kvDelete, true);
            System.out.println("done");

            /* unbind HALT service */
            printMessage("unbinding POWER service...");
            Naming.unbind(constructName("sysHalt"));
            UnicastRemoteObject.unexportObject(sysHalt, true);
            System.out.println("done");

            /* stop RMI registry */
            printMessage("closing RMI registry...");
            UnicastRemoteObject.unexportObject(registry, true);
            System.out.println("done");
            unlockSystem();

        } catch (Exception e) {
            unlockSystem();
            System.out.println("failed");
            e.printStackTrace();
            return;
        }
        printMessageln("goodbye");
    }

    private static void createLockIfNotExists(String key) {
        /* acquire read lock first to check whether lock exists */
        systemLock.readLock().lock();

        /* if not exists */
        if (!mutex.containsKey(key)) {
            /* write lock should not be acquired when holding read lock */
            systemLock.readLock().unlock();

            /* critical session: these cold should execute atomically */
            systemLock.writeLock().lock();
            /* chances are that lock is created during unlock readLock and acquire writeLock */
            /* thus have to check for the second time */
            if (!mutex.containsKey(key)) {
                ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
                mutex.put(key, lock);
            }
            systemLock.writeLock().unlock();

        }
        /* if exits */
        else {
            systemLock.readLock().unlock();
        }
    }

    public static void lockRead(String key) {
        createLockIfNotExists(key);
        mutex.get(key).readLock().lock();
    }

    public static void lockWrite(String key) {
        createLockIfNotExists(key);
        mutex.get(key).writeLock().lock();
    }

    public static void lockSystem() {
        systemLock.writeLock().lock();
    }

    public static void unlockRead(String key) {
        mutex.get(key).readLock().unlock();
    }

    public static void unlockWrite(String key) {
        mutex.get(key).writeLock().unlock();
    }

    public static void unlockSystem() {
        systemLock.writeLock().unlock();
    }

    public static void shutdown() {
        powerOn = false;
    }
}
