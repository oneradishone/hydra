/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.hydra.mq;

import java.io.IOException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.addthis.bark.StringSerializer;
import com.addthis.codec.jackson.Jackson;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkMessageConsumer<T> implements MessageConsumer<T> {

    private static final Logger log = LoggerFactory.getLogger(ZkMessageConsumer.class);

    private CuratorFramework zkClient;
    private String path;
    private Class<T> clazz;
    private final Set<MessageListener<T>> messageListeners = new HashSet<>();
    private PathChildrenCache cache;

    public ZkMessageConsumer(CuratorFramework zkClient, String path, MessageListener<T> messageListener, final Class<T> clazz) {
        this.clazz = clazz;
        init(zkClient, path, messageListener);
    }

    private void init(final CuratorFramework zkClient, final String path, MessageListener<T> messageListener) {
        this.zkClient = zkClient;
        this.path = path;
        addMessageListener(messageListener);
        try {
            open();
            if (zkClient.checkExists().forPath(path) == null) {
                zkClient.create().creatingParentsIfNeeded().forPath(path);
            }
            notifyListeners(zkClient.getChildren().forPath(path));
            cache = new PathChildrenCache(zkClient, path, true);
            cache.start();
            cache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                    switch (pathChildrenCacheEvent.getType()) {
                        case CHILD_UPDATED:
                            notifyListeners(StringSerializer.deserialize(pathChildrenCacheEvent.getData().getData()));
                            break;
                        default:
                            log.debug("Ignored path event for node: " + pathChildrenCacheEvent);
                    }
                }
            });
        } catch (Exception e) {
            log.warn("error opening client: ", e);
        }
    }


    private void notifyListeners(List<String> values) throws Exception {
        if (values != null && values.size() > 0) {
            for (String node : values) {
                notifyListeners(StringSerializer.deserialize(zkClient.getData().forPath(path + "/" + node)));
            }

        }
    }

    private void notifyListeners(String json) throws IOException {
        // A child znode may be used for another purpose and have no
        // data in it.
        if (json == null || json.isEmpty()) {
            log.warn("got null notification.  Ignoring");
            return;
        }
        T message = Jackson.defaultMapper().readValue(json, clazz);

        for (MessageListener<T> listener : messageListeners) {
            listener.onMessage(message);
        }
    }

    @Override
    public void open() throws IOException {
        // Working client is the only setup required.
    }

    @Override
    public void close() throws IOException {
        // Whomever passed us the client needs to shut it down.
    }

    @Override public boolean addMessageListener(MessageListener<T> hostMessageListener) {
        return messageListeners.add(hostMessageListener);
    }

    @Override public boolean removeMessageListener(MessageListener<T> hostMessageListener) {
        return messageListeners.remove(hostMessageListener);
    }
}
