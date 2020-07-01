package server.master.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;

public class nodeExecutor implements Watcher, Runnable, nodeMonitor.nodeMonitorListener {

    ZooKeeper zooKeeper;

    nodeMonitor monitor;

    public nodeExecutor(String host, String znode) throws Exception {
        zooKeeper = new ZooKeeper(host, 5000, this);
        monitor = new nodeMonitor(zooKeeper, znode, null, this);
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
    public void handleChanged(List<String> nodesToAdd, List<String> nodesToRemove) {
        new updateNodeList(nodesToAdd, nodesToRemove);
    }

    @Override
    public void handleClosing(int rc) {
        /* do nothing here for now */
    }

    static class updateNodeList extends Thread {
        List<String> nodesToAdd;
        List<String> nodesToRemove;

        public updateNodeList(List<String> nodesToAdd, List<String> nodesToRemove) {
            this.nodesToAdd = nodesToAdd;
            this.nodesToRemove = nodesToRemove;
            start();
        }

        private void printMessageln(String msg) {
            System.out.println("zooKeeper: " + msg);
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
        }
    }
}
