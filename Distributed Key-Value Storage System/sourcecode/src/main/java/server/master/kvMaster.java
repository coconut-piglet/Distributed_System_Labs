package server.master;

import common.Node;
import server.master.implementation.*;
import server.master.zookeeper.nodeExecutor;
import server.storage.api.sysGet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
 * Master Server of Distributed Key-Value Storage System
 * TODO:
 *   [√] add RPC support for client
 *   [√] add RPC to storage server
 *   [√] add mutex
 *   [√] add zookeeper
 *   [√] add node management
 */
public class kvMaster {

    private static boolean powerOn;

    private static String hostAddress;

    private static int hostPort;

    private static HashMap<String, ReentrantReadWriteLock> mutex;

    private static HashMap<String, String> hostCache;

    private static List<String> cachedKeys;

    private static int hostCacheSize;

    private static List<Node> availableNodes;

    private static List<Node> backupNodes;

    private static ReentrantReadWriteLock systemLock;

    private static ReentrantReadWriteLock nodeLock;

    private static ReentrantReadWriteLock cacheLock;

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
        nodeLock = new ReentrantReadWriteLock();
        cacheLock = new ReentrantReadWriteLock();
        mutex = new HashMap<String, ReentrantReadWriteLock>();
        System.out.println("done");

        printMessageln("initializing cache...");
        hostCache = new HashMap<String, String>();
        cachedKeys = new LinkedList<String>();
        hostCacheSize = 5;
        System.out.println("done");

        printMessageln("initializing available nodes list...");
        availableNodes = new LinkedList<>();
        System.out.println("done");

        printMessageln("initializing backup nodes list...");
        backupNodes = new LinkedList<>();
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

    public static void shutdown() {
        powerOn = false;
    }

    /* <---------- reserved area for nodes ----------> */

    public static void updateNode(Node node) {
        lockWriteNode();
        printMessage("updating node information...");
        String target = node.getAlias();
        for (Node availableNode : availableNodes) {
            if (availableNode.getAlias().equals(target)) {
                availableNode.setUtilization(node.getUtilization());
                System.out.println("done");
                unlockWriteNode();
                return;
            }
        }
        for (Node backupNode : backupNodes) {
            if (backupNode.getAlias().equals(target)) {
                backupNode.setUtilization(node.getUtilization());
                System.out.println("done");
                unlockWriteNode();
                return;
            }
        }
        System.out.println("failed");
        unlockWriteNode();
    }

    public static void addAvailableNodes(List<Node> nodesToAdd) {
        lockWriteNode();
        printMessage("updating node information...");
        nodesToAdd.forEach(node -> {
            if (node.isReplica()) {
                backupNodes.add(node);
            }
            else {
                availableNodes.add(node);
            }
        });
        System.out.println("done");
        printMessageln("current available nodes: " + availableNodes.size());
        printMessageln("current backup nodes: " + backupNodes.size());
        unlockWriteNode();
    }

    public static void removeExistingNodes(List<String> nodesToRemove) {
        lockWriteNode();
        printMessage("updating node information...");
        nodesToRemove.forEach(path -> {
            boolean modified = false;
            for (int i = 0; i < availableNodes.size(); i++) {
                Node tmpNode = availableNodes.get(i);
                String tmpPath = tmpNode.getZkPath();

                if (tmpPath.equals(path)) {
                    String alias = tmpNode.getAlias();
                    for (int j = 0; j < backupNodes.size(); j++) {
                        Node bakNode = backupNodes.get(i);
                        String bakFor = bakNode.getMaster();
                        if (bakFor.equals(alias)) {
                            availableNodes.add(i, bakNode);
                            backupNodes.remove(bakNode);
                            break;
                        }
                    }
                    availableNodes.remove(tmpNode);
                    modified = true;
                    break;
                }
            }
            if (!modified) {
                for (int i = 0; i < backupNodes.size(); i++) {
                    if (backupNodes.get(i).getZkPath().equals(path)) {
                        backupNodes.remove(i);
                        break;
                    }
                }
            }
            if (!modified) {
                printMessageln("found a ghost node lol");
            }
        });
        System.out.println("done");
        printMessageln("current available nodes: " + availableNodes.size());
        printMessageln("current backup nodes: " + backupNodes.size());
        unlockWriteNode();
    }

