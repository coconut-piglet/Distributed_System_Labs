package common;

public class Node {

    private final String alias;

    private final String address;

    private final int port;

    private final String replica;

    private final int capacity;

    private static int usage = 0;

    public String getAlias() {
        return alias;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getReplica() {
        return replica;
    }

    public int getCapacity() {
        return capacity;
    }

    public static int getUsage() {
        return usage;
    }

    public static void setUsage(int usage) {
        Node.usage = usage;
    }

    public Node(String alias, String address, int port, String replica, int capacity) {
        this.alias = alias;
        this.address = address;
        this.port = port;
        this.replica = replica;
        this.capacity = capacity;
    }
}
