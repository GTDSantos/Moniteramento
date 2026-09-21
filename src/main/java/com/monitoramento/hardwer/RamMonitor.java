package com.monitoramento.hardwer;

import oshi.hardware.GlobalMemory;

public class RamMonitor {
    private GlobalMemory memory;

    public RamMonitor(GlobalMemory memory){
        this.memory = memory;
    }

    long getTotalMemory(){
        return memory.getTotal() / (1024 * 1024 * 1024);
    }

    long getDisponivelMemory(){
        return memory.getAvailable() / (1024 * 1024 * 1024);
    }

    long getUsageMemory(){
        return (memory.getTotal() - memory.getAvailable()) / (1024 * 1024 * 1024);
    }
    double getPorcentMemorry(){
        return ((double)(memory.getTotal() - memory.getAvailable())) / (memory.getTotal()) * 100;
    }
}
