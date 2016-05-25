package lab.mars.util.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lab.mars.util.async.AsyncStream;

/**
 * Created by haixiao on 2015/4/1.
 * Email: wumo@outlook.com
 */
public class TCPClient {
    private Channel channel;

    public AsyncStream connectAsync(String host, int port) {
        AsyncStream async = AsyncStream.deferredAsync();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(Network.workerGroup)
                 .channel(NioSocketChannel.class)
                 .option(ChannelOption.TCP_NODELAY, true)
                 .handler(new ChannelInitializer<SocketChannel>() {
                     @Override
                     protected void initChannel(SocketChannel ch) throws Exception {
                         ch.pipeline()
                           .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
                           .addLast(new LengthFieldPrepender(4))
                           .addLast(new SimpleChannelInboundHandler<ByteBuf>(false) {
                               @Override protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
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
        bootstrap.connect(host, port).addListener((ChannelFuture future) -> {
            channel = future.channel();
            async.onEvent(future);
        });
        return async;
    }

    public void write(Object msg) {
        channel.writeAndFlush(msg);
    }

    public void close() {
        if (channel != null)
            channel.close();
    }
}
