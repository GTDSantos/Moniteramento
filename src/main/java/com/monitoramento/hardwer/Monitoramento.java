package com.monitoramento.hardwer;

import com.monitoramento.model.*;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.spi.SystemInfoProvider;

import java.util.List;

public class Monitoramento {

    private CpuMonitor cpuMonitor;
    private DisckMonitor disckMonitor;
    private NetMonitor netMonitor;
    private ProcessosMonitor processosMonitor;
    private RamMonitor ramMonitor;

    public Monitoramento(SystemInfoProvider si){
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        cpuMonitor = new CpuMonitor(hal.getProcessor());
        disckMonitor = new DisckMonitor(os,hal);
        ramMonitor = new RamMonitor(hal.getMemory());
        processosMonitor = new ProcessosMonitor(os, hal.getProcessor());
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
}