    /* <---------- reserved area for hosts ----------> */

    public static List<String> getAllStorageHost() {
        lockReadNode();
        List<String> hosts = new ArrayList<>();
        for (Node node : availableNodes) {
            hosts.add(node.getAlias() + "=//" + node.getAddress() + ":" + node.getPort() + "/");
        }
        for (Node node : backupNodes) {
            hosts.add(node.getAlias() + "=//" + node.getAddress() + ":" + node.getPort() + "/");
        }
        unlockReadNode();
        return hosts;
    }

    public static List<String> getReplicas(String masterAlias) {
        List<String> replicas = new ArrayList<String>();
        lockReadNode();
        for (Node node : backupNodes) {
            if (node.getMaster().equals(masterAlias)) {
                replicas.add("//" + node.getAddress() + ":" + node.getPort() + "/");
            }
        }
        unlockReadNode();
        return replicas;
    }

    public static String getStorageHost() {
        String host = null;
        lockReadNode();
        if (availableNodes.size() > 0) {
            Node target = availableNodes.get(0);
            host = target.getAlias() + "=//" + target.getAddress() + ":" + target.getPort() + "/";
        }
        unlockReadNode();
        return host;
    }

    public static String whereIsKey(String key) {
        String host = null;
        lockCacheRead();
        if (cachedKeys.contains(key)) {
            host = hostCache.get(key);
            unlockCacheRead();
            return host;
        }
        unlockCacheRead();
        lockReadNode();
        for (Node node : availableNodes) {
            String target = "//" + node.getAddress() + ":" + node.getPort() + "/";
            try {
                sysGet getService = (sysGet) Naming.lookup(target + "sysGet");
                if (getService.ping(key)) {
                    host = node.getAlias() + "=" + target;
                    break;
                }
            } catch (Exception e) {
                printMessageln("node unavailable during global search");
            }
        }
        unlockReadNode();
        return host;
    }

    /* <---------- reserved area for cache ----------> */

    public static void removeHostCache(String key) {
        lockCacheWrite();
        hostCache.remove(key);
        cachedKeys.remove(key);
        unlockCacheWrite();
    }

    public static void updateHostCache(String key, String host) {
        lockCacheWrite();
        /* update existing cache or cache still free */
        if (cachedKeys.contains(key)) {
            hostCache.put(key, host);
            cachedKeys.remove(key);
            cachedKeys.add(cachedKeys.size(), key);
        }
        else if (hostCache.size() < hostCacheSize) {
            hostCache.put(key, host);
            cachedKeys.add(cachedKeys.size(), key);
        }
        else {
            /* insert new key */
            cachedKeys.add(hostCacheSize, key);
            hostCache.put(key, host);
            /* and evict the least recently used key */
            String evictedKey = cachedKeys.get(0);
            cachedKeys.remove(0);
            hostCache.remove(evictedKey);
        }
        unlockCacheWrite();
    }

    /* <---------- reserved area for locks ----------> */

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

    private static void lockReadNode() {nodeLock.readLock().lock();}

    private static void lockWriteNode() {nodeLock.writeLock().lock();}

    private static void lockCacheRead() {cacheLock.readLock().lock();}

    private static void lockCacheWrite() {cacheLock.writeLock().lock();}

    public static void unlockRead(String key) {
        mutex.get(key).readLock().unlock();
    }

    public static void unlockWrite(String key) {
        mutex.get(key).writeLock().unlock();
    }

    public static void unlockSystem() {
        systemLock.writeLock().unlock();
    }

    private static void unlockReadNode() {nodeLock.readLock().unlock();}

    private static void unlockWriteNode() {nodeLock.writeLock().unlock();}

    private static void unlockCacheRead() {cacheLock.readLock().unlock();}

    private static void unlockCacheWrite() {cacheLock.writeLock().unlock();}
}
