package com.monitoramento.hardwer;

import com.monitoramento.model.NetInfo;
import oshi.hardware.NetworkIF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetMonitor {

    Map<NetworkIF, Long> prevReceived = new HashMap<>();
    Map<NetworkIF, Long> prevSent = new HashMap<>();
    List<NetworkIF> listNet;





    public NetMonitor(List<NetworkIF> listNet ){
        this.listNet = listNet;
    }

    public List<NetInfo> getNetInfo() {
        List<NetInfo> resultado = new ArrayList<>();

        for (NetworkIF hNet : listNet){
            if (!prevReceived.containsKey(hNet)){
                prevReceived.put(hNet, hNet.getBytesRecv());
            }
            if (!prevSent.containsKey(hNet)){
                prevSent.put(hNet, hNet.getBytesSent());
            }
        }

        for (NetworkIF hNet : listNet){

            String nome = hNet.getName();
            String display = hNet.getDisplayName();
            String mac = hNet.getMacaddr();
            List<String> ips = new ArrayList<>();
            for (String ip : hNet.getIPv4addr()){
                ips.add(ip);
            }

            String status = null;
            if (hNet.getIfOperStatus() == NetworkIF.IfOperStatus.UP){
                status = "ativo";
            }else {
                status = "inativo";
            }


            hNet.updateAttributes();
            long receivad = (hNet.getBytesRecv() - prevReceived.get(hNet));
            long sent = (hNet.getBytesSent() - prevSent.get(hNet));
            prevReceived.replace(hNet, hNet.getBytesRecv());
            prevSent.replace(hNet, hNet.getBytesSent());

            double porcrecv = receivad / (1024.0 * 1024.0);
            double porcsent = sent / (1024.0 * 1024.0);

            NetInfo info = new NetInfo(nome,display,mac,ips,status,porcrecv,porcsent);

            resultado.add(info);

        }
        return resultado;
    }

}
