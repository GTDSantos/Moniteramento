package com.monitoramento.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Arrays;

/** Barras de uso médio da CPU agrupadas por hora local do dia. */
final class GraficoHorarios extends Pane {
    private final Canvas canvas = new Canvas();
    private final double[] medias = new double[24];
    private int horaDestaque = -1;

    GraficoHorarios() {
        getChildren().add(canvas);
        setMinHeight(170);
        setPrefHeight(190);
        widthProperty().addListener((obs, antigo, novo) -> desenhar());
        heightProperty().addListener((obs, antigo, novo) -> desenhar());
    }

    void setDados(double[] novasMedias) {
        Arrays.fill(medias, 0);
        horaDestaque = -1;
        if (novasMedias != null) {
            System.arraycopy(novasMedias, 0, medias, 0, Math.min(24, novasMedias.length));
            for (int i = 0; i < 24; i++) {
                if (medias[i] > 0 && (horaDestaque < 0 || medias[i] > medias[horaDestaque])) {
                    horaDestaque = i;
                }
            }
        }
        desenhar();
    }

    private void desenhar() {
        double largura = Math.max(1, getWidth());
        double altura = Math.max(1, getHeight());
        canvas.setWidth(largura);
        canvas.setHeight(altura);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, largura, altura);

        double topo = 12;
        double rodape = 28;
        double alturaBarras = altura - topo - rodape;
        double espaco = 4;
        double larguraBarra = Math.max(2, (largura - espaco * 23) / 24);

        gc.setFont(Font.font("Segoe UI", 10));
        gc.setFill(Color.web("#89939F"));
        for (int i = 0; i <= 4; i++) {
            double y = topo + i * alturaBarras / 4;
            gc.setStroke(Color.web("#30363E"));
            gc.strokeLine(0, y, largura, y);
            gc.fillText(String.valueOf(100 - i * 25), 2, y - 3);
        }

        for (int hora = 0; hora < 24; hora++) {
            double x = hora * (larguraBarra + espaco);
            double valor = Math.max(0, Math.min(100, medias[hora]));
            double alturaBarra = valor / 100 * alturaBarras;
            gc.setFill(valor == 0 ? Color.web("#272D35")
                    : (hora == horaDestaque ? Color.web("#86A0FF") : Color.web("#536FE0")));
            gc.fillRoundRect(x, topo + alturaBarras - alturaBarra,
                    larguraBarra, Math.max(2, alturaBarra), 4, 4);
            if (hora % 3 == 0) {
                gc.setFill(Color.web("#89939F"));
                gc.fillText(String.format("%02d h", hora), x - 2, altura - 7);
            }
        }
    }
}
