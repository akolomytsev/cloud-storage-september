package com.geekbrains.netty;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.geekbrains.Command;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path ROOT = Paths.get("server-sep-2021", "root");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
        // TODO: 23.09.2021 Разработка системы команд
//        Files.write(
//                ROOT.resolve(fileMessage.getName()),
//                fileMessage.getBytes()
//        );
//
//        ctx.writeAndFlush("OK");
        switch (cmd.getType()) {

        }

    }
}
