package com.monitoramento.hardwer;

import oshi.hardware.CentralProcessor;
import oshi.software.os.OperatingSystem;


public class CpuMonitor {

    private OperatingSystem os;
    private CentralProcessor processor;
    private long[] prevTick;

    public CpuMonitor(OperatingSystem os,CentralProcessor processor) {
        this.processor = processor;
        this.os = os;

        prevTick = processor.getSystemCpuLoadTicks();
    }


    String getNome(){
        return processor.getProcessorIdentifier().getName();
    }

    double getCpuUsage() throws InterruptedException {

        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTick) * 100;
        prevTick = processor.getSystemCpuLoadTicks();
        return cpuLoad;
    }

}


