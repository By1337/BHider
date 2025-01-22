package dev.by1337.hider;

import dev.by1337.hider.network.PipelineHooker;
import dev.by1337.hider.shapes.BlockShapes;
import org.bukkit.plugin.java.JavaPlugin;

public class BHider extends JavaPlugin {
    private PipelineHooker pipelineHooker;

    @Override
    public void onLoad() {
        super.onLoad();
        BlockShapes.load();
    }

    @Override
    public void onEnable() {
        pipelineHooker = new PipelineHooker(this);
    }

    @Override
    public void onDisable() {
        pipelineHooker.close();
    }

}
