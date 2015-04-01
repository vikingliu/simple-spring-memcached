package com.google.code.ssm.zookeeper;

import org.apache.curator.framework.recipes.cache.ChildData;

/**
 * Copyright (c) 2013, Wandou Labs and/or its affiliates. All rights reserved.
 * WANDOU LABS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author: lidahai
 * Date: 10/10/13
 * Time: 11:28 AM
 */
public interface ZooKeeperPathAcceptor {
    boolean accept(ChildData data);
}
