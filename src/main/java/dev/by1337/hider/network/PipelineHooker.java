package dev.by1337.hider.network;

import com.destroystokyo.paper.event.player.PlayerInitialSpawnEvent;
import dev.by1337.hider.config.Config;
import dev.by1337.hider.shapes.BlockShapes;
import dev.by1337.hider.ticker.Ticker;
import io.netty.channel.Channel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.Closeable;
import java.util.List;

public class PipelineHooker implements Listener, Closeable {
    private final Plugin plugin;
    private final Config config;
    private final BlockShapes blockShapes;
    private final Ticker ticker;

    public PipelineHooker(Plugin plugin, Config config, BlockShapes blockShapes, Ticker ticker) {
        this.plugin = plugin;
        this.config = config;
        this.blockShapes = blockShapes;
        this.ticker = ticker;
        Bukkit.getOnlinePlayers().forEach(this::hook);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void hook(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        if (serverPlayer == null || serverPlayer.networkManager == null) return;
        Channel channel = serverPlayer.networkManager.channel;

        if (channel.pipeline().get(OutPacketListener.NAME) != null)
            unhook(player);

        List<String> handlers = channel.pipeline().names();
        int index = handlers.lastIndexOf("compress");
        String lastEncoder;

        if (index == -1) {
            lastEncoder = "encoder";
        } else {
            lastEncoder = handlers.get(index + 1);
        }
        channel.pipeline().addBefore(lastEncoder, OutPacketListener.NAME, new OutPacketListener(player, plugin, channel, config, blockShapes, ticker));
        channel.pipeline().addBefore(OutPacketListener.NAME, CustomPacketEncoder.NAME, new CustomPacketEncoder());
    }


    private void unhook(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        if (serverPlayer == null || serverPlayer.networkManager == null) return;
        Channel channel = serverPlayer.networkManager.channel;
        if (channel.pipeline().get(OutPacketListener.NAME) == null) return;
        var handler = channel.pipeline().remove(OutPacketListener.NAME);
        if (handler instanceof OutPacketListener opl) opl.close();
        channel.pipeline().remove(CustomPacketEncoder.NAME);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    @SuppressWarnings("deprecation")
    public void onJoin(PlayerInitialSpawnEvent event) {
        hook(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        unhook(event.getPlayer());
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
        Bukkit.getOnlinePlayers().forEach(this::unhook);
    }
}
