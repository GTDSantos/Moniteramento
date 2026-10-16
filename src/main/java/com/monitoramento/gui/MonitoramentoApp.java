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

import javafx.scene.Cursor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import com.monitoramento.model.ProcesInfo;
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
    @Override
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }

        if (graficoCpu != null) {
            graficoCpu.fecharHistorico();
        }
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
    private VBox processosLista;

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

        atualizarProcessos();

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


    private Label periodo30s;
    private Label periodo5min;
    private Label periodo24h;

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

        processosLista = new VBox();
        atualizarProcessos();

        return new VBox(14, label("Atenção", "secao"), avisoBox,
                label("Processos que mais pesam", "secao"), processosLista,
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
        n.setMinWidth(0);
        n.setMaxWidth(130);
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

    private void atualizarProcessos() {
        // Evita tentar atualizar a lista antes de ela existir.
        if (processosLista == null || monitoramento == null) {
            return;
        }

        // Busca os processos que estão rodando no computador.
        List<ProcesInfo> processos =
                new ArrayList<>(monitoramento.getProces());

        // O Idle representa tempo ocioso, não um programa consumindo CPU.
        processos.removeIf(processo -> {
            String nome = processo.getNome();

            return nome != null &&
                    (nome.equalsIgnoreCase("Idle")
                            || nome.equalsIgnoreCase("System Idle Process"));
        });

        // Ordena do maior uso de CPU para o menor.
        // Se houver empate, mostra primeiro quem utiliza mais memória.
        processos.sort((p1, p2) -> {
            int comparacaoCpu = Double.compare(
                    p2.getCpu(),
                    p1.getCpu()
            );

            if (comparacaoCpu != 0) {
                return comparacaoCpu;
            }

            return Long.compare(
                    p2.getMemory(),
                    p1.getMemory()
            );
        });

        // Limpa as linhas antigas antes de mostrar os dados atualizados.
        processosLista.getChildren().clear();

        // Cabeçalho das colunas.
        processosLista.getChildren().add(
                linhaProcesso("Processo", "Memória", "CPU", -1)
        );

        // Mostra no máximo cinco processos.
        int limite = Math.min(5, processos.size());

        for (int i = 0; i < limite; i++) {
            ProcesInfo processo = processos.get(i);

            String nome = processo.getNome();
            String memoria = formatarMemoria(processo.getMemory());
            String cpu = String.format(
                    "%.1f%%",
                    Math.max(0, processo.getCpu())
            );

            processosLista.getChildren().add(
                    linhaProcesso(nome, memoria, cpu, i)
            );
        }

        if (processos.isEmpty()) {
            processosLista.getChildren().add(
                    label("Nenhum processo encontrado", "nota")
            );
        }
    }

    private String formatarMemoria(long memoriaMb) {
        if (memoriaMb >= 1024) {
            return String.format(
                    "%.1f GB",
                    memoriaMb / 1024.0
            );
        }

        return memoriaMb + " MB";
    }

    // ---------- Gráfico da CPU (Canvas) ----------

    static class GraficoCpu extends Pane {

        // Um dia inteiro, em milissegundos.
        private static final long TEMPO_MAXIMO =
                24 * 60 * 60 * 1_000L;

        private static final long UM_SEGUNDO = 1_000L;

        // Limite de pontos desenhados para o gráfico continuar leve.
        private static final int MAX_PONTOS_DESENHO = 500;

        // Cada ponto tem o horário da coleta e o percentual da CPU.
        private static class PontoCpu {
            long instante;
            double uso;

            PontoCpu(long instante, double uso) {
                this.instante = instante;
                this.uso = uso;
            }
        }

        private final List<PontoCpu> dados = new ArrayList<>();
        private final Canvas cv = new Canvas();

        // O gráfico começa mostrando os últimos 5 minutos.
        private long periodoMs = 5 * 60 * UM_SEGUNDO;
        // Dados usados para animar a passagem entre duas leituras.
        private static final long DURACAO_ANIMACAO = 900;

        private double valorAnimacaoInicio;
        private double valorAnimacaoFim;
        private long inicioAnimacao;

        // Redesenha o gráfico durante a animação, sem coletar novos dados.
        private final Timeline animacaoGrafico;

        // Arquivo que preserva o histórico entre execuções.
        private final Path arquivoHistorico = Path.of(
                System.getProperty("user.home"),
                ".pcmonitor",
                "historico_cpu.csv"
        );

        private BufferedWriter escritorHistorico;

        GraficoCpu() {
            getChildren().add(cv);

            carregarHistorico();
            abrirArquivoHistorico();

            // Atualiza apenas o desenho, aproximadamente 30 vezes por segundo.
            animacaoGrafico = new Timeline(
                    new KeyFrame(
                            Duration.millis(33),
                            evento -> desenhar()
                    )
            );

            animacaoGrafico.setCycleCount(Animation.INDEFINITE);
        }

        // Recebe o período escolhido nos botões.
        void setPeriodo(String periodo) {
            periodoMs = switch (periodo) {
                case "30 s" -> 30 * UM_SEGUNDO;
                case "24 h" -> TEMPO_MAXIMO;
                default -> 5 * 60 * UM_SEGUNDO;
            };

            desenhar();
        }

        // Carrega do CSV as leituras das últimas 24 horas.
        private void carregarHistorico() {
            if (!Files.exists(arquivoHistorico)) {
                return;
            }

            long agora = System.currentTimeMillis();
            long limite = agora - TEMPO_MAXIMO;

            try (BufferedReader leitor = Files.newBufferedReader(
                    arquivoHistorico,
                    StandardCharsets.UTF_8
            )) {
                // Ignora a primeira linha, que contém os nomes das colunas.
                leitor.readLine();

                String linha;

                while ((linha = leitor.readLine()) != null) {
                    try {
                        String[] campos = linha.split(",");

                        long instante = Long.parseLong(campos[0]);
                        double uso = Double.parseDouble(campos[1]);

                        if (instante >= limite && instante <= agora) {
                            dados.add(new PontoCpu(instante, uso));
                        }

                    } catch (NumberFormatException
                             | ArrayIndexOutOfBoundsException e) {
                        // Ignora linhas inválidas do arquivo.
                    }
                }

            } catch (IOException e) {
                System.err.println(
                        "Erro ao carregar o histórico da CPU: "
                                + e.getMessage()
                );
            }
        }

        // Prepara o arquivo para receber novas leituras.
        private void abrirArquivoHistorico() {
            try {
                Files.createDirectories(arquivoHistorico.getParent());

                boolean arquivoNovo =
                        !Files.exists(arquivoHistorico)
                                || Files.size(arquivoHistorico) == 0;

                escritorHistorico = Files.newBufferedWriter(
                        arquivoHistorico,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );

                if (arquivoNovo) {
                    escritorHistorico.write("timestamp_ms,uso_cpu");
                    escritorHistorico.newLine();
                    escritorHistorico.flush();
                }

            } catch (IOException e) {
                System.err.println(
                        "Erro ao abrir o histórico da CPU: "
                                + e.getMessage()
                );
            }
        }

        // Recebe uma leitura da CPU a cada atualização do monitor.
        public void adicionarValor(double valor) {
            double uso = Double.isFinite(valor)
                    ? Math.max(0, Math.min(100, valor))
                    : 0;

            long agora = System.currentTimeMillis();
            // Descobre de qual valor a animação deve partir.
            double valorAnterior = uso;

            if (!dados.isEmpty()) {
                PontoCpu ultimo = dados.get(dados.size() - 1);

                // Evita animar a partir de uma leitura antiga após reabrir o programa.
                if (agora - ultimo.instante <= 2_000) {
                    valorAnterior = ultimo.uso;
                }
            }

            // Define o início e o destino da animação.
            valorAnimacaoInicio = valorAnterior;
            valorAnimacaoFim = uso;
            inicioAnimacao = agora;


            if (!dados.isEmpty()) {
                PontoCpu ultimo = dados.get(dados.size() - 1);

                // Evita animar a partir de uma leitura antiga após reabrir o programa.
                if (agora - ultimo.instante <= 2_000) {
                    valorAnterior = ultimo.uso;
                }
            }

            // Define o início e o destino da animação.


            // 1. Guarda a leitura na memória.
            dados.add(new PontoCpu(agora, uso));
            // Reinicia a transição com a nova leitura.
            animacaoGrafico.stop();
            animacaoGrafico.playFromStart();

            // 2. Grava a leitura no arquivo.
            if (escritorHistorico != null) {
                try {
                    escritorHistorico.write(agora + "," + uso);
                    escritorHistorico.newLine();
                    escritorHistorico.flush();

                } catch (IOException e) {
                    System.err.println(
                            "Erro ao salvar o histórico da CPU: "
                                    + e.getMessage()
                    );
                }
            }

            // 3. Mantém na memória somente as últimas 24 horas.
            long limite = agora - TEMPO_MAXIMO;

            while (!dados.isEmpty()
                    && dados.get(0).instante < limite) {
                dados.remove(0);
            }

            // 4. Atualiza o desenho.
            desenhar();
        }

        // Redimensiona o Canvas junto com o painel.
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

            // Desenha as linhas horizontais de 0% a 100%.
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

            // Valores do eixo vertical.
            g.setFill(Color.web("#8B948E"));
            g.setFont(Font.font("Bahnschrift", 12));

            for (int v = 0; v <= 100; v += 25) {
                double y = altura - escala * v / 100.0;
                g.fillText(v + "%", 0, y + 4);
            }

            // Define os horários que aparecerão no eixo horizontal.
            String[] tempos;

            if (periodoMs <= 30 * UM_SEGUNDO) {
                tempos = new String[]{
                        "-30 s", "-20 s", "-10 s", "Agora"
                };
            } else if (periodoMs <= 5 * 60 * UM_SEGUNDO) {
                tempos = new String[]{
                        "-5 min", "-3:20", "-1:40", "Agora"
                };
            } else {
                tempos = new String[]{
                        "-24 h", "-16 h", "-8 h", "Agora"
                };
            }

            g.setFill(Color.web("#8B948E"));
            g.setFont(Font.font("Bahnschrift", 12));

            g.fillText(tempos[0], margemEsquerda, alturaTela - 6);
            g.fillText(
                    tempos[1],
                    margemEsquerda + largura / 3 - 17,
                    alturaTela - 6
            );
            g.fillText(
                    tempos[2],
                    margemEsquerda + 2 * largura / 3 - 17,
                    alturaTela - 6
            );
            g.fillText(
                    tempos[3],
                    larguraTela - 43,
                    alturaTela - 6
            );

            // Seleciona os pontos que pertencem ao período escolhido.
            long agora = System.currentTimeMillis();
            long inicio = agora - periodoMs;

            List<PontoCpu> visiveis = new ArrayList<>();

            for (PontoCpu ponto : dados) {
                if (ponto.instante >= inicio
                        && ponto.instante <= agora) {
                    visiveis.add(ponto);
                }
            }

            if (visiveis.isEmpty()) {
                g.setFill(Color.web("#8B948E"));
                g.fillText(
                        "Aguardando leituras deste período...",
                        margemEsquerda + 10,
                        altura / 2
                );
                return;
            }

            // Reduz os pontos desenhados para melhorar o desempenho.
            // O CSV continua guardando as leituras originais.
            List<PontoCpu> pontosDesenho = new ArrayList<>();

            int tamanhoBloco = Math.max(
                    1,
                    (int) Math.ceil(
                            visiveis.size() / (double) MAX_PONTOS_DESENHO
                    )
            );

            for (int i = 0; i < visiveis.size(); i += tamanhoBloco) {
                int fim = Math.min(
                        i + tamanhoBloco,
                        visiveis.size()
                );

                double soma = 0;

                for (int j = i; j < fim; j++) {
                    soma += visiveis.get(j).uso;
                }

                double media = soma / (fim - i);

                // Usa o horário central do bloco.
                long instante = visiveis.get(
                        i + (fim - i) / 2
                ).instante;

                pontosDesenho.add(
                        new PontoCpu(instante, media)
                );
            }
            // Horário atual usado somente para desenhar a ponta do gráfico.
            long instanteVisual = System.currentTimeMillis();

// Calcula o avanço da animação entre 0 e 1.
            double progresso;

            if (inicioAnimacao == 0) {
                progresso = 1;
            } else {
                progresso = (instanteVisual - inicioAnimacao)
                        / (double) DURACAO_ANIMACAO;

                progresso = Math.max(0, Math.min(1, progresso));
            }

// Suaviza o início e o fim da transição.
            double progressoSuave =
                    progresso * progresso * (3 - 2 * progresso);

// Interpola o percentual entre a leitura anterior e a nova.
            double valorVisual = valorAnimacaoInicio
                    + (valorAnimacaoFim - valorAnimacaoInicio)
                    * progressoSuave;

            // Substitui apenas o último ponto usado no desenho.
            // O histórico real continua intacto.
            if (!pontosDesenho.isEmpty()) {
                int ultimoIndice = pontosDesenho.size() - 1;

                pontosDesenho.set(
                        ultimoIndice,
                        new PontoCpu(instanteVisual, valorVisual)
                );
            }
            int n = pontosDesenho.size();

            double[] x = new double[n];
            double[] y = new double[n];

            // Converte os horários e percentuais para posições no gráfico.
            for (int i = 0; i < n; i++) {
                PontoCpu ponto = pontosDesenho.get(i);

                double posicaoTempo =
                        (ponto.instante - inicio) / (double) periodoMs;

                posicaoTempo = Math.max(
                        0,
                        Math.min(1, posicaoTempo)
                );

                x[i] = margemEsquerda
                        + posicaoTempo * largura;

                y[i] = altura - escala * ponto.uso / 100.0;
            }

            // Preenche a área abaixo da linha.
            if (n >= 2) {
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

                // Desenha a linha da CPU.
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
            }

            // Destaca a leitura mais recente que está visível.
            double posicaoAtual = Math.max(
                    0,
                    Math.min(
                            1,
                            (instanteVisual - inicio) / (double) periodoMs
                    )
            );

            double atualX = margemEsquerda
                    + posicaoAtual * largura;

            double atualY = altura - escala * valorVisual / 100.0;

            g.setFill(Color.web("#F0654A", 0.25));
            g.fillOval(atualX - 10, atualY - 10, 20, 20);

            g.setFill(Color.WHITE);
            g.fillOval(atualX - 5, atualY - 5, 10, 10);

            // Exibe o percentual da leitura mais recente.
            double caixaX = Math.max(
                    margemEsquerda,
                    Math.min(larguraTela - 108, atualX - 100)
            );

            g.setFill(Color.web("#F2F3EF"));
            g.fillRoundRect(
                    caixaX,
                    Math.max(2, atualY - 38),
                    100,
                    28,
                    12,
                    12
            );

            g.setFill(Color.web("#14171B"));
            g.setFont(Font.font(
                    "Bahnschrift",
                    FontWeight.BOLD,
                    12
            ));

            String texto = String.format(
                    "%.1f%% CPU",
                    valorVisual
            );

            g.fillText(
                    texto,
                    caixaX + 10,
                    Math.max(20, atualY - 20)
            );
        }

        // Fecha o arquivo quando o aplicativo for encerrado.
        void fecharHistorico() {
            animacaoGrafico.stop();
            if (escritorHistorico == null) {
                return;
            }

            try {
                escritorHistorico.flush();
                escritorHistorico.close();

            } catch (IOException e) {
                System.err.println(
                        "Erro ao fechar o histórico da CPU: "
                                + e.getMessage()
                );

            } finally {
                escritorHistorico = null;
            }
        }
    }

}