package server.master.zookeeper;

import common.Node;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import server.master.kvMaster;

import java.util.ArrayList;
import java.util.List;

public class nodeExecutor implements Watcher, Runnable, nodeMonitor.nodeMonitorListener {

    ZooKeeper zooKeeper;

    nodeMonitor monitor;

    String znode;

    String host;

    public nodeExecutor(String host, String znode) throws Exception {
        this.znode = znode;
        this.host = host;
        this.zooKeeper = new ZooKeeper(host, 5000, this);
        this.monitor = new nodeMonitor(zooKeeper, znode, null, this);
    }

    public void run() {
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        monitor.process(watchedEvent);
    }

    @Override
    public void handleChanged(List<String> nodesToAdd, List<String> nodesToRemove) {
        new updateNodeList(nodesToAdd, nodesToRemove, zooKeeper, znode, host);
    }

    @Override
    public void handleClosing(int rc) {
        /* do nothing here for now */
    }

    static class updateNodeList extends Thread {
        List<String> nodesToAdd;
        List<String> nodesToRemove;
        ZooKeeper zooKeeper;
        String znode;
        String zkHost;

        public updateNodeList(List<String> nodesToAdd, List<String> nodesToRemove, ZooKeeper zooKeeper, String znode, String zkHost) {
            this.nodesToAdd = nodesToAdd;
            this.nodesToRemove = nodesToRemove;
            this.zooKeeper = zooKeeper;
            this.znode = znode;
            this.zkHost = zkHost;
            start();
        }

        private void printMessageln(String msg) {
            System.out.println("nodeExecutor: " + msg);
        }

        private String constructPath(String path) {
            return znode + "/" + path;
        }

        public void run() {
            printMessageln("<--------incoming change-------->");
            printMessageln("<<<<<<<< nodes to add <<<<<<<<<<<");
            if (nodesToAdd.size() > 0)
                nodesToAdd.forEach(this::printMessageln);
            else
                printMessageln("empty");
            printMessageln(">>>>>>>> nodes to remove >>>>>>>>");
            if (nodesToRemove.size() > 0)
                nodesToRemove.forEach(this::printMessageln);
            else
                printMessageln("empty");
            printMessageln("<--------incoming change-------->");
            List<Node> nodesToAddList = new ArrayList<>();
            if (nodesToRemove.size() > 0) {
                kvMaster.removeExistingNodes(nodesToRemove);
            }
            if (nodesToAdd.size() > 0) {
                nodesToAdd.forEach(nodePath -> {
                    try {
                        byte[] nodeData = zooKeeper.getData(constructPath(nodePath), false, null);
                        Node newNode = SerializationUtils.deserialize(nodeData);
                        newNode.setZkPath(nodePath);
                        nodesToAddList.add(newNode);
                    } catch (Exception e) {
                        printMessageln("failed to fetch node data");
                    }
                });
                printMessageln("number of newly fetched nodes: " + nodesToAddList.size());
                kvMaster.addAvailableNodes(nodesToAddList);
                nodesToAdd.forEach(nodePath -> {
                    try {
                        dataExecutor executor = new dataExecutor(zkHost, constructPath(nodePath));
                        executor.run();
                    } catch (Exception e) {
                        printMessageln("failed to set executor for node");
                    }
                });
            }
        }
    }
}
