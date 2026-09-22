package com.monitoramento;

import com.monitoramento.hardwer.DisckMonitor;
import com.monitoramento.hardwer.NetMonitor;
import com.monitoramento.model.DiskInfo;
import com.monitoramento.model.NetInfo;
import com.monitoramento.model.VolumeInfo;
import oshi.hardware.*;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OSFileStore;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        List<OSFileStore> listDisck = os.getFileSystem().getFileStores();
        List<HWDiskStore> hListDisck = hal.getDiskStores();
        List<NetworkIF> hListNet = hal.getNetworkIFs();



        System.out.println("---------PROCESSOS------------");

        List<OSProcess> prevCpuProcess = os.getProcesses();
        Map<Integer, OSProcess> prevProcess = new HashMap<>();
        for (OSProcess process : prevCpuProcess){
            prevProcess.put(process.getProcessID(),process);

        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<OSProcess> listProcess = os.getProcesses();
        for (OSProcess process : listProcess){

            System.out.println("nome: " + process.getName());
            System.out.println("ID: " + process.getProcessID());
            System.out.println("Memoria usada: " + process.getResidentMemory() / (1024 * 1024));
            System.out.println("Cpu usada: " + (process.getProcessCpuLoadBetweenTicks(prevProcess.get(process.getProcessID())) / processor.getLogicalProcessorCount() * 100 ) );


            System.out.println();
        }



        System.out.println("-----------------");
        DisckMonitor disck = new DisckMonitor(listDisck,hListDisck);
        List<VolumeInfo> volume = disck.getVolume();

        for (VolumeInfo volumes : volume){
            System.out.println(volumes.getNome());
            System.out.println(volumes.getTotal());
            System.out.println(volumes.getLivre());
        }

        System.out.println();




        /*scheduler.scheduleAtFixedRate(()-> {
            List<DiskInfo> diskI = disck.getDiskUsage();
            for (DiskInfo disk : diskI) {
                System.out.println(disk.getModelo());
                System.out.println(disk.getRead());
                System.out.println(disk.getWrite());
            }
        },1,1,TimeUnit.SECONDS);*/

        NetMonitor net = new NetMonitor(hListNet);

        scheduler.scheduleAtFixedRate(() -> {
            List<NetInfo> listNet = net.getNetInfo();
            for (NetInfo lNet : listNet){
                System.out.println(lNet.getDisplay());
                System.out.println(lNet.getIp());
                System.out.println(lNet.getRecv());
                System.out.println(lNet.getSent());
                System.out.println();
                System.out.println();
            }
        },1,1,TimeUnit.SECONDS);





    }

}
