package kafka.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import kafka.config.StompRedisRelayProperties;
import kafka.services.SubscriptionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Geoff Bourne
 */
@Component
public class SubscribeHandlerFactory implements StompFrameHandlerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeHandlerFactory.class);
    private final StompRedisRelayProperties properties;
    private final SubscriptionManagement subscriptionManagement;

    @Autowired
    public SubscribeHandlerFactory(StompRedisRelayProperties properties,
            SubscriptionManagement subscriptionManagement) {
        this.properties = properties;
        this.subscriptionManagement = subscriptionManagement;
    }

    @Override
    public StompCommand getCommand() {
        return StompCommand.SUBSCRIBE;
    }

    @Override
    public StompFrameHandler create(ChannelHandlerContext context, StompHeaders headers, ByteBuf content) {
        return new SubscribeHandler(context, headers, content);
    }

    private class SubscribeHandler extends AbstractStompFrameHandler {

        public SubscribeHandler(ChannelHandlerContext context, StompHeaders headers, ByteBuf content) {
            super(context, headers, content);
        }

        @Override
        public SubscribeHandler invoke() {
            final String subId = headers.getAsString(StompHeaders.ID);
            final String destination = headers.getAsString(StompHeaders.DESTINATION);
            subscriptionManagement.subscribe(context, destination, subId);
            return this;
        }

    }
}
