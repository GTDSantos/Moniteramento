package com.monitoramento.gui;

import com.monitoramento.hardwer.Monitoramento;
import com.monitoramento.model.CpuInfo;
import com.monitoramento.model.NetInfo;
import com.monitoramento.model.RamInfo;
import com.monitoramento.model.VolumeInfo;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;
import com.monitoramento.model.ProcesInfo;

/** Interface do Dashboard e atualização dos indicadores. */
public class DashboardView {
    private final Monitoramento monitoramento;
    private final VBox view;

    private Label cpuLabel;
    private GraficoCpu graficoCpu;
    private Label ramLabel;
    private HBox ramLinha;
    private List<DiscoUI> discosUI = new ArrayList<>();
    private Label redeLabel;
    private HBox redeLinha;
    private ProcessosView processosView;

    private Label periodo30s;
    private Label periodo5min;
    private Label periodo24h;

    public DashboardView(Monitoramento monitoramento) {
        this.monitoramento = monitoramento;
        this.view = conteudo();
        this.view.getStyleClass().add("folha");
    }

    public VBox getView() {
        return view;
    }

    public void fecharHistorico() {
        if (graficoCpu != null) {
            graficoCpu.fecharHistorico();
        }
    }

    private static class DiscoUI {
        Label label;
        HBox linha;

        public DiscoUI(Label label, HBox linha) {
            this.label = label;
            this.linha = linha;
        }
    }

    private Region ponto(String classe, double tam) {
        Region r = new Region();
        r.setPrefSize(tam, tam);
        r.setMinSize(tam, tam);
        r.setMaxSize(tam, tam);
        r.getStyleClass().add(classe);
        return r;
    }

    public void atualizarDados(
            List<ProcesInfo> processosRecebidos,
            List<NetInfo> redes
    ) {
        CpuInfo cpu = monitoramento.getCpu();

        cpuLabel.setText(
                String.format("Uso atual da CPU: %.1f%%", cpu.getCpuLoad())
        );
        if (graficoCpu != null) {
            graficoCpu.adicionarValor(cpu.getCpuLoad());
        }

        RamInfo ram = monitoramento.getRam();

        ramLabel.setText(
                String.format("%.1f de %.1f GB (%.1f%%)",
                        ram.getUsageMemory(),
                        ram.getTotaMemory(),
                        ram.getPorctMemory())
        );
        atualizarBarra(ramLinha, (int) Math.round(ram.getPorctMemory()), false);

        List<VolumeInfo> volumes = monitoramento.getDiskVolume();

        for (int i = 0; i < volumes.size() && i < discosUI.size(); i++) {
            VolumeInfo volume = volumes.get(i);
            DiscoUI disco = discosUI.get(i);

            double usado = volume.getTotal() - volume.getLivre();
            double total = volume.getTotal();
            double porcentagem = volume.getPorctVolume();

            disco.label.setText(
                    String.format("%.1f de %.1f GB (%.1f%%)",
                            usado, total, porcentagem)
            );

            atualizarBarra(
                    disco.linha,
                    (int) Math.round(porcentagem),
                    porcentagem >= 85
            );
        }



        if (redes.isEmpty()) {
            redeLabel.setText("Rede indisponível");
            atualizarBarra(redeLinha, 0, false);
        } else {
            NetInfo rede = redes.get(0);
            redeLabel.setText(
                    String.format("↓ %.2f / ↑ %.2f MB/s",
                            rede.getRecv(),
                            rede.getSent())
            );

            // Referência visual de 10 MB/s para download + upload.
            double trafego = rede.getRecv() + rede.getSent();
            int porcentagemRede = (int) Math.min(100, trafego / 100.0 * 100);
            atualizarBarra(redeLinha, porcentagemRede, porcentagemRede >= 90);
        }

        processosView.atualizarProcessos(processosRecebidos);

    }

    private void atualizarBarra(HBox linha, int pct, boolean alerta) {
        HBox medidor = (HBox) linha.getChildren().get(1);

        int porcentagem = Math.max(0, Math.min(100, pct));
        int acesos = (int) Math.round(porcentagem / 5.0);

        for (int i = 0; i < medidor.getChildren().size(); i++) {
            Region bloco = (Region) medidor.getChildren().get(i);

            bloco.getStyleClass().removeAll(
                    "bloco-alerta", "bloco-on", "bloco-off"
            );

            bloco.getStyleClass().add(
                    i < acesos
                            ? (alerta ? "bloco-alerta" : "bloco-on")
                            : "bloco-off"
            );
        }
    }

