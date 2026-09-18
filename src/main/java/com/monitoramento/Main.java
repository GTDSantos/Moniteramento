package com.monitoramento;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OSFileStore;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class    Main {



    static void main() {
        SystemInfoProvider si = SystemInfoFactory.create();
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();
        CentralProcessor processor = hal.getProcessor();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        GlobalMemory memory = hal.getMemory();
        List<HWDiskStore> diskStorage = hal.getDiskStores();

        System.out.println("A maquina esta ligada a " + os.getSystemUptime()/60 + " minutos ");
        System.out.println(processor);
        System.out.println(processor.getPhysicalProcessorCount());

        System.out.println("--------RAM--------------");
        System.out.println("memoria total: " + memory.getTotal() / (1024 * 1024));
        System.out.println("memoria disponivel : " + memory.getAvailable() / (1024 * 1024));
        System.out.println("memoria usada: " + (memory.getTotal() - memory.getAvailable()) / (1024 * 1024));
        System.out.println("Porcentagem de RAM usada: " + ((double)(memory.getTotal() - memory.getAvailable())) / (memory.getTotal()) * 100);

        System.out.println("---------DISCO--------------");
        System.out.println(diskStorage.isEmpty());
        for (HWDiskStore disk : diskStorage){
            System.out.println(disk.);
            System.out.println("tamanho total: " + disk.getModel());
            System.out.println(disk.getSize() / (1024 * 1024 * 1024));
        }




        /*scheduler.scheduleAtFixedRate(()->{
            long[] prevTick = processor.getSystemCpuLoadTicks();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTick) * 100;
            System.out.println(cpuLoad);
        },1,1, TimeUnit.SECONDS);*/



    }

}
