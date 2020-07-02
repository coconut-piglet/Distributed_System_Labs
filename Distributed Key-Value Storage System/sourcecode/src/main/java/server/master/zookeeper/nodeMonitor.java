package server.master.zookeeper;

import org.apache.zookeeper.*;

import java.util.ArrayList;
import java.util.List;

public class nodeMonitor implements Watcher, AsyncCallback.ChildrenCallback {

    ZooKeeper zooKeeper;

    String znode;

    Watcher chainedWatcher;

    boolean isDead;

    nodeMonitorListener listener;

    List<String> prevNodeList;

    public interface nodeMonitorListener {
        /* status of nodes has changed */
        void handleChanged(List<String> nodesToAdd, List<String> nodesToRemove);

        /* the zookeeper session is no longer valid */
        void handleClosing (int rc);
    }

    public nodeMonitor(ZooKeeper zooKeeper, String znode, Watcher chainedWatcher, nodeMonitorListener listener) {
        this.zooKeeper = zooKeeper;
        this.znode = znode;
        this.chainedWatcher = chainedWatcher;
        this.listener = listener;
        this.prevNodeList = new ArrayList<String>();
        /* get things start by checking if the node exists */
        zooKeeper.getChildren(znode, true, this, null);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        String path = watchedEvent.getPath();
        if (watchedEvent.getType() == Event.EventType.None) {
            switch (watchedEvent.getState()) {
                case SyncConnected:
                    break;
                case Expired:
                    isDead = true;
                    listener.handleClosing(KeeperException.Code.SESSIONEXPIRED.intValue());
                    break;
            }
        }
        else {
            if (path != null && path.equals(znode)) {
                zooKeeper.getChildren(znode, true, this, null);
            }
        }
        if (chainedWatcher != null) {
            chainedWatcher.process(watchedEvent);
        }
    }

    @Override
    public void processResult(int i, String s, Object o, List<String> list) {
        boolean exists;
        switch (i) {
            case KeeperException.Code.Ok:
                exists = true;
                break;
            case KeeperException.Code.NoNode:
                exists = false;
                break;
            case KeeperException.Code.SessionExpired:
            case KeeperException.Code.NoAuth:
                isDead = true;
                listener.handleClosing(i);
                return;
            default:
                zooKeeper.getChildren(znode, true, this, null);
                return;
        }

        List<String> currNodeList = new ArrayList<>();

        if (exists) {
            try {
                currNodeList = zooKeeper.getChildren(znode, null);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                return;
            }
        }

        printMessageln("<---------previous status-------->");
        if (prevNodeList == null || prevNodeList.size() == 0)
            printMessageln("empty");
        else
            prevNodeList.forEach(this::printMessageln);
        printMessageln("<---------previous status--------->");

        printMessageln("<---------current status--------->");
        if (currNodeList == null || currNodeList.size() == 0)
            printMessageln("empty");
        else
            currNodeList.forEach(this::printMessageln);
        printMessageln("<---------current status--------->");

        if ((currNodeList == null && currNodeList != prevNodeList)
                || (currNodeList != null && !currNodeList.equals(prevNodeList))) {
            List<String> nodesToAdd = new ArrayList<String>();
            List<String> nodesToRemove = new ArrayList<String>();
            if (currNodeList == null)
                currNodeList = new ArrayList<String>();
            for (String node : currNodeList) {
                if (!prevNodeList.contains(node))
                    nodesToAdd.add(node);
            }
            for (String node: prevNodeList) {
                if (!currNodeList.contains(node))
                    nodesToRemove.add(node);
            }
            prevNodeList = currNodeList;
            listener.handleChanged(nodesToAdd, nodesToRemove);
        }
    }

    private void printMessageln(String msg) {
        System.out.println("nodeMonitor: " + msg);
    }
}
