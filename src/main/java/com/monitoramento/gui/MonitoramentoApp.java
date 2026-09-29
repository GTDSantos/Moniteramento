package com.monitoramento.gui;

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

/**
 * Somente o visual. Todos os números são fixos (mock).
 * Quando ligar a coleta, troque os textos e o vetor DATA pelos dados
 * de CpuInfo, RamInfo, VolumeInfo, DiskInfo, NetInfo e ProcesInfo.
 */
public class MonitoramentoApp extends Application {

    @Override
    public void start(Stage stage) {
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

    // ---------- Área principal ----------
    private VBox conteudo() {
        Label sub = label("A CPU chegou a 71% às 14:32 e voltou a 38%. A memória está em 51%.", "subtitulo");
        sub.setWrapText(true);
        VBox cabecalho = new VBox(6, label("Tudo dentro do normal.", "titulo"), sub);
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

        GraficoCpu grafico = new GraficoCpu();
        grafico.setMinHeight(220);
        grafico.setPrefHeight(240);

        VBox esquerda = new VBox(26, new VBox(12, label("Uso da CPU", "secao"), grafico), medidores());
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
        box.getChildren().addAll(
                linha("Memória RAM", "8,2 de 16 GB", 51, false),
                linha("Disco C:", "415 de 512 GB", 81, true),
                linha("Leitura e escrita", "20 / 4 MB/s", 20, false),
                linha("Rede", "baixa 5, sobe 1 MB/s", 25, false));
        return box;
    }

    private HBox linha(String nome, String valor, int pct, boolean alerta) {
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
            bloco.getStyleClass().add(i < acesos ? (alerta ? "bloco-alerta" : "bloco-on") : "bloco-off");
            medidor.getChildren().add(bloco);
        }
        HBox.setHgrow(medidor, Priority.ALWAYS);

        Label v = label(valor, "linha-valor");
        v.setMinWidth(160);
        v.setAlignment(Pos.CENTER_RIGHT);

        HBox r = new HBox(18, n, medidor, v);
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
        private static final double[] DATA = {22, 25, 24, 30, 28, 35, 33, 29, 41, 38, 44, 52, 47, 43,
                39, 45, 58, 61, 55, 49, 71, 64, 52, 46, 41, 38, 36, 42, 40, 38};
        private static final int PICO = 20;
        private final Canvas cv = new Canvas();

        GraficoCpu() {
            getChildren().add(cv);
        }

        @Override
        protected void layoutChildren() {
            cv.setWidth(getWidth());
            cv.setHeight(getHeight());
            desenhar();
        }

        // curva suave passando por todos os pontos
        private static void curva(GraphicsContext g, double[] x, double[] y) {
            int n = x.length;
            for (int i = 0; i < n - 1; i++) {
                int a = Math.max(i - 1, 0), d = Math.min(i + 2, n - 1);
                g.bezierCurveTo(x[i] + (x[i + 1] - x[a]) / 6, y[i] + (y[i + 1] - y[a]) / 6,
                        x[i + 1] - (x[d] - x[i]) / 6, y[i + 1] - (y[d] - y[i]) / 6,
                        x[i + 1], y[i + 1]);
            }
        }

        private void desenhar() {
            double w = cv.getWidth(), h = cv.getHeight();
            if (w < 60 || h < 60) return;
            GraphicsContext g = cv.getGraphicsContext2D();
            g.clearRect(0, 0, w, h);

            Color cobalto = Color.web("#5B7CFA"), coral = Color.web("#F0654A");
            Color tinta = Color.web("#F2F3EF"), suave = Color.web("#8B948E");
            double esq = 42, largura = w - esq - 8, altura = h - 30, esc = altura - 14;
            int n = DATA.length;
            double[] xs = new double[n], ys = new double[n];
            for (int i = 0; i < n; i++) {
                xs[i] = esq + i * largura / (n - 1);
                ys[i] = altura - esc * DATA[i] / 100;
            }

            // grade pontilhada
            g.setLineWidth(1);
            g.setStroke(Color.web("#262B31"));
            g.setLineDashes(2, 6);
            for (int v = 0; v <= 100; v += 25) {
                double y = altura - esc * v / 100;
                g.strokeLine(esq, y, w - 8, y);
            }
            g.setLineDashes((double[]) null);

            // área com degradê
            g.setFill(new LinearGradient(0, 0, 0, altura, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#5B7CFA", 0.32)), new Stop(1, Color.web("#5B7CFA", 0.0))));
            g.beginPath();
            g.moveTo(xs[0], altura);
            g.lineTo(xs[0], ys[0]);
            curva(g, xs, ys);
            g.lineTo(xs[n - 1], altura);
            g.closePath();
            g.fill();

            // linha
            g.setStroke(cobalto);
            g.setLineWidth(3);
            g.setLineCap(StrokeLineCap.ROUND);
            g.setLineJoin(StrokeLineJoin.ROUND);
            g.beginPath();
            g.moveTo(xs[0], ys[0]);
            curva(g, xs, ys);
            g.stroke();

            // rótulos
            g.setFill(suave);
            g.setFont(Font.font("Bahnschrift", 12));
            for (int v = 0; v <= 100; v += 25) g.fillText(v + "%", 0, altura - esc * v / 100 + 4);
            String[] t = {"14:00", "14:15", "14:30", "14:45", "15:00"};
            for (int k = 0; k < t.length; k++) {
                double x = esq + largura * k / 4.0 - (k == 0 ? 0 : k == 4 ? 34 : 17);
                g.fillText(t[k], x, h - 6);
            }

            // pico: ponto com halo e etiqueta
            double px = xs[PICO], py = ys[PICO];
            g.setFill(Color.web("#F0654A", 0.22));
            g.fillOval(px - 11, py - 11, 22, 22);
            g.setFill(Color.WHITE);
            g.fillOval(px - 6.5, py - 6.5, 13, 13);
            g.setFill(coral);
            g.fillOval(px - 4.5, py - 4.5, 9, 9);

            double pw = 116, ph = 28, bx = px - pw / 2, by = py - ph - 16;
            g.setFill(tinta);
            g.fillRoundRect(bx, by, pw, ph, ph, ph);
            g.setFill(Color.web("#14171B"));
            g.setFont(Font.font("Bahnschrift", FontWeight.BOLD, 12));
            g.fillText("71% às 14:32", bx + 20, by + 18);
        }
    }
}