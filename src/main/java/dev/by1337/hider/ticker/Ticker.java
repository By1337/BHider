package dev.by1337.hider.ticker;

import dev.by1337.hider.util.TPSCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Ticker {
    public static final int TPS = 20;
    public static final int TICK_TIME_MILS = 1000 / TPS;
    private static final Logger LOGGER = LoggerFactory.getLogger("BHider#Ticker");
    private final List<Runnable> tasks = new CopyOnWriteArrayList<>();
    private long nextTick;
    private long lastOverloadTime;
    private volatile boolean stopped;
    private final TPSCounter tpsCounter = new TPSCounter();
    private long lastTickTime = 0;

    public void start() {
        nextTick = getMonotonicMillis();
        try {
            while (!stopped) {
                long i = (System.nanoTime() / (1000L * 1000L)) - nextTick;

                if (i > 5000L && nextTick - lastOverloadTime >= 30_000L) {
                    long j = i / TICK_TIME_MILS;

                    LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", i, j);

                    nextTick += j * TICK_TIME_MILS;
                    lastOverloadTime = nextTick;
                }
                nextTick += TICK_TIME_MILS;

                tpsCounter.tick();
                long startTime = System.nanoTime();
                for (Runnable task : tasks) {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        LOGGER.error("Failed to run task {}", task, t);
                    }
                }
                lastTickTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

                while (getMonotonicMillis() < nextTick) {
                    LockSupport.parkNanos(1000L);
                }
            }
        } catch (Throwable throwable) {
            stopped = true;
            LOGGER.error("Fatal error", throwable);
        }
    }

    public static long getMonotonicMillis() {
        return System.nanoTime() / 1_000_000L;
    }
    public void addTask(Runnable task) {
        tasks.add(task);
    }
    public void removeTask(Runnable task) {
        tasks.remove(task);
    }
    public void stop() {
        stopped = true;
    }

    public String tps() {
        return tpsCounter.tps();
    }

    public long lastTickTime() {
        return lastTickTime;
    }

    public List<Runnable> tasks() {
        return tasks;
    }
}
