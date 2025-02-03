package dev.by1337.hider.util;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class TPSCounter {
    private static final DecimalFormat df = new DecimalFormat("#.##");
    private final TPS tpsPerSecond;
    private final TPS tpsPer30Seconds;
    private final TPS tpsPerMinute;
    private final TPS tpsPer5Minutes;
    private final TPS tpsPer15Minutes;
    private final long oneSecInNanos = TimeUnit.SECONDS.toNanos(1);


    public TPSCounter() {
        tpsPerSecond = new TPS(TimeUnit.SECONDS.toNanos(1));
        tpsPer30Seconds = new TPS(TimeUnit.SECONDS.toNanos(30));
        tpsPerMinute = new TPS(TimeUnit.MINUTES.toNanos(1));
        tpsPer5Minutes = new TPS(TimeUnit.MINUTES.toNanos(5));
        tpsPer15Minutes = new TPS(TimeUnit.MINUTES.toNanos(15));
    }

    public void tick() {
        long currentNanoTime = System.nanoTime();
        tpsPerSecond.tick(currentNanoTime);
        tpsPer30Seconds.tick(currentNanoTime);
        tpsPerMinute.tick(currentNanoTime);
        tpsPer5Minutes.tick(currentNanoTime);
        tpsPer15Minutes.tick(currentNanoTime);
    }

    public String tps() {
        return "1s:" + df.format(tpsPerSecond.tps) + " " +
                "30s:" + df.format(tpsPer30Seconds.tps) + " " +
                "1m:" + df.format(tpsPerMinute.tps) + " " +
                "5m:" + df.format(tpsPer5Minutes.tps) + " " +
                "15m:" + df.format(tpsPer15Minutes.tps);
    }

    public double getTpsPerSecond() {
        return tpsPerSecond.tps;
    }

    public double getTpsPer30Seconds() {
        return tpsPer30Seconds.tps;
    }

    public double getTpsPerMinute() {
        return tpsPerMinute.tps;
    }

    public double getTpsPer5Minutes() {
        return tpsPer5Minutes.tps;
    }

    public double getTpsPer15Minutes() {
        return tpsPer15Minutes.tps;
    }

    private class TPS {
        private int ticksCount = 0;
        private final long time;
        private double tps;
        private long startTime = System.nanoTime();

        public TPS(long time) {
            this.time = time;
        }

        private void tick(long currentNanoTime) {
            long elapsedTime = currentNanoTime - startTime;
            if (elapsedTime >= time) {
                tps = (double) ticksCount * oneSecInNanos / elapsedTime;
                ticksCount = 0;
                startTime = currentNanoTime;
            }
            ticksCount++;
        }
    }
}