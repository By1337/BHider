package dev.by1337.hider;

import dev.by1337.hider.config.Config;
import dev.by1337.hider.metrics.Metrics;
import dev.by1337.hider.network.PipelineHooker;
import dev.by1337.hider.shapes.BlockShapes;
import dev.by1337.hider.ticker.Ticker;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.BLib;
import org.by1337.blib.command.Command;
import org.by1337.blib.command.CommandWrapper;
import org.by1337.blib.command.requires.RequiresPermission;
import org.by1337.blib.util.ResourceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BHider extends JavaPlugin {
    private PipelineHooker pipelineHooker;
    private Ticker ticker;
    private CommandWrapper commandWrapper;
    private Metrics metrics;

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnable() {
        metrics = new Metrics(this, 24652);
        Config config = ResourceUtil.load("config.yml", this).get().decode(Config.CODEC).getOrThrow().getFirst();
        BlockShapes blockShapes = new BlockShapes(config.ignoreBlocks);

        commandWrapper = new CommandWrapper(createCommand(), this);
        commandWrapper.setPermission("bhider.admin");
        commandWrapper.register();

        ticker = new Ticker();
        var t = new Thread(ticker::start);
        t.setName("bhider-ticker");
        t.start();
        pipelineHooker = new PipelineHooker(this, config, blockShapes, ticker);
    }

    @Override
    public void onDisable() {
        commandWrapper.close();
        pipelineHooker.close();
        ticker.stop();
        metrics.shutdown();
    }

    private Command<CommandSender> createCommand() {
        return new Command<CommandSender>("bhider")
                .requires(new RequiresPermission<>("bhider.admin"))
                .aliases("bh")
                .addSubCommand(new Command<CommandSender>("tps")
                        .requires(new RequiresPermission<>("bhider.admin.tps"))
                        .executor(((sender, args) -> {
                            sender.sendMessage(ticker.tps());
                        }))
                ).addSubCommand(new Command<CommandSender>("tickTime")
                        .requires(new RequiresPermission<>("bhider.admin.tickTime"))
                        .executor(((sender, args) -> {
                            sender.sendMessage(ticker.lastTickTime() + " ms");
                        }))
                ).addSubCommand(new Command<CommandSender>("pipeline")
                        .requires(new RequiresPermission<>("bhider.admin.pipeline"))
                        .executor(((sender, args) -> {
                            ServerPlayer player = ((CraftPlayer) sender).getHandle();
                            Channel channel = player.networkManager.channel;

                            List<String> list = new ArrayList<>();
                            channel.pipeline().forEach(e -> {
                                if (e.getValue() instanceof ChannelOutboundHandlerAdapter)
                                    list.add(e.getKey());
                            });
                            Collections.reverse(list);

                            StringBuilder sb = new StringBuilder();
                            sb.append("<server> -> ");
                            for (String s : list) {
                                sb.append(s).append(" -> ");
                            }
                            sb.append("<client>");
                            sender.sendMessage(sb.toString());
                            System.out.println(sb);
                        }))
                ).addSubCommand(new Command<CommandSender>("mem")
                        .requires(new RequiresPermission<>("bhider.admin.mem"))
                        .executor(((sender, args) -> {
                            int count = 0;
                            long size = 0;
                            for (Runnable task : ticker.tasks()) {
                                if (task instanceof PlayerController pc) {
                                    count++;
                                    size += pc.level.sizeOf();
                                }
                            }
                            double sizeMb = size / (1024.0 * 1024.0);
                            double avgSizeMb = count > 0 ? sizeMb / count : 0.0;

                            BLib.getApi().getMessage().sendMsg(
                                    sender,
                                    "Количество игроков {}, На виртуальные миры затрачено {}mb памяти. На одного игрока приходится в среднем {}mb.",
                                    count, String.format("%.2f", sizeMb), String.format("%.2f", avgSizeMb)
                            );
                        }))
                )
                ;
    }
}
