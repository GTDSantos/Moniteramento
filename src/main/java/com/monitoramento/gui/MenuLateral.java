package com.monitoramento.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Constrói o menu lateral da aplicação. */
public final class MenuLateral {

    private MenuLateral() {
    }

    // Mantém compatibilidade com chamadas antigas.
    public static VBox criar() {
        return criar(aba -> {
        });
    }

    // Cria o menu e avisa qual opção foi selecionada.
    public static VBox criar(Consumer<String> aoSelecionar) {
        VBox itens = new VBox(4);

        String[] nomes = {
                "Dashboard", "Processos", "Discos", "Rede"
        };

        List<Label> botoes = new ArrayList<>();

        for (int i = 0; i < nomes.length; i++) {
            String nome = nomes[i];

            Label l = label(nome, "menu-item");
            l.setMaxWidth(Double.MAX_VALUE);
            l.setCursor(Cursor.HAND);

            if (i == 0) {
                l.getStyleClass().add("ativo");
            }

            l.setOnMouseClicked(event -> {
                // Remove o destaque das opções anteriores.
                for (Label botao : botoes) {
                    botao.getStyleClass().remove("ativo");
                }

                // Destaca a opção clicada.
                l.getStyleClass().add("ativo");

                // Avisa a janela para trocar de tela.
                aoSelecionar.accept(nome);
            });

            botoes.add(l);
            itens.getChildren().add(l);
        }

        Region espaco = new Region();
        VBox.setVgrow(espaco, Priority.ALWAYS);

        HBox marca = new HBox(
                10,
                ponto("marca", 18),
                label("PC Monitor", "logo")
        );

        marca.setAlignment(Pos.CENTER_LEFT);
        marca.setPadding(new Insets(0, 0, 0, 10));

        HBox maquina = new HBox(
                10,
                ponto("ponto", 8),
                new VBox(
                        2,
                        label("DESKTOP-01", "maquina-nome"),
                        label("ligado há 3 dias e 4 h", "maquina-info")
                )
        );

        maquina.setAlignment(Pos.CENTER_LEFT);
        maquina.getStyleClass().add("maquina");

        VBox m = new VBox(30, marca, itens, espaco, maquina);

        m.getStyleClass().add("menu");
        m.setPrefWidth(214);
        m.setMinWidth(214);

        return m;
    }

    private static Region ponto(String classe, double tam) {
        Region r = new Region();
        r.setPrefSize(tam, tam);
        r.setMinSize(tam, tam);
        r.setMaxSize(tam, tam);
        r.getStyleClass().add(classe);

        return r;
    }

    private static Label label(String texto, String classe) {
        Label l = new Label(texto);
        l.getStyleClass().add(classe);

        return l;
    }
}