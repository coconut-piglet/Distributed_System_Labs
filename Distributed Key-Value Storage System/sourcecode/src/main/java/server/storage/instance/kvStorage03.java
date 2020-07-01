package server.storage.instance;

import server.storage.kvStorage;

public class kvStorage03 {
    public static void main(String[] argv) {
        kvStorage instance = new kvStorage("Storage-01", 10003, true, 1);
        instance.run();
    }
}
