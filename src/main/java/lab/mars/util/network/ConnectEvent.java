package lab.mars.util.network;

import io.netty.channel.Channel;
import lab.mars.util.async.AsyncStream;

/**
 * Created by haixiao on 2015/4/6.
 * Email: wumo@outlook.com
 */
public class ConnectEvent {
    public final Channel channel;
    public final AsyncStream asyncStream;

    public ConnectEvent(Channel channel, AsyncStream asyncStream) {
        this.channel = channel;
        this.asyncStream = asyncStream;
    }
}
