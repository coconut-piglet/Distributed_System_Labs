package server.storage.instance;

import server.storage.kvStorage;

public class kvStorage00 {
    public static void main(String[] argv) {
        kvStorage instance = new kvStorage("Storage-00", 10000, false, null, 1);
        instance.run();
    }
}