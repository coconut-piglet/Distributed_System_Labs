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
        this.prevNodeList = new ArrayList<>();
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

        if ((currNodeList == null && currNodeList != prevNodeList)
                || (currNodeList != null && !currNodeList.equals(prevNodeList))) {
            List<String> nodesToAdd = currNodeList;
            List<String> nodesToRemove = prevNodeList;
            nodesToAdd.removeAll(prevNodeList);
            nodesToRemove.removeAll(currNodeList);
            listener.handleChanged(nodesToAdd, nodesToRemove);
            prevNodeList = currNodeList;
        }
    }
}
