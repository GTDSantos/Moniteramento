package com.monitoramento.gui;

import com.monitoramento.hardwer.Monitoramento;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MenuLateral extends VBox {

    private final Monitoramento monitoramento;
    private final Label computadorNome;
    private final Label computadorTempo;

    public MenuLateral(
            Consumer<String> aoSelecionar,
            Monitoramento monitoramento
    ) {

        this.monitoramento = monitoramento;

        setSpacing(30);
        getStyleClass().add("menu");
        setPrefWidth(214);
        setMinWidth(214);

        VBox itens = new VBox(4);

        String[] nomes = {
                "Dashboard",
                "Processos",
                "Discos",
                "Rede",
                "Análises"
        };

        List<Label> botoes = new ArrayList<>();

        for (String nome : nomes) {

            Label item = label(nome, "menu-item");

            item.setMaxWidth(Double.MAX_VALUE);
            item.setCursor(Cursor.HAND);

            if (nome.equals("Dashboard")) {
                item.getStyleClass().add("ativo");
            }

            item.setOnMouseClicked(event -> {

                for (Label botao : botoes) {
                    botao.getStyleClass().remove("ativo");
                }

                item.getStyleClass().add("ativo");

                aoSelecionar.accept(nome);
            });

            botoes.add(item);
            itens.getChildren().add(item);
        }

        HBox marca = new HBox(
                10,
                ponto("marca", 18),
                label("PC Monitor", "logo")
        );

        marca.setAlignment(Pos.CENTER_LEFT);
        marca.setPadding(new Insets(0, 0, 0, 10));

        computadorNome = label(
                monitoramento.getNomeComputador(),
                "maquina-nome"
        );

        computadorTempo = label(
                "Calculando tempo ligado...",
                "maquina-info"
        );

        VBox detalhesComputador = new VBox(
                2,
                computadorNome,
                computadorTempo
        );

        HBox maquina = new HBox(
                10,
                ponto("ponto", 8),
                detalhesComputador
        );

        maquina.setAlignment(Pos.CENTER_LEFT);
        maquina.getStyleClass().add("maquina");

        Region espaco = new Region();
        VBox.setVgrow(espaco, Priority.ALWAYS);

        getChildren().addAll(
                marca,
                itens,
                espaco,
                maquina
        );

        atualizarInformacoesMaquina();
    }

    // Atualiza o tempo ligado.
    public void atualizarInformacoesMaquina() {

        computadorTempo.setText(
                "ligado há "
                        + formatarTempo(
                        monitoramento.getSystemUptimeSeconds()
                )
        );
    }

    private String formatarTempo(long totalSegundos) {

        long segundos = Math.max(0, totalSegundos);

        long dias = segundos / 86400;
        long horas = (segundos % 86400) / 3600;
        long minutos = (segundos % 3600) / 60;

        if (dias > 0) {
            return dias
                    + (dias == 1 ? " dia e " : " dias e ")
                    + horas + " h";
        }

        if (horas > 0) {
            return horas + " h e " + minutos + " min";
        }

        if (minutos > 0) {
            return minutos + " min";
        }

        return "menos de 1 min";
    }

    private static Region ponto(String classe, double tamanho) {

        Region region = new Region();

        region.setPrefSize(tamanho, tamanho);
        region.setMinSize(tamanho, tamanho);
        region.setMaxSize(tamanho, tamanho);

        region.getStyleClass().add(classe);

        return region;
    }

    private static Label label(String texto, String classe) {

        Label label = new Label(texto);
        label.getStyleClass().add(classe);

        return label;
    }
}