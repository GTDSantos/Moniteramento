package com.monitoramento.gui;

import com.monitoramento.hardwer.Monitoramento;
import com.monitoramento.historico.HistoricoMonitoramento;
import com.monitoramento.model.CpuInfo;
import com.monitoramento.model.NetInfo;
import com.monitoramento.model.RamInfo;
import com.monitoramento.model.VolumeInfo;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;
import com.monitoramento.model.ProcesInfo;

/** Interface do Dashboard e atualização dos indicadores. */
public class DashboardView {
    private final Monitoramento monitoramento;
    private final HistoricoMonitoramento historicoMonitoramento;
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

    // Elementos que serão atualizados pelos alertas
    private Label tituloEstado;
    private Label statusTexto;
    private Region statusPonto;
    private HBox statusBox;

    private Label avisoTexto;
    private Region avisoPonto;
    private HBox avisoBox;

    private Label notaRede;

    private CheckBox historicoCheckBox;
    private Label historicoStatus;

    public DashboardView(
            Monitoramento monitoramento,
            HistoricoMonitoramento historicoMonitoramento
    ) {
        this.monitoramento = monitoramento;
        this.historicoMonitoramento = historicoMonitoramento;
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



        if (redes == null
                || redes.isEmpty()
                || redes.get(0) == null) {

            redeLabel.setText("Rede indisponível");
            atualizarBarra(redeLinha, 0, false);

        } else {
            NetInfo rede = redes.get(0);

            double recebido = valorValido(rede.getRecv());
            double enviado = valorValido(rede.getSent());

            redeLabel.setText(String.format(
                    "↓ %.2f / ↑ %.2f MB/s",
                    recebido,
                    enviado
            ));

            double trafego = recebido + enviado;

            int porcentagemRede = (int) Math.min(
                    100,
                    trafego / 10.0 * 100
            );

            atualizarBarra(
                    redeLinha,
                    porcentagemRede,
                    porcentagemRede >= 90
            );
        }
        atualizarAlertas(
                cpu.getCpuLoad(),
                ram.getPorctMemory(),
                volumes,
                redes
        );

        atualizarNotaRede(redes);

        // Salva uma amostra abrangente somente se o usuário ativou o histórico.
        historicoMonitoramento.registrarSeNecessario(
                cpu,
                ram,
                volumes,
                redes,
                processosRecebidos
        );

        // Se houve erro ao salvar, sincroniza o controle visual com o estado real.
        if (historicoCheckBox != null
                && historicoCheckBox.isSelected()
                && !historicoMonitoramento.isHabilitado()) {
            historicoCheckBox.setSelected(false);
            atualizarTextoHistorico();
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

        tituloEstado = label(
                "Analisando o computador...",
                "titulo"
        );

        VBox cabecalho = new VBox(
                6,
                tituloEstado,
                cpuLabel
        );

        HBox.setHgrow(cabecalho, Priority.ALWAYS);

// Indicador de saúde dinâmico
        statusPonto = ponto("ponto", 4);
        statusTexto = label("Analisando...", "status-texto");

        statusBox = new HBox(
                8,
                statusPonto,
                statusTexto
        );

        statusBox.setAlignment(Pos.CENTER);
        statusBox.getStyleClass().add("status");

        periodo30s = botaoPeriodo("30 s", true);
        periodo5min = botaoPeriodo("5 min", false);
        periodo24h = botaoPeriodo("24 h", false);

        HBox periodo = new HBox(
                2,
                periodo30s,
                periodo5min,
                periodo24h
        );

        periodo.getStyleClass().add("periodo-grupo");

        HBox direita0 = new HBox(12, statusBox, periodo);
        direita0.setAlignment(Pos.CENTER_RIGHT);
        HBox topo = new HBox(20, cabecalho, direita0);
        topo.setAlignment(Pos.TOP_LEFT);

        graficoCpu = new GraficoCpu(historicoMonitoramento.isHabilitado());
        graficoCpu.setMinHeight(220);
        graficoCpu.setPrefHeight(240);

        historicoCheckBox = new CheckBox("Salvar histórico");
        historicoCheckBox.getStyleClass().add("historico-checkbox");
        historicoCheckBox.setSelected(historicoMonitoramento.isHabilitado());
        historicoCheckBox.setTooltip(new Tooltip(
                "Grava CPU, RAM, discos, rede e os 10 processos com maior uso "
                        + "a cada 5 segundos. Mantém até 7 dias."
        ));

        historicoStatus = label("", "historico-status");
        historicoStatus.setWrapText(true);
        atualizarTextoHistorico();

        historicoCheckBox.setOnAction(event -> {
            boolean solicitado = historicoCheckBox.isSelected();
            boolean alterado = historicoMonitoramento.setHabilitado(solicitado);

            if (!alterado) {
                historicoCheckBox.setSelected(false);
            }

            boolean habilitado = historicoMonitoramento.isHabilitado();
            graficoCpu.setHistoricoHabilitado(habilitado);
            atualizarTextoHistorico();
        });

        Region espacoTituloGrafico = new Region();
        HBox.setHgrow(espacoTituloGrafico, Priority.ALWAYS);

        HBox tituloGrafico = new HBox(
                12,
                label("Uso da CPU", "secao"),
                espacoTituloGrafico,
                historicoCheckBox
        );
        tituloGrafico.setAlignment(Pos.CENTER_LEFT);

        VBox blocoGrafico = new VBox(
                10,
                tituloGrafico,
                graficoCpu,
                historicoStatus
        );

        VBox esquerda = new VBox(26,
                blocoGrafico,
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

        avisoTexto = label(
                "Coletando informações para verificar alertas...",
                "aviso-texto"
        );
        avisoTexto.setWrapText(true);

        avisoPonto = ponto("ponto-alerta", 10);

        avisoBox = new HBox(
                12,
                avisoPonto,
                avisoTexto
        );

        avisoBox.setAlignment(Pos.CENTER_LEFT);

        avisoBox.getStyleClass().addAll(
                "aviso",
                "aviso-normal"
        );

        processosView = new ProcessosView(monitoramento);

        notaRede = label(
                "Aguardando a primeira medição de rede...",
                "nota"
        );
        notaRede.setWrapText(true);

        return new VBox(
                14,
                label("Atenção", "secao"),
                avisoBox,
                label("Processos que mais pesam", "secao"),
                processosView,
                notaRede
        );
    }
    private void atualizarAlertas(
            double usoCpu,
            double usoRam,
            List<VolumeInfo> volumes,
            List<NetInfo> redes
    ) {
        List<String> criticos = new ArrayList<>();
        List<String> atencao = new ArrayList<>();

        usoCpu = valorValido(usoCpu);
        usoRam = valorValido(usoRam);

        // CPU
        if (usoCpu >= 90) {
            criticos.add(String.format(
                    "Uso da CPU muito alto: %.1f%%.",
                    usoCpu
            ));
        } else if (usoCpu >= 80) {
            atencao.add(String.format(
                    "Uso da CPU elevado: %.1f%%.",
                    usoCpu
            ));
        }

        // RAM
        if (usoRam >= 95) {
            criticos.add(String.format(
                    "Memória RAM quase cheia: %.1f%% usada.",
                    usoRam
            ));
        } else if (usoRam >= 85) {
            atencao.add(String.format(
                    "Uso elevado da memória RAM: %.1f%%.",
                    usoRam
            ));
        }

        // Volumes de armazenamento
        if (volumes != null) {
            for (VolumeInfo volume : volumes) {

                if (volume == null) {
                    continue;
                }

                double porcentagem = valorValido(
                        volume.getPorctVolume()
                );

                String nome = volume.getNome();

                if (nome == null || nome.isBlank()) {
                    nome = "Volume de armazenamento";
                }

                if (porcentagem >= 95) {

                    criticos.add(String.format(
                            "%s está quase cheio: %.0f%% usado, restam %d GB.",
                            nome,
                            porcentagem,
                            Math.max(0, volume.getLivre())
                    ));

                } else if (porcentagem >= 85) {

                    atencao.add(String.format(
                            "%s está com pouco espaço: %.0f%% usado, restam %d GB.",
                            nome,
                            porcentagem,
                            Math.max(0, volume.getLivre())
                    ));
                }
            }
        }

        // Disponibilidade do monitor de rede
        if (redes == null
                || redes.isEmpty()
                || redes.get(0) == null) {

            atencao.add(
                    "Nenhuma interface de rede válida foi encontrada."
            );
        }

        String nivel;
        List<String> mensagens = new ArrayList<>();

        if (!criticos.isEmpty()) {
            nivel = "critico";
            mensagens.addAll(criticos);
            mensagens.addAll(atencao);

        } else if (!atencao.isEmpty()) {
            nivel = "atencao";
            mensagens.addAll(atencao);

        } else {
            nivel = "normal";
        }

        // Atualiza o cabeçalho e a mensagem do alerta
        if (mensagens.isEmpty()) {

            tituloEstado.setText("Tudo dentro do normal.");
            statusTexto.setText("Saudável");

            avisoTexto.setText(
                    "Nenhum alerta no momento. CPU, RAM e armazenamento "
                            + "estão dentro dos limites configurados."
            );

        } else {

            if (nivel.equals("critico")) {
                tituloEstado.setText("Ação necessária.");
                statusTexto.setText("Crítico");
            } else {
                tituloEstado.setText("Atenção a alguns componentes.");
                statusTexto.setText("Atenção");
            }

            // Exibe no máximo dois alertas completos
            StringBuilder texto = new StringBuilder();

            int mostradas = Math.min(2, mensagens.size());

            for (int i = 0; i < mostradas; i++) {

                if (texto.length() > 0) {
                    texto.append("\n");
                }

                texto.append("• ").append(mensagens.get(i));
            }

            int restantes = mensagens.size() - mostradas;

            if (restantes > 0) {
                texto.append("\n+ ")
                        .append(restantes)
                        .append(restantes == 1
                                ? " outro alerta."
                                : " outros alertas.");
            }

            avisoTexto.setText(texto.toString());
        }

        // Atualiza a cor do título
        tituloEstado.getStyleClass().remove("titulo-alerta");

        if (!nivel.equals("normal")) {
            tituloEstado.getStyleClass().add("titulo-alerta");
        }

        // Atualiza as cores do cartão de alerta
        avisoBox.getStyleClass().removeAll(
                "aviso-normal",
                "aviso-atencao",
                "aviso-critico"
        );

        avisoBox.getStyleClass().add("aviso-" + nivel);

        // Atualiza o indicador de saúde
        statusBox.getStyleClass().removeAll(
                "status-normal",
                "status-atencao",
                "status-critico"
        );

        statusBox.getStyleClass().add("status-" + nivel);

        // Atualiza as cores dos pontos indicadores
        String classePonto = nivel.equals("normal")
                ? "ponto"
                : nivel.equals("critico")
                  ? "ponto-alerta"
                  : "ponto-atencao";

        avisoPonto.getStyleClass().removeAll(
                "ponto",
                "ponto-alerta",
                "ponto-atencao"
        );

        avisoPonto.getStyleClass().add(classePonto);

        statusPonto.getStyleClass().removeAll(
                "ponto",
                "ponto-alerta",
                "ponto-atencao"
        );

        statusPonto.getStyleClass().add(classePonto);
    }

    private double valorValido(double valor) {

        if (!Double.isFinite(valor) || valor < 0) {
            return 0;
        }

        return valor;
    }
    private void atualizarNotaRede(List<NetInfo> redes) {

        if (redes == null
                || redes.isEmpty()
                || redes.get(0) == null) {

            notaRede.setText(
                    "Rede indisponível: não há uma interface válida "
                            + "para monitorar neste momento."
            );

            return;
        }

        NetInfo rede = redes.get(0);

        String statusRede = rede.getStatus() == null
                ? "desconhecido"
                : rede.getStatus();

        double recebido = valorValido(rede.getRecv());
        double enviado = valorValido(rede.getSent());

        boolean ativa =
                statusRede.equalsIgnoreCase("ativo")
                        || statusRede.equalsIgnoreCase("ativa")
                        || statusRede.equalsIgnoreCase("up");

        if (!ativa) {

            notaRede.setText(
                    "A interface selecionada informa que a conexão "
                            + "não está ativa."
            );

        } else if (recebido + enviado < 0.001) {

            notaRede.setText(
                    "Rede conectada; sem tráfego significativo "
                            + "na última medição."
            );

        } else {

            notaRede.setText(String.format(
                    "Atividade de rede agora: ↓ %.2f MB/s recebidos "
                            + "e ↑ %.2f MB/s enviados.",
                    recebido,
                    enviado
            ));
        }
    }


    private void atualizarTextoHistorico() {
        if (historicoStatus == null) {
            return;
        }

        if (historicoMonitoramento.isHabilitado()) {
            historicoStatus.setText(
                    "Histórico ativo: amostras a cada 5 s, retenção de 7 dias. "
                            + "Pasta dos CSVs diários: "
                            + historicoMonitoramento.getDiretorioHistorico()
            );
            historicoStatus.getStyleClass().remove("historico-status-desativado");
            if (!historicoStatus.getStyleClass().contains("historico-status-ativo")) {
                historicoStatus.getStyleClass().add("historico-status-ativo");
            }
        } else {
            historicoStatus.setText(
                    "Histórico desativado. O gráfico continua funcionando ao vivo, "
                            + "mas novas amostras não serão salvas em disco."
            );
            historicoStatus.getStyleClass().remove("historico-status-ativo");
            if (!historicoStatus.getStyleClass().contains("historico-status-desativado")) {
                historicoStatus.getStyleClass().add("historico-status-desativado");
            }
        }
    }

    private static Label label(String texto, String classe) {
        Label l = new Label(texto);
        l.getStyleClass().add(classe);
        return l;
    }

}
