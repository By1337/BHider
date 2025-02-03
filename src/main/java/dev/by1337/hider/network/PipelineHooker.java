package dev.by1337.hider.network;

import com.destroystokyo.paper.event.player.PlayerInitialSpawnEvent;
import dev.by1337.hider.config.Config;
import dev.by1337.hider.shapes.BlockShapes;
import dev.by1337.hider.ticker.Ticker;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.Closeable;

// client <- prepender <- compress <- encoder
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
        Channel channel = ((CraftPlayer) player).getHandle().networkManager.channel;
        if (channel.pipeline().get(OutPacketListener.NAME) != null)
            unhook(player);
        // client <- prepender <- compress <- bhider_encoder <- bhider_listener <- via-encoder <- encoder <- generate packet
        channel.pipeline().addBefore("encoder", OutPacketListener.NAME, new OutPacketListener(player, plugin, channel, config, blockShapes, ticker));
        channel.pipeline().addBefore(OutPacketListener.NAME, CustomPacketEncoder.NAME, new CustomPacketEncoder());
        StringBuilder sb = new StringBuilder();
        sb.append("client <- ");
        channel.pipeline().forEach(e -> {
            if (e.getValue() instanceof ChannelOutboundHandlerAdapter)
                sb.append(e.getKey()).append(" <- ");
        });
        sb.append("generate packet");
        System.out.println(sb);
    }

    public boolean isHooked(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        return serverPlayer.playerConnection.networkManager.channel.pipeline().get(OutPacketListener.NAME) != null;
    }

    private void unhook(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        channel.pipeline().forEach(e -> System.out.println(e.getKey()));
        if (channel.pipeline().get(OutPacketListener.NAME) == null) return;
        var handler = channel.pipeline().remove(OutPacketListener.NAME);
        if (handler instanceof OutPacketListener opl) opl.close();
        channel.pipeline().remove(CustomPacketEncoder.NAME);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
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
