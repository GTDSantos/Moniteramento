package com.monitoramento.model;

public class VolumeInfo {
    private String nome;
    private long total;
    private long livre;
    private double porct;

    public VolumeInfo(String nome, long total, long livre, double porct) {
        this.nome = nome;
        this.total = total;
        this.livre = livre;
        this.porct = porct;
    }

    public String getNome() {
        return nome;
    }

    public long getTotal() {
        return total;
    }

    public long getLivre() {
        return livre;
    }

    public double getPorctVolume(){
        return porct;
    }
}
