package com.polygamma.adsdemo.conf;

public class BenchmarkConfig {

    public enum Mode {
        ENCRYPT,
        DECRYPT,
        FULL
    }

    private final int n;
    private final int msgMod;
    private final int carryMod;
    private final int logNoiseB;
    private final int warmupIterations;
    private final int benchmarkIterations;
    private final Mode mode;

    public BenchmarkConfig(
            int n,
            int msgMod,
            int carryMod,
            int logNoiseB,
            int warmupIterations,
            int benchmarkIterations,
            Mode mode
    ) {
        this.n = n;
        this.msgMod = msgMod;
        this.carryMod = carryMod;
        this.logNoiseB = logNoiseB;
        this.warmupIterations = warmupIterations;
        this.benchmarkIterations = benchmarkIterations;
        this.mode = mode;
    }

    public int getN() {
        return n;
    }

    public int getMsgMod() {
        return msgMod;
    }

    public int getCarryMod() {
        return carryMod;
    }

    public int getLogNoiseB() {
        return logNoiseB;
    }

    public int getWarmupIterations() {
        return warmupIterations;
    }

    public int getBenchmarkIterations() {
        return benchmarkIterations;
    }

    public Mode getMode() {
        return mode;
    }
}