package kafka.services;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;
import io.netty.handler.codec.stomp.StompSubframeEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import kafka.config.StompRedisRelayProperties;
import kafka.handlers.StompFrameHandler;
import kafka.handlers.StompFrameHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Geoff Bourne
 */
@Service
public class StompKafkaRelayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StompKafkaRelayService.class);

    private final StompRedisRelayProperties properties;
    private final SubscriptionManagement subscriptionManagement;
    private final EnumMap<StompCommand, StompFrameHandlerFactory> handlerFactories;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    @Autowired
    public StompKafkaRelayService(StompRedisRelayProperties properties,
            List<StompFrameHandlerFactory> handlerFactories,
            SubscriptionManagement subscriptionManagement) {
        this.properties = properties;
        this.subscriptionManagement = subscriptionManagement;

        this.handlerFactories = new EnumMap<>(StompCommand.class);
        for (StompFrameHandlerFactory handlerFactory : handlerFactories) {
            LOGGER.debug("Registering handler factory {} for {}", handlerFactory, handlerFactory.getCommand());
            this.handlerFactories.put(handlerFactory.getCommand(), handlerFactory);
        }
    }

    @PostConstruct
    public void start() {
        try {
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            LOGGER.info("Starting...");
            new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            final ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast("Logger", new LoggingHandler(LogLevel.TRACE));
                            pipeline.addLast("decoder", new StompSubframeDecoder());
                            pipeline.addLast("encoder", new StompSubframeEncoder());
                            pipeline.addLast("aggregator", new StompSubframeAggregator(properties.getMaxContentLength()));
                            pipeline.addLast("relay", new InboundStompFrameHandler());
                        }
                    })
                    .bind(properties.getPort())
                    .sync();

            LOGGER.info("STOMP relay service started on port {}", properties.getPort());

        } catch (InterruptedException e) {
            throw new IllegalStateException("Failed to startup server channel", e);
        }
    }

    @PreDestroy
    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        bossGroup = null;
        workerGroup = null;
    }

    private class InboundStompFrameHandler extends SimpleChannelInboundHandler<StompFrame> {

        @Override
        protected void channelRead0(ChannelHandlerContext context, StompFrame stompFrame) throws Exception {

            final StompCommand command = stompFrame.command();
            LOGGER.debug("Processing incoming STOMP {} frame: {}", command, stompFrame);
            
            final StompHeaders headers = stompFrame.headers();
            final String receipt = headers.getAsString(StompHeaders.RECEIPT);
            final ByteBuf content = stompFrame.content();

            final StompFrameHandlerFactory handlerFactory = handlerFactories.get(command);
            if (handlerFactory == null) {
                LOGGER.warn("Received an unsupported command {}", command);
                StompFrame response = new DefaultStompFrame(StompCommand.ERROR);
                context.writeAndFlush(response);
                context.close();
                return;
            }

            final StompFrameHandler stompFrameHandler = handlerFactory.create(context, headers, content);
            stompFrameHandler.invoke();

            final StompFrame response = stompFrameHandler.getResponse();
            if (response != null) {
                LOGGER.trace("Responding with STOMP frame: {}", response);
                context.writeAndFlush(response);
            } else if (receipt != null) {
                final DefaultStompFrame receiptFrame = new DefaultStompFrame(StompCommand.RECEIPT);
                receiptFrame.headers().add(StompHeaders.RECEIPT_ID, receipt);
                context.writeAndFlush(receiptFrame);
            }

            if (stompFrameHandler.isCloseAfterResponse()) {
                context.close();
                subscriptionManagement.unsubscribeAllForContext(context);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
            LOGGER.warn("Exception in channel", cause);
            subscriptionManagement.unsubscribeAllForContext(context);
            context.close();
        }
    }
}
