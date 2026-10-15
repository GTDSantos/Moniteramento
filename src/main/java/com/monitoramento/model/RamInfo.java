package com.monitoramento.model;

public class RamInfo {
    private double totaMemory;
    private double disponivelMemory;
    private double usageMemory;
    private double porctMemory;

    public RamInfo(double totaMemory, double disponivelMemory, double usageMemory, double porctMemory) {
        this.totaMemory = totaMemory;
        this.disponivelMemory = disponivelMemory;
        this.usageMemory = usageMemory;
        this.porctMemory = porctMemory;
    }


    public double getTotaMemory() {
        return totaMemory;
    }

    public double getDisponivelMemory() {
        return disponivelMemory;
    }

    public double getUsageMemory() {
        return usageMemory;
    }

    public double getPorctMemory() {
        return porctMemory;
    }
}
