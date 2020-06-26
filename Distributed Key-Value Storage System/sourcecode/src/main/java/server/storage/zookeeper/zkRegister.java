package server.storage.zookeeper;

import common.Node;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

public class zkRegister implements Runnable {

    private static ZooKeeper zooKeeper;

    private static String znode = "/kvStorage";

    private static Node initNode;

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
                String info = "znode for kvStorage management";
                byte[] data = info.getBytes();
                zooKeeper.create(znode, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            zooKeeper.create(znode + "/" + initNode.getAlias(),
                    initNode.toString().getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL
                    );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() throws Exception {
        zooKeeper.close();
    }
}
