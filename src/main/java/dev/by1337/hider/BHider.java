package dev.by1337.hider;

import dev.by1337.hider.config.Config;
import dev.by1337.hider.network.PipelineHooker;
import dev.by1337.hider.shapes.BlockShapes;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.util.ResourceUtil;
import org.jetbrains.annotations.NotNull;

public class BHider extends JavaPlugin {
    private PipelineHooker pipelineHooker;

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnable() {
        Config config = ResourceUtil.load("config.yml", this).get().decode(Config.CODEC).getOrThrow().getFirst();
        BlockShapes blockShapes = new BlockShapes(config.ignoreBlocks);
        pipelineHooker = new PipelineHooker(this, config, blockShapes);
    }

    @Override
    public void onDisable() {
        pipelineHooker.close();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        Player player = (Player) sender;
        World world = player.getWorld();

        return true;
    }

}
