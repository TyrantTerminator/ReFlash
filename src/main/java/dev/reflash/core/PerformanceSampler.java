package dev.reflash.core;

/**
 * Tiny FPS sampler: constant memory, no screenshots, no shader hooks, no timers.
 * Used for rough A/B comparison in the same world with the same settings.
 */
public final class PerformanceSampler {
    public record Result(int idleSamples, double idleFps, int recordingSamples, double recordingFps,
                         int lowestIdleSample, int lowestRecordingSample) {
        public boolean comparable() { return idleSamples >= 5 && recordingSamples >= 5; }
        public double relativeChangePercent() {
            return idleFps > 0 ? (recordingFps - idleFps) / idleFps * 100.0 : 0;
        }
    }

    private boolean active;
    private int ticks;
    private long idleFpsSum;
    private long recordingFpsSum;
    private int idleSamples;
    private int recordingSamples;
    private int idleMin=Integer.MAX_VALUE;
    private int recordingMin=Integer.MAX_VALUE;

    public void start() {
        active = true;
        ticks = 0;
        idleFpsSum = recordingFpsSum = 0;
        idleSamples = recordingSamples = 0;
        idleMin=recordingMin=Integer.MAX_VALUE;
    }

    public void stop() { active = false; }
    public boolean active() { return active; }

    public void tick(int fps, boolean recording) {
        if (!active) return;
        ticks++;
        if (ticks < 20) return;
        ticks = 0;
        if (fps <= 0 || fps > 5000) return;
        if (recording) {
            recordingFpsSum += fps;
            recordingMin=Math.min(recordingMin,fps);
            recordingSamples++;
        } else {
            idleFpsSum += fps;
            idleMin=Math.min(idleMin,fps);
            idleSamples++;
        }
    }

    public Result result() {
        return new Result(idleSamples, idleSamples > 0 ? (double) idleFpsSum / idleSamples : 0,
                recordingSamples, recordingSamples > 0 ? (double) recordingFpsSum / recordingSamples : 0,
                idleSamples>0?idleMin:0,recordingSamples>0?recordingMin:0);
    }
}
