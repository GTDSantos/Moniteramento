package com.monitoramento.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MonitoramentoApp extends Application {

    @Override
    public void start(Stage stage) {

        // =========================
        // MENU LATERAL
        // =========================

        Label logo = new Label("PC MONITOR");
        logo.getStyleClass().add("logo");

        Label dashboard = new Label("Dashboard");
        dashboard.getStyleClass().add("menu-item");

        Label processos = new Label("Processos");
        processos.getStyleClass().add("menu-item");

        Label discos = new Label("Discos");
        discos.getStyleClass().add("menu-item");

        Label rede = new Label("Rede");
        rede.getStyleClass().add("menu-item");

        VBox menu = new VBox(
                20,
                logo,
                dashboard,
                processos,
                discos,
                rede
        );


        menu.setPadding(new Insets(30));
        menu.getStyleClass().add("menu");

        // =========================
        // TÍTULO
        // =========================

        Label titulo = new Label("Dashboard");
        titulo.getStyleClass().add("titulo");

        Label subtitulo = new Label("Monitoramento do seu computador");
        subtitulo.getStyleClass().add("subtitulo");

        VBox cabecalho = new VBox(
                5,
                titulo,
                subtitulo
        );

        // =========================
        // CARDS
        // =========================

        VBox cpu = criarCard(
                "CPU",
                "32%",
                "Uso do processador"
        );

        VBox ram = criarCard(
                "RAM",
                "51%",
                "Memória utilizada"
        );

        VBox disco = criarCard(
                "DISCO",
                "20 MB/s",
                "Velocidade"
        );

        VBox redeCard = criarCard(
                "REDE",
                "5 MB/s",
                "Download"
        );

        HBox cards = new HBox(
                20,
                cpu,
                ram,
                disco,
                redeCard
        );

        // =========================
        // PROCESSOS
        // =========================

        Label tituloProcessos = new Label("Processos");
        tituloProcessos.getStyleClass().add("secao-titulo");

        Label processo1 = new Label("Chrome                         12%");
        Label processo2 = new Label("Spotify                         5%");
        Label processo3 = new Label("IntelliJ IDEA                  8%");
        Label processo4 = new Label("Steam                           3%");

        processo1.getStyleClass().add("processo");
        processo2.getStyleClass().add("processo");
        processo3.getStyleClass().add("processo");
        processo4.getStyleClass().add("processo");

        VBox listaProcessos = new VBox(
                15,
                tituloProcessos,
                processo1,
                processo2,
                processo3,
                processo4
        );

        listaProcessos.setPadding(new Insets(25));
        listaProcessos.getStyleClass().add("processos-box");

        // =========================
        // CONTEÚDO PRINCIPAL
        // =========================

        VBox conteudo = new VBox(
                30,
                cabecalho,
                cards,
                listaProcessos
        );

        conteudo.setPadding(new Insets(35));

        // =========================
        // LAYOUT PRINCIPAL
        // =========================

        BorderPane layout = new BorderPane();

        layout.setLeft(menu);
        layout.setCenter(conteudo);

        // =========================
        // CENA
        // =========================

        Scene scene = new Scene(layout, 1100, 700);

        scene.getStylesheets().add(
                getClass()
                        .getResource("/com/monitoramento/style.css")
                        .toExternalForm()
        );

        stage.setTitle("PC Monitor");
        stage.setScene(scene);
        stage.show();
    }

    // Método para criar os cards
    private VBox criarCard(
            String titulo,
            String valor,
            String descricao
    ) {

        Label labelTitulo = new Label(titulo);
        labelTitulo.getStyleClass().add("card-titulo");

        Label labelValor = new Label(valor);
        labelValor.getStyleClass().add("card-valor");

        Label labelDescricao = new Label(descricao);
        labelDescricao.getStyleClass().add("card-descricao");

        VBox card = new VBox(
                10,
                labelTitulo,
                labelValor,
                labelDescricao
        );

        card.setPadding(new Insets(20));
        card.setPrefWidth(200);

        card.getStyleClass().add("card");

        return card;
    }
}