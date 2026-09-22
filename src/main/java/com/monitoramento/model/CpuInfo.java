package com.monitoramento.model;

public class CpuInfo {
    private String nome;
    private double cpuLoad;

    public CpuInfo(String nome, double cpuLoad) {
        this.nome = nome;
        this.cpuLoad = cpuLoad;

    }

    public String getNome() {
        return nome;
    }

    public double getCpuLoad() {
        return cpuLoad;
    }


}
