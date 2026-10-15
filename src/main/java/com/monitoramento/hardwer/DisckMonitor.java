package com.monitoramento.hardwer;

import com.monitoramento.model.DiskInfo;
import com.monitoramento.model.VolumeInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisckMonitor {

    private Map<HWDiskStore, Long> prevRead = new HashMap<>();
    private Map<HWDiskStore, Long> prevWrite = new HashMap<>();
    private final OperatingSystem os;
    private final HardwareAbstractionLayer hal;


    public DisckMonitor( OperatingSystem os, HardwareAbstractionLayer hal){
        this.hal = hal;
        this.os = os;
    }

    public List<VolumeInfo> getVolume(){

        List<VolumeInfo> resultado = new ArrayList<>();
        List<OSFileStore> volumes = os.getFileSystem().getFileStores();
        for (OSFileStore volume : volumes){

            String nome = volume.getName();
            long espaco = volume.getTotalSpace() / (1024 * 1024 * 1024);
            long espacoLivre = volume.getFreeSpace() / (1024 * 1024 * 1024);
            long espacoUsado = espaco - espacoLivre;
            double porcentagemUso = ((double) espacoUsado / espaco) * 100;

            VolumeInfo info = new VolumeInfo(nome, espaco, espacoLivre,porcentagemUso);

            resultado.add(info);
        }

        return resultado;
    }

    public List<DiskInfo> getDiskUsage(){

        List<DiskInfo> resultado = new ArrayList<>();


        List<HWDiskStore> disks = hal.getDiskStores();
        for (HWDiskStore hDisk : disks){
            if (!prevRead.containsKey(hDisk)){
                prevRead.put(hDisk, hDisk.getReadBytes());
            }
            if (!prevWrite.containsKey(hDisk)){
                prevWrite.put(hDisk, hDisk.getWriteBytes());
            }
        }

        for (HWDiskStore hwdDisck : disks){

            String modelo = hwdDisck.getModel();

            hwdDisck.updateAttributes();
            long useReding = (hwdDisck.getReadBytes() - prevRead.get(hwdDisck));
            long useWrite = (hwdDisck.getWriteBytes() - prevWrite.get(hwdDisck));
            prevRead.replace(hwdDisck, hwdDisck.getReadBytes());
            prevWrite.replace(hwdDisck, hwdDisck.getWriteBytes());

            double porcReding = useReding / (1024.0 * 1024.0);
            double porcWrite = useWrite / (1024.0 * 1024.0);

            DiskInfo info = new DiskInfo(modelo,porcReding,porcWrite);

            resultado.add(info);

        }

        return resultado;
    }



}
