package client;

import common.KeyValuePair;
import common.Message;
import server.master.api.kvPut;

import java.nio.charset.MalformedInputException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

/*
 * Client of Distributed Key-Value Storage System
 * TODO:
 *   [√] add user interface
 *   [√] support command NULL
 *   [√] support command INVALID
 *   [√] support command HELP
 *   [√] support command EXIT
 *   [√] support PUT RPC
 *   [ ] support READ RPC
 *   [ ] support DELETE RPC
 *   [ ] support UPDATE RPC
 *   [√] support command PUT
 *   [√] support command READ
 *   [√] support command DELETE
 */
public class kvClient {
    private static void printUsage() {
        System.out.println("usage:");
        System.out.println("\thelp               display helper information");
        System.out.println("\tput [key] [value]  create or update data");
        System.out.println("\tread [key]         read data from cluster");
        System.out.println("\tdelete [key]       delete data from cluster");
        System.out.println("\texit               exit kvClient");
    }

    private static void printMessage(String msg) {
        System.out.println("kvClient: " + msg);
    }

    public static void main(String[] argv) {
        /* print information */
        System.out.println(".d8888. d88888b d8888b.   j88D  d88888D      db   dD db    db  .o88b. db      d888888b d88888b d8b   db d888888b ");
        System.out.println("88'  YP 88'     VP  `8D  j8~88  VP  d8'      88 ,8P' 88    88 d8P  Y8 88        `88'   88'     888o  88 `~~88~~' ");
        System.out.println("`8bo.   88ooooo   oooY' j8' 88     d8'       88,8P   Y8    8P 8P      88         88    88ooooo 88V8o 88    88    ");
        System.out.println("  `Y8b. 88~~~~~   ~~~b. V88888D   d8'        88`8b   `8b  d8' 8b      88         88    88~~~~~ 88 V8o88    88    ");
        System.out.println("db   8D 88.     db   8D     88   d8'         88 `88.  `8bd8'  Y8b  d8 88booo.   .88.   88.     88  V888    88    ");
        System.out.println("`8888Y' Y88888P Y8888P'     VP  d8'          YP   YD    YP     `Y88P' Y88888P Y888888P Y88888P VP   V8P    YP    ");
        System.out.println("_________________________________________________________________________________________________________________");
        System.out.println("Welcome To Distributed Key-Value Storage System By YUEQI ZHAO");

        /* start routine */
        while (true) {
            System.out.print("command > ");
            Scanner scanner = new Scanner(System.in);
            String cmd = scanner.nextLine().trim();
            String args[] = cmd.split(" ");

            /* empty input or HELP */
            if (cmd.equals("") || args[0].equals("help")) {

                /* display how to use this program */
                printUsage();

            }

            /* PUT operation */
            else if (args[0].equals("put")) {

                /* check whether cmd matches put [key] [value] */
                if (args.length != 3) {
                    printMessage("invalid arguments, see 'help' for usage");
                    continue;
                }

                /* parse arguments */
                String key = args[1];
                String value = args[2];

                /* create new KeyValuePair object */
                KeyValuePair keyValuePair = new KeyValuePair(key, value);

                try {
                    /* call PUT via RPC */
                    kvPut put = (kvPut) Naming.lookup("kvPut");
                    Message retMsg = put.put(new KeyValuePair("key", "value"));

                    /* SUCCESS: a new key/value pair has been stored */
                    if (retMsg.getType().equals("SUCCESS")) {
                        printMessage("ok");
                    }
                    /* WARNING: the key has been stored before */
                    else if (retMsg.getType().equals("WARNING")) {

                        /* let the user choose whether to overwrite or not */
                        printMessage("conflict with existing key/value pair");
                        printMessage("previously stored value '" + retMsg.getContent() + "'");
                        System.out.print("kvClient: do you want to overwrite it ? (Y/n) ");
                        Scanner confirm = new Scanner(System.in);
                        String confirmation = confirm.nextLine().trim();

                        if (confirmation.equals("Y")) {
                            /* PLACEHOLDER for RPC */
                        }
                        else {
                            printMessage("operation abort");
                        }
                    }
                    /* ERROR: something went wrong on the server side */
                    else {
                        printMessage(retMsg.getContent());
                        printMessage("operation failed");
                    }
                } catch (Exception e) {
                    printMessage("sorry, something went wrong");
                    e.printStackTrace();
                }
            }

            /* READ operation */
            else if (args[0].equals("read")) {

                /* check whether cmd matches read [key] */
                if (args.length != 2) {
                    printMessage("invalid arguments, see 'help' for usage");
                }

                /* parse arguments */
                String key = args[1];

                /* PLACEHOLDER for RPC */

                printMessage("retVal");
            }

            /* DELETE operation */
            else if (args[0].equals("delete")) {

                /* check whether cmd matches delete [key] */
                if (args.length != 2) {
                    printMessage("invalid arguments, see 'help' for usage");
                }

                /* parse arguments */
                String key = args[1];

                /* PLACEHOLDER for RPC */

                printMessage("ok");
            }

            /* EXIT operatoin */
            else if (args[0].equals("exit")) {

                /* say goodbye and end looping */
                printMessage("goodbye");
                break;

            }

            /* INVALID operation */
            else {

                /* alert and ask user to see usage (git style) */
                printMessage("'" + args[0] + "' is not a kvClient command, see 'help' for usage");

            }
        }
    }
}