package com.monitoramento;

import com.monitoramento.hardwer.DisckMonitor;
import com.monitoramento.hardwer.DiskInfo;
import com.monitoramento.hardwer.VolumeInfo;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OSFileStore;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;

import java.util.Arrays;
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

        NetworkParams netParams = os.getNetworkParams();
        List<NetworkIF> ListNet = hal.getNetworkIFs();


        System.out.println("-------------NET--------------");
        Map<NetworkIF, Long> prevReceived = new HashMap<>();
        Map<NetworkIF, Long> prevSent = new HashMap<>();
        for (NetworkIF halNet : ListNet){
            prevReceived.put(halNet,halNet.getBytesRecv());
            prevSent.put(halNet, halNet.getBytesSent());
        }

        for (NetworkIF halNet : ListNet){
            System.out.println("nome da rede : " + halNet.getName());
            System.out.println("display da rede: " + halNet.getDisplayName());
            System.out.println("MAC: " + halNet.getMacaddr());
            for (String ip : halNet.getIPv4addr()){
                System.out.println("IP: " + ip);
            }
            if (halNet.getIfOperStatus() == NetworkIF.IfOperStatus.UP){
                System.out.println("Status: Ativo");
            }else {
                System.out.println("Status: inativo");
            }

            halNet.updateAttributes();
            double receivad = (halNet.getBytesRecv() - prevReceived.get(halNet)) / (1024 * 1024);
            double sent = (halNet.getBytesSent() - prevSent.get(halNet)) / (1024 * 1024);
        }
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




        scheduler.scheduleAtFixedRate(()-> {
            List<DiskInfo> diskI = disck.getDiskUsage();
            for (DiskInfo disk : diskI) {
                System.out.println(disk.getModelo());
                System.out.println(disk.getRead());
                System.out.println(disk.getWrite());
            }
        },1,1,TimeUnit.SECONDS);





    }

}
