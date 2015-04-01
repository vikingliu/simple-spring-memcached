package com.google.code.ssm.zookeeper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;

/**
 * @author chenxingrun@wandoujia.com
 */
public class ZooKeeperServerManagerImpl implements ServerManager {
    private String zkAddress;

    private String zkPath;

    private volatile boolean autoSync;

    protected ServerManagerAwareClient client;

    private CuratorFramework zkClient;

    private PathChildrenCache pathChildrenCache;

    private ZooKeeperPathAcceptor acceptor;

    private static final int sleepInterval = (int) TimeUnit.SECONDS
            .toMillis(10);

    // Adds a thread to sync by period if no server found so far.
    // This is to make sure the client could found correct server even if the
    // server change event is missed from zk by any reason.
    private class ScanRunnable implements Runnable {
        private volatile boolean stop = false;

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(sleepInterval);
                    ServerManagerAwareClient serverManagerAwareClient = client;
                    if (serverManagerAwareClient == null) {
                        continue;
                    }
                    if (stop) {
                        break;
                    }
                    List<String> serverList = getLocalServerList();
                    if (CollectionUtils.isEmpty(serverList)
                            || serverManagerAwareClient.hasDeadConnection()) {
                        syncServerList();
                    }
                } catch (InterruptedException ignored) {} catch (KeeperException ignored) {} catch (IOException ignored) {}
            }
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }
    }

    private ScanRunnable scanRunnable;

    /**
     * @param zkAddress
     *            ip:port address of ZooKeeper server
     * @param zkPath
     *            root path in ZooKeeper, eg.
     *            /com/wandoujia/[project]/[moudle]/[
     *            service]/[shard]/[replica]/type
     * @see <a
     *      href="https://docs.google.com/a/wandoujia.com/document/d/1WfMpwgqZ5l2OhyP5ArV_XY8H_BcnN7KLY15EZykZ2rM/edit#heading=h.uckhfogoyhh8">ZooKeeper
     *      Path Spec</a>
     */
    public ZooKeeperServerManagerImpl(final String zkAddress,
            final String zkPath) {
        this.zkAddress = zkAddress;
        this.zkPath = zkPath;
        this.autoSync = true;
        scanRunnable = new ScanRunnable();
    }

    @Override
    // for example: rpc change (but not frontend change).
    public void setClient(final ServerManagerAwareClient client) {
        this.client = client;
    }

    @Override
    public void setAutoSync(final boolean autoSync) {
        this.autoSync = autoSync;
    }

    @Override
    public void start() throws Exception {
        zkClient = CuratorFrameworkFactory.newClient(zkAddress,
                new ExponentialBackoffRetry(1000, 3));
        zkClient.start();
        pathChildrenCache = new PathChildrenCache(zkClient, zkPath, true);
        pathChildrenCache.getListenable().addListener(new ServerListListener(),
                Executors.newSingleThreadExecutor());
        pathChildrenCache.start(StartMode.BUILD_INITIAL_CACHE);
        syncServerList();
        Thread thread = new Thread(scanRunnable);
        thread.start();
    }

    @Override
    public void shutdown() {
        scanRunnable.setStop(true);
        pathChildrenCache.getListenable().clear();
        IOUtils.closeQuietly(pathChildrenCache);
        IOUtils.closeQuietly(zkClient);
    }

    @Override
    public synchronized void syncServerList() throws KeeperException,
            InterruptedException, IOException {
        if (client == null) {
            return;
        }

        List<String> remoteServerList = getRemoteServerList();
        List<String> localServerList = getLocalServerList();

        @SuppressWarnings("unchecked")
        Collection<String> serversToBeAdd = CollectionUtils.subtract(
                remoteServerList, localServerList);
        @SuppressWarnings("unchecked")
        Collection<String> serversToBeRemove = CollectionUtils.subtract(
                localServerList, remoteServerList);

        boolean changeServers = false;
        if (CollectionUtils.isNotEmpty(serversToBeAdd)) {
            changeServers = true;
            for (String server: serversToBeAdd) {
                client.addServer(server);
            }
        }

        if (CollectionUtils.isNotEmpty(serversToBeRemove)) {
            changeServers = true;
            for (String server: serversToBeRemove) {
                client.removeServer(server);
            }
        }
        if (changeServers) {
            client.changeServers(remoteServerList);
        }
    }

    @Override
    public synchronized List<String> getRemoteServerList()
            throws KeeperException, InterruptedException {
        List<ChildData> children = pathChildrenCache.getCurrentData();
        List<String> serverList = new ArrayList<String>(children.size());
        for (ChildData data: children) {
            if (acceptor != null && !acceptor.accept(data)) {
                continue;
            }
            if (data.getData().length == 0)
                continue;
            serverList.add(InetSocketAddressUtils.normalizeAddress(new String(
                    data.getData())));
        }
        return serverList;
    }

    @Override
    public List<String> getLocalServerList() {
        if (client != null) {
            Collection<InetSocketAddress> servers = client
                    .getAvailableServers();
            List<String> serverList = new ArrayList<String>(servers.size());
            for (InetSocketAddress addr: servers) {
                String server = InetSocketAddressUtils.getAddress(addr);
                serverList.add(server);
            }
            return serverList;
        }
        return Collections.emptyList();
    }

    // for unittest only
    protected synchronized void addServerToRemote(final String address)
            throws Exception {
        zkClient.create().creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT_SEQUENTIAL)
                .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                .forPath(zkPath + "/node-", address.getBytes());
    }

    // for unittest only
    protected synchronized void removeServerFromRemote(final String address)
            throws Exception {
        List<ChildData> children = pathChildrenCache.getCurrentData();
        for (ChildData data: children) {
            if (address.equals(new String(data.getData()))) {
                zkClient.delete().withVersion(data.getStat().getVersion())
                        .forPath(data.getPath());
            }
        }
    }

    public void setAcceptor(ZooKeeperPathAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    class ServerListListener implements PathChildrenCacheListener {
        @Override
        public void childEvent(final CuratorFramework client,
                final PathChildrenCacheEvent event) throws Exception {
            switch (event.getType()) {
                case CHILD_ADDED:
                case CHILD_REMOVED:
                case CHILD_UPDATED:
                    if (autoSync) {
                        syncServerList();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    protected CuratorFramework getZkClient() {
        return zkClient;
    }

    protected PathChildrenCache getPathChildrenCache() {
        return pathChildrenCache;
    }

    protected ZooKeeperPathAcceptor getAcceptor() {
        return acceptor;
    }
}
