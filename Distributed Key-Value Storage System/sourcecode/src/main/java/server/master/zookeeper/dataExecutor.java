package server.master.zookeeper;

import common.Node;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import server.master.kvMaster;

public class dataExecutor implements Watcher, Runnable, dataMonitor.dataMonitorListener {

    ZooKeeper zooKeeper;

    dataMonitor monitor;

    String znode;

    public dataExecutor(String host, String znode) throws Exception {
        //System.out.println("dataExecutor created for path: " + znode);
        this.znode = znode;
        this.zooKeeper = new ZooKeeper(host, 5000, this);
        this.monitor = new dataMonitor(zooKeeper, znode, null, this);
    }

    public void run() {
        /*
        try {
            synchronized (this) {
                while (!monitor.isDead) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
        }*/
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        monitor.process(watchedEvent);
    }

    @Override
    public void handleUpdate(byte[] nodeData) {
        new updataNode(nodeData);
    }

    @Override
    public void handleClosing(int rc) {
        System.out.println("dataExecutor closed for path: " + znode);
    }

    static class updataNode extends Thread {
        byte[] nodeData;

        public updataNode(byte[] nodeData) {
            this.nodeData = nodeData;
            start();
        }

        private void printMessageln(String msg) {
            System.out.println("dataExecutor: " + msg);
        }

        public void run() {
            Node node = SerializationUtils.deserialize(nodeData);
            printMessageln("<--------incoming change-------->");
            printMessageln("node '" + node.getAlias() + "' updated its utilization to " + node.getUtilization());
            printMessageln("<--------incoming change-------->");
            kvMaster.updateNode(node);
        }

    }
}
