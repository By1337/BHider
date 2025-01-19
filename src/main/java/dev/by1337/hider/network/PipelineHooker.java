package dev.by1337.hider.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.Closeable;
// client <- prepender <- compress <- encoder
public class PipelineHooker implements Listener, Closeable {
    private final Plugin plugin;

    public PipelineHooker(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getOnlinePlayers().forEach(this::hook);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void hook(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        if (channel.pipeline().get(OutPacketListener.NAME) != null)
            unhook(player);
        channel.pipeline().addAfter("compress", OutPacketListener.NAME, new OutPacketListener(player, plugin));
        StringBuilder sb = new StringBuilder();
        channel.pipeline().forEach(e -> {
            if (e.getValue() instanceof ChannelOutboundHandlerAdapter)
                sb.append(e.getKey()).append(" <- ");
        });
        sb.append("client");
        System.out.println(sb);

    }

    public boolean isHooked(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        return serverPlayer.playerConnection.networkManager.channel.pipeline().get(OutPacketListener.NAME) != null;
    }

    private void unhook(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        if (channel.pipeline().get(OutPacketListener.NAME) == null) return;
        var handler = channel.pipeline().remove(OutPacketListener.NAME);
        if (handler instanceof OutPacketListener opl) opl.close();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
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
