package com.monitoramento.historico;

import java.util.ArrayList;
import java.util.List;

/** Uma leitura persistida do computador, usada pelas telas de análise. */
public final class HistoricoAmostra {
    private final long instanteMs;
    private final double cpuPercentual;
    private final double ramUsadaGb;
    private final double ramTotalGb;
    private final double ramPercentual;
    private final double downloadMbS;
    private final double uploadMbS;
    private final List<VolumeHistorico> volumes = new ArrayList<>();

    public HistoricoAmostra(long instanteMs, double cpuPercentual,
                            double ramUsadaGb, double ramTotalGb,
                            double ramPercentual, double downloadMbS,
                            double uploadMbS) {
        this.instanteMs = instanteMs;
        this.cpuPercentual = limitarPercentual(cpuPercentual);
        this.ramUsadaGb = Math.max(0, ramUsadaGb);
        this.ramTotalGb = Math.max(0, ramTotalGb);
        this.ramPercentual = limitarPercentual(ramPercentual);
        this.downloadMbS = Math.max(0, downloadMbS);
        this.uploadMbS = Math.max(0, uploadMbS);
    }

    public long getInstanteMs() { return instanteMs; }
    public double getCpuPercentual() { return cpuPercentual; }
    public double getRamUsadaGb() { return ramUsadaGb; }
    public double getRamTotalGb() { return ramTotalGb; }
    public double getRamPercentual() { return ramPercentual; }
    public double getDownloadMbS() { return downloadMbS; }
    public double getUploadMbS() { return uploadMbS; }
    public List<VolumeHistorico> getVolumes() { return volumes; }

    public double getMaiorUsoVolume() {
        return volumes.stream().mapToDouble(VolumeHistorico::getUsoPercentual)
                .max().orElse(0);
    }

    public String getNomeVolumeMaisUsado() {
        return volumes.stream().max((a, b) -> Double.compare(
                        a.getUsoPercentual(), b.getUsoPercentual()))
                .map(VolumeHistorico::getNome).orElse("Volume");
    }

    void adicionarVolume(VolumeHistorico volume) { volumes.add(volume); }

    private static double limitarPercentual(double valor) {
        if (!Double.isFinite(valor)) return 0;
        return Math.max(0, Math.min(100, valor));
    }

    public static final class VolumeHistorico {
        private final String nome;
        private final double usadoGb;
        private final double totalGb;
        private final double usoPercentual;

        public VolumeHistorico(String nome, double usadoGb, double totalGb,
                               double usoPercentual) {
            this.nome = nome == null || nome.isBlank() ? "Volume" : nome;
            this.usadoGb = Math.max(0, usadoGb);
            this.totalGb = Math.max(0, totalGb);
            this.usoPercentual = limitarPercentual(usoPercentual);
        }
        public String getNome() { return nome; }
        public double getUsadoGb() { return usadoGb; }
        public double getTotalGb() { return totalGb; }
        public double getUsoPercentual() { return usoPercentual; }
    }

}
