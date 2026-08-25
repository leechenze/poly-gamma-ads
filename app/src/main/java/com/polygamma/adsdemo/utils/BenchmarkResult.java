package com.polygamma.adsdemo.utils;

public class BenchmarkResult {

    private boolean correctnessPassed;

    private long minNs;
    private long maxNs;
    private double avgNs;
    private double p50Ns;
    private double p95Ns;

    private long totalNs;

    public boolean isCorrectnessPassed() {
        return correctnessPassed;
    }

    public void setCorrectnessPassed(boolean correctnessPassed) {
        this.correctnessPassed = correctnessPassed;
    }

    public long getMinNs() {
        return minNs;
    }

    public void setMinNs(long minNs) {
        this.minNs = minNs;
    }

    public long getMaxNs() {
        return maxNs;
    }

    public void setMaxNs(long maxNs) {
        this.maxNs = maxNs;
    }

    public double getAvgNs() {
        return avgNs;
    }

    public void setAvgNs(double avgNs) {
        this.avgNs = avgNs;
    }

    public double getP50Ns() {
        return p50Ns;
    }

    public void setP50Ns(double p50Ns) {
        this.p50Ns = p50Ns;
    }

    public double getP95Ns() {
        return p95Ns;
    }

    public void setP95Ns(double p95Ns) {
        this.p95Ns = p95Ns;
    }

    public long getTotalNs() {
        return totalNs;
    }

    public void setTotalNs(long totalNs) {
        this.totalNs = totalNs;
    }
}