    private VBox conteudo() {

        cpuLabel = label("Carregando uso da CPU...", "subtitulo");
        cpuLabel.setWrapText(true);

        VBox cabecalho = new VBox(
                6,
                label("Tudo dentro do normal.", "titulo"),
                cpuLabel
        );
        HBox.setHgrow(cabecalho, Priority.ALWAYS);

        HBox status = new HBox(8, ponto("ponto", 4), label("Saudável", "status-texto"));
        status.setAlignment(Pos.CENTER);
        status.getStyleClass().add("status");

        periodo30s = botaoPeriodo("30 s", false);
        periodo5min = botaoPeriodo("5 min", true);
        periodo24h = botaoPeriodo("24 h", false);

        HBox periodo = new HBox(
                2,
                periodo30s,
                periodo5min,
                periodo24h
        );

        periodo.getStyleClass().add("periodo-grupo");

        HBox direita0 = new HBox(12, status, periodo);
        direita0.setAlignment(Pos.CENTER_RIGHT);
        HBox topo = new HBox(20, cabecalho, direita0);
        topo.setAlignment(Pos.TOP_LEFT);

        graficoCpu = new GraficoCpu();
        graficoCpu.setMinHeight(220);
        graficoCpu.setPrefHeight(240);

        VBox esquerda = new VBox(26,
                new VBox(12,
                        label("Uso da CPU", "secao"),
                        graficoCpu
                ),
                medidores()
        );
        HBox.setHgrow(esquerda, Priority.ALWAYS);

        VBox direita = colunaDireita();
        direita.setPrefWidth(330);
        direita.setMinWidth(330);

        VBox c = new VBox(26, topo, new HBox(42, esquerda, direita));
        c.getStyleClass().add("conteudo");
        return c;
    }

    private Label botaoPeriodo(String texto, boolean ativo) {
        Label l = label(texto, "periodo");

        if (ativo) {
            l.getStyleClass().add("periodo-ativo");
        }

        l.setCursor(Cursor.HAND);

        l.setOnMouseClicked(event -> {
            selecionarPeriodo(texto);
        });

        return l;
    }

    private void selecionarPeriodo(String periodo) {
        periodo30s.getStyleClass().remove("periodo-ativo");
        periodo5min.getStyleClass().remove("periodo-ativo");
        periodo24h.getStyleClass().remove("periodo-ativo");

        switch (periodo) {
            case "30 s" ->
                    periodo30s.getStyleClass().add("periodo-ativo");

            case "5 min" ->
                    periodo5min.getStyleClass().add("periodo-ativo");

            case "24 h" ->
                    periodo24h.getStyleClass().add("periodo-ativo");
        }

        if (graficoCpu != null) {
            graficoCpu.setPeriodo(periodo);
        }
    }

    private VBox medidores() {
        VBox box = new VBox(2, label("Componentes", "secao"));

        ramLabel = label("Carregando...", "linha-valor");
        ramLinha = linha("Memória RAM", ramLabel, 0, false);
        box.getChildren().add(ramLinha);

        List<VolumeInfo> volumes = monitoramento.getDiskVolume();

        discosUI.clear();

        for (VolumeInfo volume : volumes) {
            Label discoLabel = label("Carregando...", "linha-valor");

            HBox discoLinha = linha(
                    volume.getNome(),
                    discoLabel,
                    0,
                    false
            );

            discosUI.add(new DiscoUI(discoLabel, discoLinha));
            box.getChildren().add(discoLinha);
        }

        redeLabel = label("Carregando...", "linha-valor");

        redeLinha = linha("Rede (↓ / ↑)", redeLabel, 0, false);

        box.getChildren().add(redeLinha);

        return box;
    }

    private HBox linha(String nome, Label valorLabel, int pct, boolean alerta) {
        Label n = label(nome, "linha-nome");
        n.setMinWidth(130);

        HBox medidor = new HBox(4);
        medidor.setAlignment(Pos.CENTER_LEFT);

        int acesos = (int) Math.round(pct / 5.0);

        for (int i = 0; i < 20; i++) {
            Region bloco = new Region();
            bloco.setPrefSize(10, 20);
            bloco.setMinWidth(4);
            bloco.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(bloco, Priority.ALWAYS);

            bloco.getStyleClass().add(
                    i < acesos
                            ? (alerta ? "bloco-alerta" : "bloco-on")
                            : "bloco-off"
            );

            medidor.getChildren().add(bloco);
        }

        HBox.setHgrow(medidor, Priority.ALWAYS);

        valorLabel.setMinWidth(160);
        valorLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox r = new HBox(18, n, medidor, valorLabel);
        r.setAlignment(Pos.CENTER_LEFT);
        r.getStyleClass().add("linha");

        return r;
    }

    private VBox colunaDireita() {
        Label aviso = label("O disco C: passou de 80%. Restam 97 GB livres.", "aviso-texto");
        aviso.setWrapText(true);
        HBox avisoBox = new HBox(12, ponto("ponto-alerta", 10), aviso);
        avisoBox.setAlignment(Pos.CENTER_LEFT);
        avisoBox.getStyleClass().add("aviso");

        processosView = new ProcessosView(monitoramento);

        return new VBox(14, label("Atenção", "secao"), avisoBox,
                label("Processos que mais pesam", "secao"), processosView,
                label("Rede estável nas últimas 2 h.", "nota"));
    }

    private static Label label(String texto, String classe) {
        Label l = new Label(texto);
        l.getStyleClass().add(classe);
        return l;
    }

}
