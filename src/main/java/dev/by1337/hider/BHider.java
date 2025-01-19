package dev.by1337.hider;

import dev.by1337.hider.network.PipelineHooker;
import org.bukkit.plugin.java.JavaPlugin;

public class BHider extends JavaPlugin {
    private PipelineHooker pipelineHooker;

    @Override
    public void onLoad() {
        super.onLoad();
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
