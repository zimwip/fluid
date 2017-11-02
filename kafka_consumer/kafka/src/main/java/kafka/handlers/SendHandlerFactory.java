package kafka.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompHeaders;
import java.nio.charset.Charset;
import kafka.config.StompRedisRelayProperties;
import kafka.services.KafkaProducer;
import kafka.services.SubscriptionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author @albertocsm
 */
@Component
public class SendHandlerFactory implements StompFrameHandlerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendHandlerFactory.class);
    private KafkaProducer producer;
    private final StompRedisRelayProperties properties;

    @Autowired
    public SendHandlerFactory(
            StompRedisRelayProperties properties,
            KafkaProducer producer
    ) {

        this.producer = producer;
        this.properties = properties;
    }

    @Override
    public StompCommand getCommand() {
        return StompCommand.SEND;
    }

    @Override
    public StompFrameHandler create(
            ChannelHandlerContext context,
            StompHeaders headers,
            ByteBuf content) {
        return new SendHandler(context, headers, content);
    }

    private class SendHandler extends AbstractStompFrameHandler {

        public SendHandler(
                ChannelHandlerContext context,
                StompHeaders headers,
                ByteBuf content) {
            super(context, headers, content);
        }

        @Override
        public SendHandler invoke() {

            final String destination = headers.getAsString(StompHeaders.DESTINATION);
            if (!true /*!destination.startsWith(properties.getChannelPrefix())*/) {
                buildErrorResponse("Incorrect subscription prefix");
            }
            producer.send(destination, content.toString(Charset.defaultCharset()));
            return this;
        }

    }
}
