package com.monitoramento.gui;

import com.monitoramento.hardwer.Monitoramento;
import com.monitoramento.model.CpuInfo;
import com.monitoramento.model.NetInfo;
import com.monitoramento.model.RamInfo;
import com.monitoramento.model.VolumeInfo;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import oshi.SystemInfo;
import oshi.spi.SystemInfoProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Somente o visual. Todos os números são fixos (mock).
 * Quando ligar a coleta, troque os textos e o vetor DATA pelos dados
 * de CpuInfo, RamInfo, VolumeInfo, DiskInfo, NetInfo e ProcesInfo.
 */
public class MonitoramentoApp extends Application {

    private Monitoramento monitoramento;

    @Override
    public void start(Stage stage) {
        SystemInfoProvider si = new SystemInfo();
        monitoramento = new Monitoramento(si);

        CpuInfo cpu = monitoramento.getCpu();
        System.out.println(cpu.getNome());
        VBox folha = conteudo();
        folha.getStyleClass().add("folha");

        BorderPane root = new BorderPane();
        root.setLeft(menu());
        root.setCenter(folha);
        BorderPane.setMargin(folha, new Insets(14, 14, 14, 0));

        Scene scene = new Scene(root, 1200, 740);
        scene.getStylesheets().add(
                getClass().getResource("/com/monitoramento/style.css").toExternalForm());

        stage.setTitle("PC Monitor");
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
        CpuInfo cpuInfo = monitoramento.getCpu();
        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), evento -> atualizarDados())
        );

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

    }

    // ---------- Menu lateral ----------
    private VBox menu() {
        VBox itens = new VBox(4);
        String[] nomes = {"Dashboard", "Processos", "Discos", "Rede"};
        for (int i = 0; i < nomes.length; i++) {
            Label l = label(nomes[i], "menu-item");
            l.setMaxWidth(Double.MAX_VALUE);
            if (i == 0) l.getStyleClass().add("ativo");
            itens.getChildren().add(l);
        }
        Region espaco = new Region();
        VBox.setVgrow(espaco, Priority.ALWAYS);

        HBox marca = new HBox(10, ponto("marca", 18), label("PC Monitor", "logo"));
        marca.setAlignment(Pos.CENTER_LEFT);
        marca.setPadding(new Insets(0, 0, 0, 10));

        HBox maquina = new HBox(10, ponto("ponto", 8),
                new VBox(2, label("DESKTOP-01", "maquina-nome"), label("ligado há 3 dias e 4 h", "maquina-info")));
        maquina.setAlignment(Pos.CENTER_LEFT);
        maquina.getStyleClass().add("maquina");

        VBox m = new VBox(30, marca, itens, espaco, maquina);
        m.getStyleClass().add("menu");
        m.setPrefWidth(214);
        m.setMinWidth(214);
        return m;
    }

    private Region ponto(String classe, double tam) {
        Region r = new Region();
        r.setPrefSize(tam, tam);
        r.setMinSize(tam, tam);
        r.setMaxSize(tam, tam);
        r.getStyleClass().add(classe);
        return r;
    }

    private static class DiscoUI {
        Label label;
        HBox linha;

        public DiscoUI(Label label, HBox linha) {
            this.label = label;
            this.linha = linha;
        }
    }


    private Label cpuLabel;
    private GraficoCpu graficoCpu;
    private Timeline timeline;
    private Label ramLabel;
    private HBox ramLinha;
    private List<DiscoUI> discosUI = new ArrayList<>();
    private Label redeLabel;
    private HBox redeLinha;

    private void atualizarDados() {
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

        List<NetInfo> redes = monitoramento.getNet();

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



    // ---------- Área principal ----------
    private VBox conteudo() {

        cpuLabel = label("Carregando uso da CPU...", "subtitulo");
        cpuLabel.setWrapText(true);

        VBox cabecalho = new VBox(
                6,
                label("Tudo dentro do normal.", "titulo"),
                cpuLabel
        );
        HBox.setHgrow(cabecalho, Priority.ALWAYS);

        HBox status = new HBox(8, ponto("ponto", 8), label("Saudável", "status-texto"));
        status.setAlignment(Pos.CENTER);
        status.getStyleClass().add("status");

        HBox periodo = new HBox(2, botaoPeriodo("5 min", false), botaoPeriodo("1 h", true), botaoPeriodo("24 h", false));
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
        if (ativo) l.getStyleClass().add("periodo-ativo");
        return l;
    }

    // ---------- Medidores em blocos ----------

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


    // ---------- Coluna da direita ----------
    private VBox colunaDireita() {
        Label aviso = label("O disco C: passou de 80%. Restam 97 GB livres.", "aviso-texto");
        aviso.setWrapText(true);
        HBox avisoBox = new HBox(12, ponto("ponto-alerta", 10), aviso);
        avisoBox.setAlignment(Pos.CENTER_LEFT);
        avisoBox.getStyleClass().add("aviso");

        VBox lista = new VBox();
        lista.getChildren().add(linhaProcesso("Processo", "Memória", "CPU", -1));
        String[][] p = {
                {"chrome.exe", "1,4 GB", "18%"},
                {"javaw.exe", "612 MB", "9%"},
                {"Code.exe", "540 MB", "6%"},
                {"discord.exe", "310 MB", "3%"},
                {"System", "24 MB", "2%"}};
        for (int i = 0; i < p.length; i++) lista.getChildren().add(linhaProcesso(p[i][0], p[i][1], p[i][2], i));

        return new VBox(14, label("Atenção", "secao"), avisoBox,
                label("Processos que mais pesam", "secao"), lista,
                label("Rede estável nas últimas 2 h.", "nota"));
    }

    private HBox linhaProcesso(String nome, String mem, String cpu, int i) {
        boolean cab = i < 0;
        Label icone = cab ? new Label() : label(nome.substring(0, 1).toUpperCase(), "proc-icone");
        if (!cab) icone.getStyleClass().add("ic-" + (i % 4));
        icone.setMinSize(30, 30);
        icone.setPrefSize(30, 30);
        icone.setAlignment(Pos.CENTER);

        Label n = label(nome, cab ? "proc-cab" : "proc-nome");
        Label m = label(mem, cab ? "proc-cab" : "proc-valor");
        Label c = label(cpu, cab ? "proc-cab" : "proc-valor");
        m.setMinWidth(72);
        c.setMinWidth(44);
        m.setAlignment(Pos.CENTER_RIGHT);
        c.setAlignment(Pos.CENTER_RIGHT);
        Region g = new Region();
        HBox.setHgrow(g, Priority.ALWAYS);
        HBox r = new HBox(12, icone, n, g, m, c);
        r.setAlignment(Pos.CENTER_LEFT);
        r.getStyleClass().add("proc");
        return r;
    }

    private static Label label(String texto, String classe) {
        Label l = new Label(texto);
        l.getStyleClass().add(classe);
        return l;
    }

    // ---------- Gráfico da CPU (Canvas) ----------

    static class GraficoCpu extends Pane {

        private static final int MAX_PONTOS = 30;

        private final List<Double> dados = new ArrayList<>();
        private final Canvas cv = new Canvas();

        GraficoCpu() {
            getChildren().add(cv);
        }

        public void adicionarValor(double valor) {

            // Garante que o valor fique entre 0 e 100.
            double uso = Double.isFinite(valor)
                    ? Math.max(0, Math.min(100, valor))
                    : 0;

            // Inicializa o histórico com o primeiro valor real.
            if (dados.isEmpty()) {
                for (int i = 0; i < MAX_PONTOS; i++) {
                    dados.add(uso);
                }
            } else {
                dados.add(uso);
            }

            // Mantém somente as últimas 30 leituras.
            while (dados.size() > MAX_PONTOS) {
                dados.remove(0);
            }

            desenhar();
        }

        @Override
        protected void layoutChildren() {
            cv.setWidth(getWidth());
            cv.setHeight(getHeight());
            desenhar();
        }

        private void desenhar() {

            double larguraTela = cv.getWidth();
            double alturaTela = cv.getHeight();

            if (larguraTela < 60 || alturaTela < 60) {
                return;
            }

            GraphicsContext g = cv.getGraphicsContext2D();

            g.clearRect(0, 0, larguraTela, alturaTela);

            double margemEsquerda = 42;
            double margemDireita = 8;
            double margemInferior = 30;

            double largura = larguraTela
                    - margemEsquerda - margemDireita;

            double altura = alturaTela - margemInferior;
            double escala = altura - 14;

            // Desenha a grade de 0% até 100%.
            g.setStroke(Color.web("#262B31"));
            g.setLineWidth(1);
            g.setLineDashes(2, 6);

            for (int v = 0; v <= 100; v += 25) {
                double y = altura - escala * v / 100.0;

                g.strokeLine(
                        margemEsquerda, y,
                        larguraTela - margemDireita, y
                );
            }

            g.setLineDashes((double[]) null);

            // Exibe os valores do eixo vertical.
            g.setFill(Color.web("#8B948E"));
            g.setFont(Font.font("Bahnschrift", 12));

            for (int v = 0; v <= 100; v += 25) {
                double y = altura - escala * v / 100.0;

                g.fillText(v + "%", 0, y + 4);
            }

            if (dados.size() < 2) {
                return;
            }

            int n = dados.size();

            double[] x = new double[n];
            double[] y = new double[n];

            // Converte cada percentual em uma posição no gráfico.
            for (int i = 0; i < n; i++) {
                x[i] = margemEsquerda
                        + i * largura / (MAX_PONTOS - 1);

                y[i] = altura - escala * dados.get(i) / 100.0;
            }

            // Preenche a área abaixo da linha.
            g.setFill(Color.web("#5B7CFA", 0.20));
            g.beginPath();
            g.moveTo(x[0], altura);
            g.lineTo(x[0], y[0]);

            for (int i = 1; i < n; i++) {
                g.lineTo(x[i], y[i]);
            }

            g.lineTo(x[n - 1], altura);
            g.closePath();
            g.fill();

            // Desenha a linha com o uso real da CPU.
            g.setStroke(Color.web("#5B7CFA"));
            g.setLineWidth(3);
            g.setLineCap(StrokeLineCap.ROUND);
            g.setLineJoin(StrokeLineJoin.ROUND);

            g.beginPath();
            g.moveTo(x[0], y[0]);

            for (int i = 1; i < n; i++) {
                g.lineTo(x[i], y[i]);
            }

            g.stroke();

            // Marca o ponto da leitura mais recente.
            double atualX = x[n - 1];
            double atualY = y[n - 1];

            g.setFill(Color.web("#F0654A", 0.25));
            g.fillOval(atualX - 10, atualY - 10, 20, 20);

            g.setFill(Color.WHITE);
            g.fillOval(atualX - 5, atualY - 5, 10, 10);

            // Mostra o percentual atual.
            g.setFill(Color.web("#F2F3EF"));
            g.fillRoundRect(
                    Math.max(margemEsquerda, atualX - 100),
                    Math.max(2, atualY - 38),
                    100, 28, 12, 12
            );

            g.setFill(Color.web("#14171B"));
            g.setFont(Font.font(
                    "Bahnschrift", FontWeight.BOLD, 12
            ));

            String texto = String.format(
                    "%.1f%% agora", dados.get(n - 1)
            );

            g.fillText(
                    texto,
                    Math.max(margemEsquerda, atualX - 90),
                    Math.max(20, atualY - 20)
            );

            // Indica aproximadamente o tempo do histórico.
            g.setFill(Color.web("#8B948E"));
            g.setFont(Font.font("Bahnschrift", 12));

            g.fillText("-30 s", margemEsquerda, alturaTela - 6);
            g.fillText("-20 s", margemEsquerda + largura / 3 - 17,
                    alturaTela - 6);
            g.fillText("-10 s", margemEsquerda + 2 * largura / 3 - 17,
                    alturaTela - 6);
            g.fillText("Agora", larguraTela - 43, alturaTela - 6);
        }
    }

}