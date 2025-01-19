package dev.by1337.hider.network;

import io.netty.channel.Channel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.io.Closeable;

public class PipelineHooker implements Listener, Closeable {
    private final Plugin plugin;

    public PipelineHooker(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getOnlinePlayers().forEach(this::hook);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void hook(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        if (channel.pipeline().get(OutPacketListener.NAME) != null){
            channel.pipeline().remove(OutPacketListener.NAME);
        }
        channel.pipeline().addBefore("compress", OutPacketListener.NAME, new OutPacketListener());
    }

    public boolean isHooked(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        return serverPlayer.playerConnection.networkManager.channel.pipeline().get(OutPacketListener.NAME) != null;
    }

    private void unhook(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        serverPlayer.playerConnection.networkManager.channel.pipeline().remove(OutPacketListener.NAME);
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        hook(event.getPlayer());
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
        Bukkit.getOnlinePlayers().forEach(this::unhook);
    }
}
