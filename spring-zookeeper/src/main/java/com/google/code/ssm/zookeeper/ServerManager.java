package com.google.code.ssm.zookeeper;

import java.util.List;

/**
 * @author chenxingrun@wandoujia.com
 */
public interface ServerManager {

    /**
     * get all servers from remote configuration
     * 
     * @return host:port
     * @throws Exception
     */
    List<String> getRemoteServerList() throws Exception;

    /**
     * get all servers in used
     *
     * @return host:port
     * @throws Exception
     */
    List<String> getLocalServerList() throws Exception;

    /**
     * set client
     * 
     * @param client
     */
    void setClient(ServerManagerAwareClient client);

    /**
     * sync local server list with remote
     * 
     * @throws Exception
     */
    void syncServerList() throws Exception;

    /**
     * start server manager to keep local server list up-to-date
     *
     * @throws Exception
     */
    void start() throws Exception;

    /**
     * stop server manager
     */
    void shutdown();

    /**
     * enable or disable auto sync function
     * @param autoSync
     */
    void setAutoSync(boolean autoSync);

}
