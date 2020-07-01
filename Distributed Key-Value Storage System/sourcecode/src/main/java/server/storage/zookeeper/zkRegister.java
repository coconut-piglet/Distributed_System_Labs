package server.storage.zookeeper;

import common.Node;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

public class zkRegister implements Runnable {

    private final ZooKeeper zooKeeper;

    private final String znode = "/kvStorage";

    private final Node initNode;

    private String myPath;

    public String getMyPath() {
        return myPath;
    }

    public zkRegister(String host, Node node) throws Exception {
        initNode = node;

        CountDownLatch connectedSignal = new CountDownLatch(1);

        Watcher watcher = new Watcher() {

            @Override
            public void process(WatchedEvent watchedEvent) {

                if (watchedEvent.getState() == Event.KeeperState.SyncConnected)
                    connectedSignal.countDown();

            }
        };

        zooKeeper = new ZooKeeper(host, 5000, watcher);

        connectedSignal.await();
    }

    @Override
    public void run() {
        try {
            Stat stat = zooKeeper.exists(znode,false);
            if (stat == null) {
                String info = "znode for kvStorage management [MODIFIED]";
                byte[] data = info.getBytes();
                zooKeeper.create(znode, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            byte[] initData = SerializationUtils.serialize(initNode);
            myPath = zooKeeper.create(znode + "/" + initNode.getAlias(),
                    initData,
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL
                    );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateNodeData(Node node) throws Exception {
        String info = "znode for kvStorage management [MODIFIED]";
        byte[] newData = SerializationUtils.serialize(node);
        byte[] newInfo = info.getBytes();
        zooKeeper.setData(myPath, newData, zooKeeper.exists(myPath, true).getVersion());
        zooKeeper.setData(znode, newInfo, zooKeeper.exists(znode, true).getVersion());
    }

    public void disconnect() throws Exception {
        zooKeeper.close();
    }
}
