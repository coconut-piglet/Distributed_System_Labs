package common;

import java.io.Serializable;

public class Node implements Serializable {

    private final String alias;

    private final String address;

    private final int port;

    private final boolean isReplica;

    private final String master;

    private double utilization;

    private String zkPath;

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

    public String getMaster() {
        return master;
    }

    public double getUtilization() {
        return utilization;
    }

    public void setUtilization(double utilization) {
        this.utilization = utilization;
    }

    public String getZkPath() {
        return zkPath;
    }

    public void setZkPath(String zkPath) {
        this.zkPath = zkPath;
    }

    public Node(String alias, String address, int port, boolean isReplica, String master) {
        this.alias = alias;
        this.address = address;
        this.port = port;
        this.isReplica = isReplica;
        this.master = master;
    }
}
