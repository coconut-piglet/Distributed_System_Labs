package server.storage.instance;

import server.storage.kvStorage;

public class kvStorage03 {
    public static void main(String[] argv) {
        kvStorage instance = new kvStorage("Storage-03", 10003, true, "Storage-01", 1);
        instance.run();
    }
}
