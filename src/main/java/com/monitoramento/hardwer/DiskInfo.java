package com.monitoramento.hardwer;
public class DiskInfo {
    private String modelo;
    private double read;
    private double write;

    public DiskInfo(String modelo, double read, double write) {
        this.modelo = modelo;
        this.read = read;
        this.write = write;
    }

    public String getModelo() {
            return modelo;
    }

    public double getRead() {
        return read;
    }

    public double getWrite() {
        return write;
    }
}
