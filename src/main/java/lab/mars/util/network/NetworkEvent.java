package lab.mars.util.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lab.mars.util.async.Cleanable;

/**
 * Created by haixiao on 2015/4/3.
 * Email: wumo@outlook.com
 */
public class NetworkEvent<T> implements Cleanable {
    public final ChannelHandlerContext ctx;
    public final T msg;

    public NetworkEvent(ChannelHandlerContext ctx, T msg) {
        this.ctx = ctx;
        this.msg = msg;
    }

    @Override
    public void clean() {
        ReferenceCountUtil.release(msg);
    }
}
