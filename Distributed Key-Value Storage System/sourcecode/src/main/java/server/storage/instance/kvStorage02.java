package server.storage.instance;

import server.storage.kvStorage;

public class kvStorage02 {
    public static void main(String[] argv) {
        kvStorage instance = new kvStorage("Storage-00", 10002, true, 1);
        instance.run();
    }
}
