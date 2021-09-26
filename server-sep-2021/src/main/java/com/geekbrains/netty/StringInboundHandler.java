package com.geekbrains.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringInboundHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        log.debug("buf: {}", buf);
        StringBuilder s = new StringBuilder();
        while (buf.isReadable()) {
            s.append((char) buf.readByte());
        }
        log.debug("received: {}", s);
        ctx.fireChannelRead(s.toString());
    }
}
