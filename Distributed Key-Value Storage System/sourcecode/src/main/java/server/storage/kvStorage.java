package server.storage;

import server.storage.implementation.sysGetImpl;

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
 *   [ ] support zookeeper
 *   [ ] upgrade to journaling storage
 *   [ ] upgrade to persistent storage
 */
public class kvStorage {

    private static HashMap<String, String> storage;

    private static String hostAddress;

    private static int port;

    private static void printMessage(String msg) {
        System.out.print("kvServer: " + msg);
    }

    private static void printMessageln(String msg) {
        System.out.println("kvServer: " + msg);
    }

    private static String constructName(String service) {
        return "//" + hostAddress + ":" + port + "/" + service;
    }

    private static void devFakeData() {
        storage.put("se347", "distributed system");
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

        printMessageln("initializing service");

        printMessage("loading storage...");
        storage = new HashMap<String, String>();
        System.out.println("done");

        /* for development purpose, add fake data to storage */
        devFakeData();

        try {
            /* print ip address information */
            InetAddress inetAddress = Inet4Address.getLocalHost();
            hostAddress = inetAddress.getHostAddress();
            printMessageln("current ip address..." + hostAddress);

            /* start RMI registry on random port */
            int maxPortNum = 20000, minPortNum = 10000;
            port = (int) (Math.random() * (maxPortNum - minPortNum) + minPortNum);

            /* for development purpose, port is set manually to 10000 */
            port = 10000;

            printMessage("launch RMI registry on port " + port + "...");
            Registry registry = LocateRegistry.createRegistry(port);
            System.out.println("done");

            /* bind sysGet service */
            printMessage("binding GET service...");
            sysGetImpl sysGet = new sysGetImpl();
            Naming.rebind(constructName("sysGet"), sysGet);
            System.out.println("done");

            printMessageln("service initialized");

            /* PLACEHOLDER for routine */

            //printMessageln("shutting down");

            /* unbind sysGet service */
            //printMessage("unbinding GET service...");
            //Naming.unbind(constructName("sysGet"));
            //UnicastRemoteObject.unexportObject(sysGet, true);
            //System.out.println("done");

            /* stop RMI registry */
            //printMessage("closing RMI registry...");
            //UnicastRemoteObject.unexportObject(registry, true);
            //System.out.println("done");

        } catch (Exception e) {
            System.out.println("failed");
            e.printStackTrace();
            return;
        }
        //printMessageln("goodbye");
    }

    public static String getValue(String key) {
        return storage.getOrDefault(key, null);
    }

    public static void putValue(String key, String value) {
        storage.put(key, value);
    }
}
