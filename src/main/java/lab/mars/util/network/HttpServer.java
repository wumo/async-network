package lab.mars.util.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import lab.mars.util.async.AsyncStream;
import lab.mars.util.network.ssl.SslContextFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by haixiao on 2015/4/1.
 * Email: wumo@outlook.com
 */
public class HttpServer {
    private Set<Channel> channels;
    private boolean ssl = false;
    private SSLContext context;

    public HttpServer() {
        this.channels = new HashSet<>();
    }

    public AsyncStream bindAsync(String host, int port) {
        if(ssl) {
            context = SslContextFactory.getServerContext();
        }
        AsyncStream async = AsyncStream.deferredAsync();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(Network.bossGroup, Network.workerGroup)
                 .channel(NioServerSocketChannel.class)
                 .option(ChannelOption.TCP_NODELAY, true)
                 .option(ChannelOption.SO_BACKLOG, 1000)
                 .handler(new ChannelInboundHandlerAdapter() {
                     @Override public void channelActive(ChannelHandlerContext ctx) throws Exception {
                         channels.add(ctx.channel());
//				         super.channelActive(ctx);
                     }

                     @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                         channels.remove(ctx.channel());
//				         super.channelInactive(ctx);
                     }
                 })
                 .childHandler(new ChannelInitializer<Channel>() {
                     @Override protected void initChannel(Channel ch) throws Exception {
                         if(ssl) {
                             SSLEngine engine = context.createSSLEngine();
                             engine.setUseClientMode(false);
                             ch.pipeline().addFirst("ssl", new SslHandler(engine));
                         }
                         ch.pipeline().addLast(new HttpServerCodec())
                           .addLast(new HttpObjectAggregator(512 * 1024))
                           .addLast(new HttpContentCompressor())
                           .addLast(new SimpleChannelInboundHandler<FullHttpRequest>(false) {
                               @Override protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
                                   async.onEvent(new NetworkEvent<>(ctx, msg));
                               }

                               @Override
                               public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                   if (!async.onException(cause))
                                       super.exceptionCaught(ctx, cause);
                               }
                           });
                     }
                 });

        bootstrap.bind(host, port).addListener((ChannelFuture channelFuture) -> {
            channels.add(channelFuture.channel());
            async.onEvent(channelFuture);
        });
        return async;
    }

    public void enableHttps(boolean flag) {
        ssl = flag;
    }

    public void close() {
        for (Channel channel : channels)
            if (channel != null)
                channel.close();
    }
}
