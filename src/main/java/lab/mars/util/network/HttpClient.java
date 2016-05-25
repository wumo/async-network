package lab.mars.util.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import lab.mars.util.async.AsyncStream;
import lab.mars.util.network.ssl.SslContextFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.URI;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by haixiao on 2015/4/1.
 * Email: wumo@outlook.com
 */
public class HttpClient {

    private SSLContext context;
    private boolean ssl = false;

    public HttpClient() {
        context = SslContextFactory.getClientContext();
    }

    public static HttpRequest makeRequest(HttpMethod method,
                                          String rawPath,
                                          String[][] req_headers,
                                          String requestBody) {
        ByteBufOutputStream out = new ByteBufOutputStream(Unpooled.buffer());
        if (requestBody != null)
            try {
                out.write(requestBody.getBytes(CharsetUtil.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, method, rawPath, out.buffer());
        if (req_headers != null)
            for (String[] head : req_headers)
                request.headers().set(head[0], head[1]);
//		request.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        request.headers().set(Names.CONTENT_LENGTH, Integer.toString(request.content().readableBytes()));
        return request;
    }

    public AsyncStream connectAsync(String inetHost, int inetPort) {
        AsyncStream async = AsyncStream.deferredAsync();

        Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap.group(Network.workerGroup)
                       .channel(NioSocketChannel.class)
                       .option(ChannelOption.TCP_NODELAY, true)
                       .handler(new ChannelInitializer<Channel>() {
                           @Override
                           protected void initChannel(Channel ch) throws Exception {
                               if(ssl) {
                                   SSLEngine engine = context.createSSLEngine();
                                   engine.setUseClientMode(true);
                                   ch.pipeline().addFirst("ssl", new SslHandler(engine));
                               }
                               ch.pipeline().addLast(new HttpClientCodec())
                                 .addLast(new HttpContentDecompressor())
                                 .addLast(new HttpObjectAggregator(1048576))
                                 .addLast(new SimpleChannelInboundHandler<FullHttpResponse>(false) {
                                     @Override protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
                                         async.onEvent(new NetworkEvent<>(ctx, msg));
                                     }

                                     @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                         if (!async.onException(cause))
                                             super.exceptionCaught(ctx, cause);
                                     }
                                 });
                           }
                       });

        clientBootstrap.connect(inetHost, inetPort).addListener(async::onEvent);

        return async;
    }

    /**
     * if fail, put null into asyncStream.
     */
    public AsyncStream requestAsync(URI uri, HttpRequest request) {
        AsyncStream async = AsyncStream.deferredAsync();
        Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap.group(Network.workerGroup)
                       .channel(NioSocketChannel.class)
                       .handler(new ChannelInitializer<Channel>() {
                           @Override
                           protected void initChannel(Channel ch) throws Exception {
                               if(ssl) {
                                   SSLEngine engine = context.createSSLEngine();
                                   engine.setUseClientMode(true);
                                   ch.pipeline().addFirst("ssl", new SslHandler(engine));
                               }
                               ch.pipeline().addLast(new HttpClientCodec())
                                 .addLast(new HttpContentDecompressor())
                                 .addLast(new HttpObjectAggregator(1048576))
                                 .addLast(new SimpleChannelInboundHandler<FullHttpResponse>(false) {
                                     @Override protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
                                         async.onEvent(new NetworkEvent<>(ctx, msg));
                                     }

                                     @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                         if (!async.onException(cause))
                                             super.exceptionCaught(ctx, cause);
                                     }
                                 });
                           }
                       });

        clientBootstrap.connect(uri.getHost(), uri.getPort())
                       .addListener((ChannelFuture channelFuture) -> {
                           Channel ch = channelFuture.channel();
                           if (channelFuture.isSuccess())
                               ch.writeAndFlush(request);
                           else
                               async.onEvent(null);
                       });

        return async;
    }

    public void enableHttps(boolean flag) {
        ssl = flag;
    }

}
