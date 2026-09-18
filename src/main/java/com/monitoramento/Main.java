package com.monitoramento;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {



    static void main() {
        SystemInfoProvider si = SystemInfoFactory.create();
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();
        CentralProcessor processor = hal.getProcessor();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        System.out.println("A maquina esta ligada a " + os.getSystemUptime()/60 + " minutos ");
        System.out.println(processor);
        System.out.println(processor.getPhysicalProcessorCount());



        scheduler.scheduleAtFixedRate(()->{
            long[] prevTick = processor.getSystemCpuLoadTicks();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTick) * 100;
            System.out.println(cpuLoad);
        },1,1, TimeUnit.SECONDS);


    }

}
