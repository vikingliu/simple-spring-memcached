package com.google.code.ssm.zookeeper;

import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;

/**
 * Copyright (c) 2013, Wandou Labs and/or its affiliates. All rights reserved.
 * WANDOU LABS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms. User:
 * dahaili Date: 10/5/13
 */
public class InetSocketAddressUtils {
    static public InetSocketAddress parseAddrFromString(String address) {
        String[] arr = StringUtils.split(address, ":");
        return new InetSocketAddress(arr[0], Integer.parseInt(arr[1]));
    }

    static public String getAddress(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    static public String normalizeAddress(String address) {
        InetSocketAddress inetSocketAddress = parseAddrFromString(address);
        return getAddress(inetSocketAddress);
    }
}
