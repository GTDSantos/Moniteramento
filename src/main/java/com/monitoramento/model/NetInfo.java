package com.monitoramento.model;

import java.util.List;

public class NetInfo {
    private String nome;
    private String display;
    private String mac;
    private List<String> ip;
    private String status;
    private double recv;
    private double sent;


    public NetInfo(String nome,String display, String mac, List<String> ip, String status, double recv, double sent) {
        this.nome = nome;
        this.display = display;
        this.mac = mac;
        this.ip = ip;
        this.status = status;
        this.recv = recv;
        this.sent = sent;
    }

    public String getNome() {
        return nome;
    }

    public String getDisplay(){
        return display;
    }

    public String getMac(){
        return mac;
    }

    public List<String> getIp(){
        return ip;
    }

    public String getStatus(){
        return status;
    }

    public double getRecv() {
        return recv;
    }

    public double getSent() {
        return sent;
    }
}
