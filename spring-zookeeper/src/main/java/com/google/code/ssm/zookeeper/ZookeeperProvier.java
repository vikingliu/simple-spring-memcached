package com.google.code.ssm.zookeeper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import com.google.code.ssm.config.AddressChangeListener;
import com.google.code.ssm.config.AddressChangeNotifier;
import com.google.code.ssm.config.DefaultAddressProvider;

public class ZookeeperProvier extends DefaultAddressProvider implements
        AddressChangeNotifier, InitializingBean, ServerManagerAwareClient {
    private ServerManager serverManager;

    @Getter
    @Setter
    private AddressChangeListener addressChangeListener;

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public List<InetSocketAddress> getAddresses() {
        try {
            List<String> serverList = serverManager.getRemoteServerList();
            serverManager.setClient(this);
            String address = StringUtils.join(serverList, " ");
            return super.getAddresses(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<InetSocketAddress> getAvailableServers() {
        return null;
    }

    @Override
    public void addServer(String address) throws IOException {

    }

    @Override
    public void removeServer(String address) {

    }

    @Override
    public boolean hasDeadConnection() {
        return false;
    }

    @Override
    public void changeServers(List<String> remoteServerList) {
        if (addressChangeListener != null) {
            String address = StringUtils.join(remoteServerList, " ");
            List<InetSocketAddress> addresses = super.getAddresses(address);
            addressChangeListener.changeAddresses(addresses);
        }
    }
}
