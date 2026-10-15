package com.monitoramento.hardwer;

import com.monitoramento.model.RamInfo;
import oshi.hardware.GlobalMemory;

import java.util.ArrayList;
import java.util.List;

public class RamMonitor {
    private final GlobalMemory memory;

    public RamMonitor(GlobalMemory memory){
        this.memory = memory;
    }


    public RamInfo getRamInfo() {
        double totalMemory = memory.getTotal() / (1024.0 * 1024 * 1024);
        double diponivelMemory = memory.getAvailable() / (1024.0 * 1024 * 1024);

        double usageMemory =
                (memory.getTotal() - memory.getAvailable()) / (1024.0 * 1024 * 1024);

        double porcentMemory =
                ((double) (memory.getTotal() - memory.getAvailable()))
                        / memory.getTotal() * 100;

        RamInfo info = new RamInfo(
                totalMemory,
                diponivelMemory,
                usageMemory,
                porcentMemory
        );

        return info;
    }


}
