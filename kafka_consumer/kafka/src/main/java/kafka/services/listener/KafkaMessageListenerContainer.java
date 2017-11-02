/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.services.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ErrorHandler;

/**
 *
 * @author TZIMMER
 */
@Service
public class KafkaMessageListenerContainer implements InitializingBean, DisposableBean, BeanNameAware, SmartLifecycle {

    /**
     * Logger available to subclasses
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageListenerContainer.class);

    /**
     * Default thread name prefix: "RedisListeningContainer-".
     */
    public static final String DEFAULT_THREAD_NAME_PREFIX = ClassUtils.getShortName(KafkaMessageListenerContainer.class)
            + "-";

    private final String topic = "stomp-topic";

    /**
     * The default recovery interval: 5000 ms = 5 seconds.
     */
    public static final long DEFAULT_RECOVERY_INTERVAL = 5000;

    /**
     * The default subscription wait time: 2000 ms = 2 seconds.
     */
    public static final long DEFAULT_SUBSCRIPTION_REGISTRATION_WAIT_TIME = 2000L;

    private Executor subscriptionExecutor;

    private String beanName;

    private ErrorHandler errorHandler;

    // whether the container is running (or not)
    private volatile boolean running = false;
    // whether the container is listener to Kafka
    private final AtomicBoolean listening = new AtomicBoolean(false);
    // whether the container has been initialized
    private volatile boolean initialized = false;

    private volatile boolean manageExecutor = false;

    // lookup maps
    // to avoid creation of hashes for each message, the maps use raw byte arrays (wrapped to respect the equals/hashcode
    // contract)
    // lookup map between channels and listeners
    private final Map<ByteArrayWrapper, Collection<MessageListener>> channelMapping = new ConcurrentHashMap<>();

    // lookup map between listeners and channels
    private final Map<MessageListener, Set<Topic>> listenerTopics = new ConcurrentHashMap<>();

    private final SubscriptionTask subscriptionTask = new SubscriptionTask();

    private long recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

    private long maxSubscriptionRegistrationWaitingTime = DEFAULT_SUBSCRIPTION_REGISTRATION_WAIT_TIME;

    @Override
    public void afterPropertiesSet() {

        if (subscriptionExecutor == null) {
            subscriptionExecutor = createDefaultTaskExecutor();
        }

        initialized = true;
    }

    /**
     * Creates a default TaskExecutor. Called if no explicit TaskExecutor has
     * been specified.
     * <p>
     * The default implementation builds a
     * {@link org.springframework.core.task.SimpleAsyncTaskExecutor} with the
     * specified bean name (or the class name, if no bean name specified) as
     * thread name prefix.
     *
     * @see
     * org.springframework.core.task.SimpleAsyncTaskExecutor#SimpleAsyncTaskExecutor(String)
     */
    protected TaskExecutor createDefaultTaskExecutor() {
        String threadNamePrefix = (beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
        return new SimpleAsyncTaskExecutor(threadNamePrefix);
    }

    @Override
    public void destroy() throws Exception {
        initialized = false;

        stop();

        if (manageExecutor) {
            if (subscriptionExecutor instanceof DisposableBean) {
                ((DisposableBean) subscriptionExecutor).destroy();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Stopped internally-managed task executor");
                }
            }
        }
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        // start the latest
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void start() {
        if (!running) {
            running = true;
            // wait for the subscription to start before returning
            // technically speaking we can only be notified right before the subscription starts
            if (!listening.get()) {
                subscriptionExecutor.execute(subscriptionTask);
            }
            LOGGER.info("Started KafkaMessageListenerContainer");
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            running = false;
            subscriptionTask.cancel();
        }
        LOGGER.info("Stopped RedisMessageListenerContainer");
    }

    public void config() {
        String zookeeperConnect = "localhost:32181";
        int sessionTimeoutMs = 10 * 1000;
        int connectionTimeoutMs = 8 * 1000;

        ZkClient zkClient = new ZkClient(
                zookeeperConnect,
                sessionTimeoutMs,
                connectionTimeoutMs,
                ZKStringSerializer$.MODULE$);

        // Security for Kafka was added in Kafka 0.9.0.0
        boolean isSecureKafkaCluster = false;
        // ZkUtils for Kafka was used in Kafka 0.9.0.0 for the AdminUtils API
        ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConnect), isSecureKafkaCluster);

