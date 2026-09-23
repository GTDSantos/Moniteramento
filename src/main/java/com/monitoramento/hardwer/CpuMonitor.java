package com.monitoramento.hardwer;

import com.monitoramento.model.CpuInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.List;


public class CpuMonitor {

    private final CentralProcessor processor;
    private long[] prevTick;

    public CpuMonitor(CentralProcessor processor) {
        this.processor = processor;

        prevTick = processor.getSystemCpuLoadTicks();
    }


    public CpuInfo getCpuInfos() {
        String nome = processor.getProcessorIdentifier().getName();
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTick) * 100;
        prevTick = processor.getSystemCpuLoadTicks();
        return new CpuInfo(nome, cpuLoad);
    }

}


