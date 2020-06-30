package common;

public class Node {

    private final String alias;

    private final String address;

    private final int port;

    private final boolean isReplica;

    private double utilization;

    public String getAlias() {
        return alias;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public boolean isReplica() {
        return isReplica;
    }

    public double getUtilization() {
        return this.utilization;
    }

    public void setUtilization(double utilization) {
        this.utilization = utilization;
    }

    public Node(String alias, String address, int port, boolean isReplica) {
        this.alias = alias;
        this.address = address;
        this.port = port;
        this.isReplica = isReplica;
    }
}