        int partitions = 1;
        int replication = 1;

        // Add topic configuration here
        Properties topicConfig = new Properties();

        scala.collection.Map<String, Properties> configs = AdminUtils.fetchAllTopicConfigs(zkUtils);
        if (configs.get(topic) == null) {
            AdminUtils.createTopic(zkUtils, topic, partitions, replication, topicConfig, RackAwareMode.Disabled$.MODULE$);
            LOGGER.info("Topic created");
        }
        zkClient.close();

    }

    /**
     * Process a message received from the provider.
     *
     */
    protected void processMessage(MessageListener listener, String topic, String message) {
        try {
            listener.onMessage(topic, message);
        } catch (Throwable ex) {
            handleListenerException(ex);
        }
    }

    /**
     * Return whether this container is currently active, that is, whether it
     * has been set up but not shut down yet.
     */
    public final boolean isActive() {
        return initialized;
    }

    /**
     * Handle the given exception that arose during listener execution.
     * <p>
     * The default implementation logs the exception at error level. This can be
     * overridden in subclasses.
     *
     * @param ex the exception to handle
     */
    protected void handleListenerException(Throwable ex) {
        if (isActive()) {
            // Regular case: failed while active.
            // Invoke ErrorHandler if available.
            invokeErrorHandler(ex);
        } else {
            // Rare case: listener thread failed after container shutdown.
            // Log at debug level, to avoid spamming the shutdown LOGGER.
            LOGGER.debug("Listener exception after container shutdown", ex);
        }
    }

    /**
     * Invoke the registered ErrorHandler, if any. Log at error level otherwise.
     *
     * @param ex the uncaught error that arose during message processing.
     * @see #setErrorHandler
     */
    protected void invokeErrorHandler(Throwable ex) {
        if (this.errorHandler != null) {
            this.errorHandler.handleError(ex);
        } else if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Execution of message listener failed, and no ErrorHandler has been set.", ex);
        }
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    /**
     * Sets the task execution used for subscribing to Redis channels. By
     * default, if no executor is set, the {@link #setTaskExecutor(Executor)}
     * will be used. In some cases, this might be undersired as the listening to
     * the connection is a long running task.
     * <p>
     * Note: This implementation uses at most one long running thread (depending
     * on whether there are any listeners registered or not) and up to two
     * threads during the initial registration.
     *
     * @param subscriptionExecutor The subscriptionExecutor to set.
     */
    public void setSubscriptionExecutor(Executor subscriptionExecutor) {
        this.subscriptionExecutor = subscriptionExecutor;
    }

    /**
     * Set an ErrorHandler to be invoked in case of any uncaught exceptions
     * thrown while processing a Message. By default there will be <b>no</b>
     * ErrorHandler so that error-level logging is the only result.
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Attaches the given listeners (and their topics) to the container.
     * <p>
     * Note: it's possible to call this method while the container is running
     * forcing a reinitialization of the container. Note however that this might
     * cause some messages to be lost (while the container reinitializes) -
     * hence calling this method at runtime is considered advanced usage.
     *
     * @param listeners map of message listeners and their associated topics
     */
    public void setMessageListeners(Map<? extends MessageListener, Collection<? extends Topic>> listeners) {
        initMapping(listeners);
    }

    /**
     * Adds a message listener to the (potentially running) container. If the
     * container is running, the listener starts receiving (matching) messages
     * as soon as possible.
     *
     * @param listener message listener
     * @param topics message listener topic
     */
    public void addMessageListener(MessageListener listener, Collection<? extends Topic> topics) {
        addListener(listener, topics);
    }

    /**
     * Adds a message listener to the (potentially running) container. If the
     * container is running, the listener starts receiving (matching) messages
     * as soon as possible.
     *
     * @param listener message listener
     * @param topic message topic
     */
    public void addMessageListener(MessageListener listener, Topic topic) {
        addMessageListener(listener, Collections.singleton(topic));
    }

    /**
     * Removes a message listener from the given topics. If the container is
     * running, the listener stops receiving (matching) messages as soon as
     * possible.
     * <p>
     * Note that this method obeys the Redis (p)unsubscribe semantics - meaning
     * an empty/null collection will remove listener from all channels.
     * Similarly a null listener will unsubscribe all listeners from the given
     * topic.
     *
     * @param listener message listener
     * @param topics message listener topics
     */
    public void removeMessageListener(MessageListener listener, Collection<? extends Topic> topics) {
        removeListener(listener, topics);
    }

    /**
     * Removes a message listener from the from the given topic. If the
     * container is running, the listener stops receiving (matching) messages as
     * soon as possible.
     * <p>
     * Note that this method obeys the Redis (p)unsubscribe semantics - meaning
     * an empty/null collection will remove listener from all channels.
     * Similarly a null listener will unsubscribe all listeners from the given
     * topic.
     *
     * @param listener message listener
     * @param topic message topic
     */
    public void removeMessageListener(MessageListener listener, Topic topic) {
        removeMessageListener(listener, Collections.singleton(topic));
    }

    /**
     * Removes the given message listener completely (from all topics). If the
     * container is running, the listener stops receiving (matching) messages as
     * soon as possible. Similarly a null listener will unsubscribe all
     * listeners from the given topic.
     *
     * @param listener message listener
     */
    public void removeMessageListener(MessageListener listener) {
        removeMessageListener(listener, Collections.<Topic>emptySet());
    }

    private void initMapping(Map<? extends MessageListener, Collection<? extends Topic>> listeners) {
        // stop the listener if currently running
        if (isRunning()) {
            subscriptionTask.cancel();
        }

        listenerTopics.clear();

        if (!CollectionUtils.isEmpty(listeners)) {
            for (Map.Entry<? extends MessageListener, Collection<? extends Topic>> entry : listeners.entrySet()) {
                addListener(entry.getKey(), entry.getValue());
            }
        }

        // resume activity
        if (initialized) {
            start();
        }
    }

    private void addListener(MessageListener listener, Collection<? extends Topic> topics) {
        Assert.notNull(listener, "a valid listener is required");
        Assert.notEmpty(topics, "at least one topic is required");

        List<byte[]> channels = new ArrayList<>(topics.size());

        boolean trace = LOGGER.isTraceEnabled();

        // add listener mapping
        Set<Topic> set = listenerTopics.get(listener);
        if (set == null) {
            set = new CopyOnWriteArraySet<>();
            listenerTopics.put(listener, set);
        }
        set.addAll(topics);

        for (Topic topic : topics) {

            ByteArrayWrapper holder = new ByteArrayWrapper(topic.getTopic().getBytes());

            if (topic instanceof ChannelTopic) {
                Collection<MessageListener> collection = channelMapping.get(holder);
                if (collection == null) {
                    collection = new CopyOnWriteArraySet<>();
                    channelMapping.put(holder, collection);
                }
                collection.add(listener);
                channels.add(holder.getArray());

                if (trace) {
                    LOGGER.trace("Adding listener '" + listener + "' on channel '" + topic.getTopic() + "'");
                }
            } else {
                throw new IllegalArgumentException("Unknown topic type '" + topic.getClass() + "'");
            }
        }
    }

    private void removeListener(MessageListener listener, Collection<? extends Topic> topics) {
        boolean trace = LOGGER.isTraceEnabled();

        // check stop listening case
        if (listener == null && CollectionUtils.isEmpty(topics)) {
            subscriptionTask.cancel();
            return;
        }

        List<byte[]> channelsToRemove = new ArrayList<>();

        // check unsubscribe all topics case
        if (CollectionUtils.isEmpty(topics)) {
            Set<Topic> set = listenerTopics.remove(listener);
            // listener not found, bail out
            if (set == null) {
                return;
            }
            topics = set;
        }

        for (Topic topic : topics) {
            ByteArrayWrapper holder = new ByteArrayWrapper(topic.getTopic().getBytes());

            if (topic instanceof ChannelTopic) {
                remove(listener, topic, holder, channelMapping, channelsToRemove);

                if (trace) {
                    String msg = (listener != null ? "listener '" + listener + "'" : "all listeners");
                    LOGGER.trace("Removing " + msg + " from channel '" + topic.getTopic() + "'");
                }
            }
        }
    }

    private void remove(MessageListener listener, Topic topic, ByteArrayWrapper holder,
            Map<ByteArrayWrapper, Collection<MessageListener>> mapping, List<byte[]> topicToRemove) {

        Collection<MessageListener> listeners = mapping.get(holder);
        Collection<MessageListener> listenersToRemove = null;

        if (listeners != null) {
            // remove only one listener
            if (listener != null) {
                listeners.remove(listener);
                listenersToRemove = Collections.singletonList(listener);
            } // no listener given - remove all of them
            else {
                listenersToRemove = listeners;
            }

            // start removing listeners
            for (MessageListener messageListener : listenersToRemove) {
                Set<Topic> topics = listenerTopics.get(messageListener);
                if (topics != null) {
                    topics.remove(topic);
                }
                if (CollectionUtils.isEmpty(topics)) {
                    listenerTopics.remove(messageListener);
                }
            }
            // if we removed everything, remove the empty holder collection
            if (listener == null || listeners.isEmpty()) {
                mapping.remove(holder);
                topicToRemove.add(holder.getArray());
            }
        }
    }

    /**
     * Handle subscription task exception. Will attempt to restart the
     * subscription if the Exception is a connection failure (for example, Redis
     * was restarted).
     *
     * @param ex Throwable exception
     */
    protected void handleSubscriptionException(Throwable ex) {
        listening.set(false);
    }

    /**
     * Sleep according to the specified recovery interval. Called between
     * recovery attempts.
     */
    protected void sleepBeforeRecoveryAttempt() {
        if (this.recoveryInterval > 0) {
            try {
                Thread.sleep(this.recoveryInterval);
            } catch (InterruptedException interEx) {
                LOGGER.debug("Thread interrupted while sleeping the recovery interval");
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Runnable used for Redis subscription. Implemented as a dedicated class to
     * provide as many hints as possible to the underlying thread pool.
     *
     * @author Costin Leau
     */
    private class SubscriptionTask implements SchedulingAwareRunnable {

        // Create our producer properties
        private final Properties props = new Properties();
        private org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer;
        private final Object localMonitor = new Object();
        private Collection<MessageListener> listeners = null;

        public SubscriptionTask() {
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092");
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
            props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60000);
            props.put(ConsumerConfig.CLIENT_ID_CONFIG, "my-consumer");
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-consumer-group");
        }

        @Override
        public boolean isLongLived() {
            return true;
        }

        @Override
        public void run() {
            try {
                // Run loop to get message on topic.
                listening.set(true);
                config();
                consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
                consumer.subscribe(Arrays.asList(topic));
                LOGGER.debug("Kafka Message Broker start listening to topic {}", topic);
                while (listening.get()) {
                    ConsumerRecords<String, String> records = consumer.poll(100);
                    // Handle new records
                    for (ConsumerRecord<String, String> record : records) {
                        if (record.topic().equals(topic)) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("partition", record.partition());
                            data.put("offset", record.offset());
                            data.put("timestamp", new Date(record.timestamp()));
                            data.put("key", record.key());
                            data.put("value", record.value());
                            LOGGER.debug("Kafka Broker get message {}", data);
                            listeners = channelMapping.get(new ByteArrayWrapper(record.key().getBytes()));
                            if (!CollectionUtils.isEmpty(listeners)) {
                                dispatchMessage(listeners, record.key(), record.value());
                            }
                        }
                    }
                }
                LOGGER.info("Kafka Message Broker stop listening to {}", topic);

            } catch (Throwable t) {
                // do not catch WakeupException when stopping.
                if (!listening.get() && t instanceof WakeupException) {
                    return;
                }
                handleSubscriptionException(t);
            } finally {
                // this block is executed once the subscription thread has ended, this may or may not mean
                // the connection has been unsubscribed, depending on driver
                synchronized (localMonitor) {
                    consumer.close();
                    localMonitor.notify();
                }
            }
        }

        void cancel() {
            if (!listening.get()) {
                return;
            }
            listening.set(false);
            consumer.wakeup();
            LOGGER.trace("Cancelling Kafka subscription...");
        }
    }

    private void dispatchMessage(Collection<MessageListener> listeners, final String topic, final String message) {
        for (final MessageListener messageListener : listeners) {
            subscriptionExecutor.execute(() -> processMessage(messageListener, topic, message));
        }
    }
}
