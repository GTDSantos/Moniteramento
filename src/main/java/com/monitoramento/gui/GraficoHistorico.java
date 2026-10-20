package com.monitoramento.gui;

import com.monitoramento.historico.HistoricoAmostra;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Gráfico leve para visualizar amostras históricas sem depender de bibliotecas extras. */
final class GraficoHistorico extends Pane {
    private static final int MAX_PONTOS = 520;
    private final Canvas canvas = new Canvas();
    private final ToDoubleFunction<HistoricoAmostra> valor;
    private final String cor;
    private final String unidade;
    private List<HistoricoAmostra> amostras = List.of();

    GraficoHistorico(ToDoubleFunction<HistoricoAmostra> valor, String cor, String unidade) {
        this.valor = valor;
        this.cor = cor;
        this.unidade = unidade;
        getChildren().add(canvas);
        setMinHeight(220);
        setPrefHeight(240);
        widthProperty().addListener((obs, anterior, atual) -> desenhar());
        heightProperty().addListener((obs, anterior, atual) -> desenhar());
    }

    void setAmostras(List<HistoricoAmostra> novasAmostras) {
        amostras = novasAmostras == null ? List.of() : new ArrayList<>(novasAmostras);
        desenhar();
    }

    private void desenhar() {
        double largura = Math.max(1, getWidth());
        double altura = Math.max(1, getHeight());
        canvas.setWidth(largura);
        canvas.setHeight(altura);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, largura, altura);

        double esquerda = 42;
        double direita = 12;
        double topo = 12;
        double rodape = 29;
        double graficoLargura = Math.max(1, largura - esquerda - direita);
        double graficoAltura = Math.max(1, altura - topo - rodape);

        gc.setFont(Font.font("Segoe UI", 10));
        gc.setStroke(Color.web("#30363E"));
        gc.setFill(Color.web("#89939F"));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double pct = 100 - i * 25;
            double y = topo + i * graficoAltura / 4;
            gc.strokeLine(esquerda, y, esquerda + graficoLargura, y);
            gc.fillText(String.format("%.0f%s", pct, unidade), 2, y + 3);
        }

        if (amostras.isEmpty()) {
            gc.setFill(Color.web("#89939F"));
            gc.setFont(Font.font("Segoe UI", 12));
            gc.fillText("Sem amostras nesse período", esquerda + 16, topo + graficoAltura / 2);
            return;
        }

        int quantidade = amostras.size();
        int passo = Math.max(1, (int) Math.ceil((double) quantidade / MAX_PONTOS));
        List<Double> valores = new ArrayList<>();
        List<Long> tempos = new ArrayList<>();
        for (int inicio = 0; inicio < quantidade; inicio += passo) {
            int fim = Math.min(quantidade, inicio + passo);
            // Usa o pico de cada pequeno bloco para não esconder picos importantes.
            double pico = 0;
            long instante = amostras.get(inicio).getInstanteMs();
            for (int i = inicio; i < fim; i++) {
                double leitura = valor.applyAsDouble(amostras.get(i));
                if (Double.isFinite(leitura) && leitura >= pico) {
                    pico = leitura;
                    instante = amostras.get(i).getInstanteMs();
                }
            }
            valores.add(Math.max(0, Math.min(100, pico)));
            tempos.add(instante);
        }

        long inicioMs = amostras.get(0).getInstanteMs();
        long fimMs = amostras.get(quantidade - 1).getInstanteMs();
        long duracao = Math.max(1, fimMs - inicioMs);
        gc.setStroke(Color.web(cor));
        gc.setLineWidth(2.2);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        double anteriorX = -1;
        double anteriorY = -1;
        for (int i = 0; i < valores.size(); i++) {
            double x = esquerda + ((double) (tempos.get(i) - inicioMs) / duracao) * graficoLargura;
            double y = topo + ((100 - valores.get(i)) / 100.0) * graficoAltura;
            if (anteriorX >= 0) gc.strokeLine(anteriorX, anteriorY, x, y);
            anteriorX = x;
            anteriorY = y;
        }

        DateTimeFormatter formato = duracao > 24L * 60 * 60 * 1000
                ? DateTimeFormatter.ofPattern("dd/MM")
                : DateTimeFormatter.ofPattern("HH:mm");
        ZoneId zona = ZoneId.systemDefault();
        gc.setFill(Color.web("#89939F"));
        gc.fillText(Instant.ofEpochMilli(inicioMs).atZone(zona).format(formato),
                esquerda, altura - 7);
        String meio = Instant.ofEpochMilli(inicioMs + duracao / 2).atZone(zona).format(formato);
        gc.fillText(meio, esquerda + graficoLargura / 2 - 16, altura - 7);
        String fim = Instant.ofEpochMilli(fimMs).atZone(zona).format(formato);
        gc.fillText(fim, esquerda + graficoLargura - 32, altura - 7);
    }
}
