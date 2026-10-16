package com.monitoramento.gui;

import com.monitoramento.model.ProcesInfo;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tela completa de processos.
 * Recebe as leituras do MonitoramentoApp e as exibe em uma tabela.
 */
public class ProcessosPage extends VBox {

    // Lista original de processos recebidos do monitoramento.
    private final ObservableList<ProcesInfo> dados =
            FXCollections.observableArrayList();

    // Lista filtrada conforme o texto digitado na pesquisa.
    private final FilteredList<ProcesInfo> filtrados =
            new FilteredList<>(dados, processo -> true);

    private final TableView<ProcesInfo> tabela = new TableView<>();
    private final TextField campoPesquisa = new TextField();

    private final Label quantidadeValor = new Label("0");
    private final Label maiorCpuValor = new Label("--");
    private final Label maiorMemoriaValor = new Label("--");

    public ProcessosPage() {
        getStyleClass().addAll("folha", "processos-pagina");
        setPadding(new Insets(30, 38, 26, 38));
        setSpacing(24);
        setFillWidth(true);

        // ---------- Cabeçalho ----------

        VBox textosCabecalho = new VBox(
                6,
                label("Processos", "titulo"),
                label(
                        "Acompanhe os processos ativos e os recursos utilizados.",
                        "subtitulo"
                )
        );

        campoPesquisa.setPromptText("Pesquisar por nome ou PID...");
        campoPesquisa.getStyleClass().add("processos-pesquisa");
        campoPesquisa.setPrefWidth(300);
        campoPesquisa.setMaxWidth(360);

        HBox cabecalho = new HBox(
                20,
                textosCabecalho,
                campoPesquisa
        );

        cabecalho.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textosCabecalho, Priority.ALWAYS);

        // ---------- Indicadores ----------

        HBox indicadores = new HBox(
                14,
                cartao("Processos encontrados", quantidadeValor),
                cartao("Maior uso de CPU", maiorCpuValor),
                cartao("Maior uso de memória", maiorMemoriaValor)
        );

        // ---------- Tabela ----------

        configurarTabela();

        // A pesquisa pode localizar tanto o nome quanto o PID.
        campoPesquisa.textProperty().addListener(
                (observavel, textoAntigo, textoNovo) -> {
                    String termo = textoNovo == null
                            ? ""
                            : textoNovo.trim().toLowerCase(Locale.ROOT);

                    filtrados.setPredicate(processo -> {
                        if (termo.isEmpty()) {
                            return true;
                        }

                        String nome = processo.getNome() == null
                                ? ""
                                : processo.getNome().toLowerCase(Locale.ROOT);

                        String pid = String.valueOf(processo.getId());

                        return nome.contains(termo)
                                || pid.contains(termo);
                    });
                }
        );

        VBox.setVgrow(tabela, Priority.ALWAYS);

