package com.monitoramento.model;

public class ProcesInfo {
    private String nome;
    private int id;
    private long memory;
    private double cpu;

    public ProcesInfo(String nome, int id, long memory, double cpu) {
        this.nome = nome;
        this.id = id;
        this.memory =memory;
        this.cpu = cpu;
    }

    public String getNome() {
        return nome;
    }

    public int getId() {
        return id;
    }

    public long getMemory() {
        return memory;
    }

    public double getCpu(){return cpu;}
}
