
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

    private final Map<String, Long> prevRead = new HashMap<>();
    private final Map<String, Long> prevWrite = new HashMap<>();
    private final Map<String, Long> prevTime = new HashMap<>();

    private final OperatingSystem os;
    private final HardwareAbstractionLayer hal;

    private static final double MB = 1024.0 * 1024.0;
    private static final long GB = 1024L * 1024L * 1024L;

    public DisckMonitor(
            OperatingSystem os,
            HardwareAbstractionLayer hal
    ) {
        this.hal = hal;
        this.os = os;
    }

    // Obtém os volumes lógicos, como C: e D:
    public List<VolumeInfo> getVolume() {

        List<VolumeInfo> resultado = new ArrayList<>();

        List<OSFileStore> volumes =
                os.getFileSystem().getFileStores();

        for (OSFileStore volume : volumes) {

            String nome = volume.getName();

            long totalBytes = Math.max(0, volume.getTotalSpace());

            long livreBytes = Math.max(
                    0,
                    Math.min(totalBytes, volume.getFreeSpace())
            );

            long usadoBytes = totalBytes - livreBytes;

            long totalGB = totalBytes / GB;
            long livreGB = livreBytes / GB;

            double porcentagemUso = totalBytes > 0
                    ? (double) usadoBytes / totalBytes * 100.0
                    : 0.0;

            VolumeInfo info = new VolumeInfo(
                    nome,
                    totalGB,
                    livreGB,
                    porcentagemUso
            );

            resultado.add(info);
        }

        return resultado;
    }

    // Obtém a atividade dos discos físicos em MB/s
    public List<DiskInfo> getDiskUsage() {

        List<DiskInfo> resultado = new ArrayList<>();

        List<HWDiskStore> discos = hal.getDiskStores();

        for (int i = 0; i < discos.size(); i++) {

            HWDiskStore disco = discos.get(i);

            // Atualiza os contadores de leitura e escrita.
            disco.updateAttributes();

            String chave = disco.getName();

            if (chave == null || chave.isBlank()) {
                chave = "disco-" + i + "-" + disco.getModel();
            }

            long leituraAtual = disco.getReadBytes();
            long escritaAtual = disco.getWriteBytes();
            long agora = System.nanoTime();

            double leituraMBs = 0;
            double escritaMBs = 0;

            // Confere se já existe uma medição anterior.
            if (prevRead.containsKey(chave)
                    && prevWrite.containsKey(chave)
                    && prevTime.containsKey(chave)) {

                long intervaloNanos = agora - prevTime.get(chave);

                if (intervaloNanos > 0) {

                    double intervaloSegundos =
                            intervaloNanos / 1_000_000_000.0;

                    long bytesLidos = Math.max(
                            0,
                            leituraAtual - prevRead.get(chave)
                    );

                    long bytesEscritos = Math.max(
                            0,
                            escritaAtual - prevWrite.get(chave)
                    );

                    leituraMBs =
                            bytesLidos / MB / intervaloSegundos;

                    escritaMBs =
                            bytesEscritos / MB / intervaloSegundos;
                }
            }

            // Guarda os contadores para a próxima medição.
            prevRead.put(chave, leituraAtual);
            prevWrite.put(chave, escritaAtual);
            prevTime.put(chave, agora);

            String modelo = disco.getModel();

            if (modelo == null || modelo.isBlank()) {
                modelo = disco.getName();
            }

            if (modelo == null || modelo.isBlank()) {
                modelo = "Disco físico " + (i + 1);
            }

            DiskInfo info = new DiskInfo(
                    modelo,
                    leituraMBs,
                    escritaMBs
            );

            resultado.add(info);
        }

        return resultado;
    }
}
