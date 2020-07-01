package server.storage;

import common.Node;
import server.storage.implementation.sysGetImpl;
import server.storage.implementation.sysPutImpl;
import server.storage.implementation.sysShutdownImpl;
import server.storage.zookeeper.zkRegister;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

/*
 * Storage Server of Distributed Key-Value Storage System
 * TODO:
 *   [√] support storage in memory
 *   [√] support RPC
 *   [√] support zookeeper
 *   [ ] upgrade to journaling storage
 *   [ ] upgrade to persistent storage
 */
public class kvStorage {

    private static HashMap<String, String> storage;

    private static String hostAddress;

    private static int hostPort;

    private static boolean powerOn;

    private static void printMessage(String msg) {
        System.out.print("kvServer: " + msg);
    }

    private static void printMessageln(String msg) {
        System.out.println("kvServer: " + msg);
    }

    private static String constructName(String service) {
        return "//" + hostAddress + ":" + hostPort + "/" + service;
    }

    private static double roundNumber(double number, double precision) {
        double base = Math.pow(10, precision);
        return (double) Math.round(number * base) / base;
    }

    public static void main(String[] argv) {
        System.out.println(".d8888. d88888b d8888b.   j88D  d88888D      db   dD db    db .d8888. d888888b d8888b.  .d88b.   .d8b.   d888b  d88888b ");
        System.out.println("88'  YP 88'     VP  `8D  j8~88  VP  d8'      88 ,8P' 88    88 88'  YP `~~88~~' 88  `8D .8P  Y8. d8' `8b 88' Y8b 88'     ");
        System.out.println("`8bo.   88ooooo   oooY' j8' 88     d8'       88,8P   Y8    8P `8bo.      88    88oobY' 88    88 88ooo88 88      88ooooo ");
        System.out.println("  `Y8b. 88~~~~~   ~~~b. V88888D   d8'        88`8b   `8b  d8'   `Y8b.    88    88`8b   88    88 88~~~88 88  ooo 88~~~~~ ");
        System.out.println("db   8D 88.     db   8D     88   d8'         88 `88.  `8bd8'  db   8D    88    88 `88. `8b  d8' 88   88 88. ~8~ 88.     ");
        System.out.println("`8888Y' Y88888P Y8888P'     VP  d8'          YP   YD    YP    `8888Y'    YP    88   YD  `Y88P'  YP   YP  Y888P  Y88888P ");
        System.out.println("________________________________________________________________________________________________________________________");
        System.out.println("Welcome To Distributed Key-Value Storage System By YUEQI ZHAO");

        powerOn = true;
        int capacity = 100;

        printMessageln("initializing service");

        printMessage("loading storage...");
        storage = new HashMap<String, String>();
        System.out.println("done");

        try {
            /* print ip address information */
            InetAddress inetAddress = Inet4Address.getLocalHost();
            hostAddress = inetAddress.getHostAddress();
            printMessageln("current ip address..." + hostAddress);

            /* start RMI registry on random port */
            int maxPortNum = 20000, minPortNum = 10000;
            hostPort = (int) (Math.random() * (maxPortNum - minPortNum) + minPortNum);

            /* for development purpose, port is set manually to 10000 */
            hostPort = 10000;

            printMessage("launch RMI registry on port " + hostPort + "...");
            Registry registry = LocateRegistry.createRegistry(hostPort);
            System.out.println("done");

            /* bind sysGet service */
            printMessage("binding GET service...");
            sysGetImpl sysGet = new sysGetImpl();
            Naming.rebind(constructName("sysGet"), sysGet);
            System.out.println("done");

            /* bind sysPut service */
            printMessage("binding PUT service...");
            sysPutImpl sysPut = new sysPutImpl();
            Naming.rebind(constructName("sysPut"), sysPut);
            System.out.println("done");

            /* bind sysPut service */
            printMessage("binding POWER service...");
            sysShutdownImpl sysShutdown = new sysShutdownImpl();
            Naming.rebind(constructName("sysShutdown"), sysShutdown);
            System.out.println("done");

            printMessageln("service initialized");

            /* register to zookeeper cluster */
            printMessageln("registering server...");

            /* prepare node information */
            printMessage("preparing node information...");
            Node node = new Node("kvStorage-00", hostAddress, hostPort, false);
            node.setUtilization(0);
            System.out.println("done");

            /* connect to zookeeper */
            printMessage("connecting to zookeeper...");
            zkRegister zk = new zkRegister("127.0.0.1:2181", node);
            zk.run();
            System.out.println("done");
            printMessage("zookeeper path...");
            System.out.println(zk.getMyPath());

            printMessageln("server registered");

            double prev = 0;
            while (powerOn) {
                double crnt = (double) storage.size() / (double) capacity;
                crnt = roundNumber(crnt, 1);
                if (crnt != prev) {
                    printMessageln("update utilization info");
                    node.setUtilization(crnt);
                    try {
                        zk.updateNodeData(node);
                    } catch (Exception e) {
                        printMessageln("failed to update metadata");
                    }
                    prev = crnt;
                }
                Thread.sleep(1000);
            }

            printMessageln("shutting down");

            /* disconnect from zookeeper */
            printMessage("disconnecting from zookeeper...");
            zk.disconnect();
            System.out.println("done");

            /* unbind sysGet service */
            printMessage("unbinding GET service...");
            Naming.unbind(constructName("sysGet"));
            UnicastRemoteObject.unexportObject(sysGet, true);
            System.out.println("done");

            /* unbind sysPut service */
            printMessage("unbinding PUT service...");
            Naming.unbind(constructName("sysPut"));
            UnicastRemoteObject.unexportObject(sysPut, true);
            System.out.println("done");

            /* unbind sysShutdown service */
            printMessage("unbinding POWER service...");
            Naming.unbind(constructName("sysShutdown"));
            UnicastRemoteObject.unexportObject(sysShutdown, true);
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

    public static String getValue(String key) {
        return storage.getOrDefault(key, null);
    }

    public static void putValue(String key, String value) {
        if (value == null)
            storage.remove(key);
        else
            storage.put(key, value);
    }

    public static void shutdown() {
        powerOn = false;
    }
}
