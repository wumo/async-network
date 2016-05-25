package lab.mars.util.network;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Created by haixiao on 2015/4/1.
 * Email: wumo@outlook.com
 */
public class Network {
    public static final int NCPU = Runtime.getRuntime().availableProcessors();
    public static final EventLoopGroup bossGroup;
    public static final EventLoopGroup workerGroup;

    static {
        bossGroup = new NioEventLoopGroup(NCPU);
        workerGroup = new NioEventLoopGroup(NCPU);
    }

    public static void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
