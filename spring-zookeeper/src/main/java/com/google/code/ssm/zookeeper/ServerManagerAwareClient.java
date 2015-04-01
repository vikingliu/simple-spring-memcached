package com.google.code.ssm.zookeeper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

/**
 * @author chenxingrun@wandoujia.com
 */
public interface ServerManagerAwareClient {

    Collection<InetSocketAddress> getAvailableServers();

    void addServer(String address) throws IOException;

    void removeServer(String address);

    void changeServers(List<String> remoteServerList);

    boolean hasDeadConnection();
}
