package com.monitoramento.hardwer;

import com.monitoramento.model.*;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.spi.SystemInfoProvider;
import java.net.InetAddress;
import java.util.List;

public class Monitoramento {

    private CpuMonitor cpuMonitor;
    private DisckMonitor disckMonitor;
    private NetMonitor netMonitor;
    private ProcessosMonitor processosMonitor;
    private RamMonitor ramMonitor;
    private final OperatingSystem os;
    public Monitoramento(SystemInfoProvider si){
        HardwareAbstractionLayer hal = si.getHardware();
        this.os = si.getOperatingSystem();

        cpuMonitor = new CpuMonitor(hal.getProcessor());
        disckMonitor = new DisckMonitor(this.os, hal);
        ramMonitor = new RamMonitor(hal.getMemory());
        processosMonitor = new ProcessosMonitor(
                this.os,
                hal.getProcessor()
        );
        netMonitor = new NetMonitor(hal);
    }

    public CpuInfo getCpu(){
        return cpuMonitor.getCpuInfos();
    }

    public List<DiskInfo> getDiskUsage(){
        return disckMonitor.getDiskUsage();
    }

    public  List<VolumeInfo> getDiskVolume(){
        return disckMonitor.getVolume();
    }

    public List<NetInfo> getNet(){
        return netMonitor.getNetInfo();
    }

    public List<ProcesInfo> getProces(){
        return processosMonitor.getProcesInfo();
    }

    public RamInfo getRam(){
        return ramMonitor.getRamInfo();
    }
    // Tempo, em segundos, desde a inicialização do sistema.
    public long getSystemUptimeSeconds() {
        return Math.max(0, os.getSystemUptime());
    }

    // Nome real do computador.
    public String getNomeComputador() {

        try {
            String nome = os.getNetworkParams().getHostName();

            if (nome != null && !nome.isBlank()) {
                return nome;
            }

        } catch (Exception ignored) {
            // Tenta as alternativas abaixo.
        }

        String nomeWindows = System.getenv("COMPUTERNAME");

        if (nomeWindows != null && !nomeWindows.isBlank()) {
            return nomeWindows;
        }

        try {
            String nome = InetAddress.getLocalHost().getHostName();

            if (nome != null && !nome.isBlank()) {
                return nome;
            }

        } catch (Exception ignored) {
            // Não foi possível encontrar o nome.
        }

        return "Meu computador";
    }
}
