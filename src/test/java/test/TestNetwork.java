package test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lab.mars.util.network.*;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static lab.mars.util.network.HttpClient.makeRequest;

/**
 * Created by haixiao on 2015/4/1.
 * Email: wumo@outlook.com
 */
public class TestNetwork {
    @Test
    public void testHttp() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpServer server = new HttpServer();
        server.enableHttps(true);
        server.bindAsync("localhost", 8080)
              .<ChannelFuture>then(future -> {
                  Assert.assertTrue(future.isSuccess());
                  HttpClient client = new HttpClient();
                  client.enableHttps(true);
                  client.connectAsync("localhost", 8080)
                        .<ChannelFuture>then(future2 -> {
                            Assert.assertTrue(future2.isSuccess());
                            HttpRequest request = makeRequest(HttpMethod.GET, "/", null, "Hello! from client");
                            future2.channel().writeAndFlush(request);
                        })
                        .<NetworkEvent<FullHttpResponse>>then(e -> {
                            System.out.println(e.msg.content().toString(CharsetUtil.UTF_8));
                            latch.countDown();
                            e.ctx.close();
                        });
              })
              .<NetworkEvent<FullHttpRequest>>loop(e -> {
                  System.out.println(e.msg.content().toString(CharsetUtil.UTF_8));
                  FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer("World! from Server", CharsetUtil
                          .UTF_8));
                  e.ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                  return true;
              });
        latch.await(10, TimeUnit.SECONDS);
        server.close();
    }

    @Test
    public void testHttpShortcut() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpServer server = new HttpServer();
        server.bindAsync("localhost", 8080)
              .<ChannelFuture>then(future -> {
                  Assert.assertTrue(future.isSuccess());
                  HttpClient client = new HttpClient();
                  try {
                      client.requestAsync(new URI("http://localhost:8080"), makeRequest(HttpMethod.GET, "/", null, "Hello! from client"))
                            .<NetworkEvent<FullHttpResponse>>then(e -> {
                                System.out.println(e.msg.content().toString(CharsetUtil.UTF_8));
                                latch.countDown();
                                e.ctx.close();
                            });
                  } catch (URISyntaxException e) {
                      e.printStackTrace();
                  }
              })
              .<NetworkEvent<FullHttpRequest>>loop(e -> {
                  System.out.println(e.msg.content().toString(CharsetUtil.UTF_8));
                  FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer("World! from Server", CharsetUtil
                          .UTF_8));
                  e.ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                  return true;
              });
        latch.await(10, TimeUnit.SECONDS);
        server.close();
    }

    @Test
    public void testTCP() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TCPServer server = new TCPServer();
        server.bindAsync("localhost", 8080)
              .<ChannelFuture>then(future -> {
                  System.out.println("successfuly bind");
                  Assert.assertTrue(future.isSuccess());
                  TCPClient client = new TCPClient();
                  client.connectAsync("localhost", 8080)
                        .<ChannelFuture>then(future2 -> {
                            Assert.assertTrue(future2.isSuccess());
                            future2.channel().writeAndFlush(Unpooled.copiedBuffer("Hello! from client", CharsetUtil.UTF_8));
                        })
                        .<NetworkEvent<ByteBuf>>loop(e -> {
                            System.out.println(e.msg.toString(CharsetUtil.UTF_8));
                            e.ctx.close();
                            latch.countDown();
                            return true;
                        });
              })
              .<ConnectEvent>loop(e -> {
                  e.asyncStream.<NetworkEvent<ByteBuf>>loop(e2 -> {
                      System.out.println(e2.msg.toString(CharsetUtil.UTF_8));
                      e2.ctx.writeAndFlush(Unpooled.copiedBuffer("World! from Server", CharsetUtil.UTF_8)).addListener(ChannelFutureListener.CLOSE);
                      return true;
                  });
                  return true;
              }).end();

        latch.await(10, TimeUnit.SECONDS);
        server.close();
    }
}
