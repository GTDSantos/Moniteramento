package com.monitoramento.hardwer;

public class VolumeInfo {
    private String nome;
    private long total;
    private long livre;

    public VolumeInfo(String nome, long total, long livre) {
        this.nome = nome;
        this.total = total;
        this.livre = livre;
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
}
