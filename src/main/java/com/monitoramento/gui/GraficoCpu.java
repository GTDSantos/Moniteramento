package com.monitoramento.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Gráfico histórico da CPU, incluindo a animação e o CSV. */
public class GraficoCpu extends Pane {

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
