package kafka.services;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompHeaders;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kafka.config.StompRedisRelayProperties;
import kafka.helpers.Listener;
import kafka.services.listener.ChannelTopic;
import kafka.services.listener.KafkaMessageListenerContainer;
import kafka.services.listener.MessageListener;
import kafka.services.listener.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Geoff Bourne
 */
@Service
public class SubscriptionManagement {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionManagement.class);
    private final StompRedisRelayProperties properties;
    private final KafkaMessageListenerContainer kafkaMessageListenerContainer;

    private final ConcurrentHashMap<String/*listnerUniqueId*/, PerSubscriptionListenerAdapter> subscriptions
            = new ConcurrentHashMap<>();

    @Autowired
    public SubscriptionManagement(
            StompRedisRelayProperties properties,
            KafkaMessageListenerContainer kafkaMessageListenerContainer
    ) {

        this.properties = properties;
        this.kafkaMessageListenerContainer = kafkaMessageListenerContainer;
    }

    public void subscribe(ChannelHandlerContext context, String channel, String subId) {

        // dont want to trust the client to guive me a unique session id
        // so, going to add some salt to the session id and try to mitigate the chances for the client miss behaving
        String listenerId = Listener.getListenerUniqueId(context, subId);

        if (!subscriptions.containsKey(listenerId)) {
            Topic topic = new ChannelTopic(channel);
            LOGGER.debug("Subscribing client address={} subscription={} onto topic={}",
                    context.channel().remoteAddress(), subId, topic);

            PerSubscriptionListenerAdapter listener = new PerSubscriptionListenerAdapter(context, subId, listenerId);
            kafkaMessageListenerContainer.addMessageListener(listener, topic);
            subscriptions.put(listenerId, listener);
        } else {

            LOGGER.warn(
                    "Listner [{}] already exists. Maybe clients are not sending unique session ids.",
                    listenerId);
        }
    }

    public void unsubscribe(ChannelHandlerContext context, String listnerUniqueId) {

        PerSubscriptionListenerAdapter listener = subscriptions.remove(listnerUniqueId);

        if (listener != null) {
            kafkaMessageListenerContainer.removeMessageListener(listener);
            LOGGER.debug("Unsubscribed subscription ID={} from context={}", listnerUniqueId, context);
        } else {
            throw new IllegalArgumentException(String.format("Unknown subscription ID [%s]", listnerUniqueId));
        }
    }

    public void unsubscribeAllForContext(ChannelHandlerContext context) {
        subscriptions.entrySet().stream()
                .filter(e -> e.getValue().context.channel().equals(context.channel()))
                .forEach(e -> unsubscribe(context, e.getKey()));
    }

    private class PerSubscriptionListenerAdapter implements MessageListener {

        private final ChannelHandlerContext context;
        private final AtomicLong messageId = new AtomicLong(0);
        private final String subId, listenerId;

        public PerSubscriptionListenerAdapter(ChannelHandlerContext context, String subId, String listenerId) {

            this.context = context;
            this.subId = subId;
            this.listenerId = listenerId;
        }

        @Override
        public void onMessage(String topic, String message) {
            LOGGER.trace("Subscription [{}] received kafka channel message [{}]", listenerId, message);
            final DefaultStompFrame messageFrame = new DefaultStompFrame(StompCommand.MESSAGE);
            messageFrame.headers().add(StompHeaders.SUBSCRIPTION, subId);
            messageFrame.headers().addLong(StompHeaders.MESSAGE_ID, messageId.addAndGet(1));
            messageFrame.headers().add(StompHeaders.DESTINATION, topic);
            messageFrame.headers().add(StompHeaders.CONTENT_TYPE, "application/json");
            messageFrame.headers().addInt(StompHeaders.CONTENT_LENGTH, message.length());
            messageFrame.content().writeCharSequence(message, Charset.defaultCharset());
            context.writeAndFlush(messageFrame);
        }
    }
}
