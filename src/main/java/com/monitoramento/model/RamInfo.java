package com.monitoramento.model;

public class RamInfo {
    private long totaMemory;
    private long disponivelMemory;
    private long usageMemory;
    private double porctMemory;

    public RamInfo(long totaMemory, long disponivelMemory, long usageMemory, double porctMemory) {
        this.totaMemory = totaMemory;
        this.disponivelMemory =disponivelMemory;
        this.usageMemory = usageMemory;
        this.porctMemory = porctMemory;
    }

    public long getTotaMemory(){return totaMemory;}

    public long getDisponivelMemory() {
        return disponivelMemory;
    }

    public long getUsageMemory() {
        return usageMemory;
    }

    public double getPorctMemory() {
        return porctMemory;
    }
}
