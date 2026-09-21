package com.monitoramento.hardwer;

import oshi.hardware.CentralProcessor;
import oshi.software.os.OperatingSystem;


public class CpuMonitor {

    private OperatingSystem os;
    private CentralProcessor processor;
    private long[] prevTick = processor.getSystemCpuLoadTicks();

    public CpuMonitor(OperatingSystem os,CentralProcessor processor) {
        this.processor = processor;
        this.os = os;
    }

    String getNome(){
        return processor.getProcessorIdentifier().getName();
    }

    double getCpuUsage() throws InterruptedException {

        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTick) * 100;
        long[] prevTick = processor.getSystemCpuLoadTicks();
        return cpuLoad;
    }
    long getTimeOn() {
        return os.getSystemBootTime()/60;
    }

}


