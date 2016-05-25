package lab.mars.util.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lab.mars.util.async.AsyncStream;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by haixiao on 2015/4/1.
 * Email: wumo@outlook.com
 */
public class TCPServer {
    private Set<Channel> channels;

    public TCPServer() {
        this.channels = new HashSet<>();
    }

    public AsyncStream bindAsync(String host, int port) {
        AsyncStream async = AsyncStream.deferredAsync();
        ServerBootstrap b = new ServerBootstrap();
        b.group(Network.bossGroup, Network.workerGroup)
         .channel(NioServerSocketChannel.class)
         .option(ChannelOption.TCP_NODELAY, true)
         .option(ChannelOption.SO_BACKLOG, 1000)
         .handler(new ChannelInboundHandlerAdapter() {
             @Override public void channelActive(ChannelHandlerContext ctx) throws Exception {
                 channels.add(ctx.channel());
                 super.channelActive(ctx);
             }

             @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                 channels.remove(ctx.channel());
                 super.channelInactive(ctx);
             }
         })
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
                 ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
                   .addLast(new LengthFieldPrepender(4))
                   .addLast(new SimpleChannelInboundHandler<ByteBuf>(false) {
                       @Override protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                           asyncStream.onEvent(new NetworkEvent<>(ctx, msg));
                       }

                       private AsyncStream asyncStream = AsyncStream.deferredAsync();

                       @Override
                       public void channelActive(ChannelHandlerContext ctx) throws Exception {
                           async.onEvent(new ConnectEvent(ctx.channel(), asyncStream));
                       }
                    
                       @Override
                       public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                           if (!asyncStream.onException(cause))
                               super.exceptionCaught(ctx, cause);
                       }
                   });
             }
         });

        b.bind(host, port).addListener((ChannelFuture channelFuture) -> {
            channels.add(channelFuture.channel());
            async.onEvent(channelFuture);
        });
        return async;
    }

    public void close() {
        for (Channel channel : channels)
            if (channel != null)
                channel.close();
    }
}
