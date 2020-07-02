package server.master.zookeeper;

import common.Node;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Arrays;

public class dataMonitor implements Watcher, AsyncCallback.StatCallback {

    ZooKeeper zooKeeper;

    String znode;

    Watcher chainedWatcher;

    boolean isDead;

    dataMonitorListener listener;

    byte[] prevData;

    public interface dataMonitorListener {
        void handleUpdate(byte[] nodeData);
        void handleClosing(int rc);
    }

    public dataMonitor(ZooKeeper zooKeeper, String znode, Watcher chainedWatcher, dataMonitorListener listener) {
        this.zooKeeper = zooKeeper;
        this.znode = znode;
        this.chainedWatcher = chainedWatcher;
        this.listener = listener;
        this.prevData = null;
        zooKeeper.exists(znode, true, this, null);
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
                zooKeeper.exists(znode, true, this, null);
            }
        }
        if (chainedWatcher != null) {
            chainedWatcher.process(watchedEvent);
        }
    }

    @Override
    public void processResult(int i, String s, Object o, Stat stat) {
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
                zooKeeper.exists(znode, true, this, null);
                return;
        }

        byte[] currData = null;

        if (exists) {
            try {
                currData = zooKeeper.getData(znode, false, null);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                return;
            }
        }

        if ((currData == null && currData != prevData)
                || (currData != null && !Arrays.equals(prevData, currData))) {
            prevData = currData;
            if (currData != null)
                listener.handleUpdate(currData);
        }
    }
}