        getChildren().addAll(
                cabecalho,
                indicadores,
                tabela
        );
    }

    private void configurarTabela() {
        tabela.getStyleClass().add("processos-tabela");
        tabela.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        tabela.setPlaceholder(
                label("Nenhum processo encontrado.", "nota")
        );

        // Coluna do nome do processo.
        TableColumn<ProcesInfo, String> nomeColuna =
                new TableColumn<>("Processo");

        nomeColuna.setCellValueFactory(celula ->
                new ReadOnlyObjectWrapper<>(
                        celula.getValue().getNome()
                )
        );

        nomeColuna.setPrefWidth(400);
        nomeColuna.setMinWidth(180);

        // Coluna do identificador do processo.
        TableColumn<ProcesInfo, Integer> pidColuna =
                new TableColumn<>("PID");

        pidColuna.setCellValueFactory(celula ->
                new ReadOnlyObjectWrapper<>(
                        celula.getValue().getId()
                )
        );

        pidColuna.setPrefWidth(100);

        // Coluna de CPU: mantém o valor numérico para ordenar corretamente.
        TableColumn<ProcesInfo, Double> cpuColuna =
                new TableColumn<>("CPU");

        cpuColuna.setCellValueFactory(celula ->
                new ReadOnlyObjectWrapper<>(
                        cpuSeguro(celula.getValue())
                )
        );

        cpuColuna.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(Double uso, boolean vazio) {
                super.updateItem(uso, vazio);

                if (vazio || uso == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f%%", uso));
                }

                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        cpuColuna.setPrefWidth(130);
        cpuColuna.setSortType(
                TableColumn.SortType.DESCENDING
        );

        // Coluna de memória residente em MB.
        TableColumn<ProcesInfo, Long> memoriaColuna =
                new TableColumn<>("Memória");

        memoriaColuna.setCellValueFactory(celula ->
                new ReadOnlyObjectWrapper<>(
                        celula.getValue().getMemory()
                )
        );

        memoriaColuna.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(Long memoria, boolean vazio) {
                super.updateItem(memoria, vazio);

                if (vazio || memoria == null) {
                    setText(null);
                } else {
                    setText(formatarMemoria(memoria));
                }

                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        memoriaColuna.setPrefWidth(160);

        tabela.getColumns().addAll(
                nomeColuna,
                pidColuna,
                cpuColuna,
                memoriaColuna
        );

        // Começa mostrando os maiores consumidores de CPU.
        tabela.getSortOrder().add(cpuColuna);

        // Faz a tabela respeitar o filtro e a ordenação.
        SortedList<ProcesInfo> ordenados =
                new SortedList<>(filtrados);

        ordenados.comparatorProperty().bind(
                tabela.comparatorProperty()
        );

        tabela.setItems(ordenados);
    }

    /**
     * Recebe uma fotografia dos processos coletada pelo monitor principal.
     */
    public void atualizarProcessos(List<ProcesInfo> processosRecebidos) {
        if (processosRecebidos == null) {
            return;
        }

        // Criamos uma cópia para não alterar a lista original.
        List<ProcesInfo> processos =
                new ArrayList<>(processosRecebidos);

        // Não exibe o Idle como se fosse um programa consumidor de CPU.
        processos.removeIf(processo -> {
            if (processo == null || processo.getNome() == null) {
                return true;
            }

            String nome = processo.getNome();

            return nome.equalsIgnoreCase("Idle")
                    || nome.equalsIgnoreCase("System Idle Process");
        });

        dados.setAll(processos);

        quantidadeValor.setText(
                String.valueOf(processos.size())
        );

        if (processos.isEmpty()) {
            maiorCpuValor.setText("--");
            maiorMemoriaValor.setText("--");
            return;
        }

        // Encontra o processo com maior uso de CPU.
        ProcesInfo maiorCpu = processos.get(0);

        for (ProcesInfo processo : processos) {
            if (cpuSeguro(processo) > cpuSeguro(maiorCpu)) {
                maiorCpu = processo;
            }
        }

        maiorCpuValor.setText(
                String.format("%.1f%%", cpuSeguro(maiorCpu))
        );

        // Encontra o processo com maior memória residente.
        ProcesInfo maiorMemoria = processos.get(0);

        for (ProcesInfo processo : processos) {
            if (processo.getMemory() > maiorMemoria.getMemory()) {
                maiorMemoria = processo;
            }
        }

        maiorMemoriaValor.setText(
                formatarMemoria(maiorMemoria.getMemory())
        );
    }

    private double cpuSeguro(ProcesInfo processo) {
        double cpu = processo.getCpu();

        if (!Double.isFinite(cpu)) {
            return 0;
        }

        return Math.max(0, cpu);
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

    private VBox cartao(String titulo, Label valor) {
        Label nome = label(titulo, "processos-stat-titulo");
        valor.getStyleClass().add("processos-stat-valor");

        VBox cartao = new VBox(8, nome, valor);
        cartao.getStyleClass().add("processos-stat-card");

        HBox.setHgrow(cartao, Priority.ALWAYS);

        return cartao;
    }

    private Label label(String texto, String classe) {
        Label label = new Label(texto);
        label.getStyleClass().add(classe);
        return label;
    }
}