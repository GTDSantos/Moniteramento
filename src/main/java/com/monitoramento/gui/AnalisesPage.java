package com.monitoramento.gui;

import com.monitoramento.historico.HistoricoAmostra;
import com.monitoramento.historico.HistoricoMonitoramento;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/** Página de análise das amostras persistidas pelo PC Monitor. */
public final class AnalisesPage extends ScrollPane {
    private static final long DUAS_HORAS = 2L * 60 * 60 * 1000;
    private static final long VINTE_QUATRO_HORAS = 24L * 60 * 60 * 1000;
    private static final long SETE_DIAS = 7L * 24 * 60 * 60 * 1000;
    private static final ZoneId ZONA_LOCAL = ZoneId.systemDefault();
    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss");
    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final HistoricoMonitoramento historico;
    private final VBox conteudo = new VBox(18);
    private final List<Button> botoesPeriodo = new ArrayList<>();
    private final Button duasHorasBotao;
    private final Button vinteQuatroBotao;
    private final Button seteDiasBotao;
    private final Button exportarBotao;
    private final Label statusPeriodo;
    private final Label estadoHistorico;
    private Region indicadorEstadoHistorico;
    private final Label horaMaiorUso;
    private final Label resumoRede;
    private final Label avisoSemDados;

    private final Label cpuMediaValor;
    private final Label cpuPicoValor;
    private final Label cpuPicoDetalhe;
    private final Label ramMediaValor;
    private final Label ramPicoValor;
    private final Label ramPicoDetalhe;

    private final GraficoHistorico graficoCpu =
            new GraficoHistorico(HistoricoAmostra::getCpuPercentual, "#8298FF", "%");
    private final GraficoHistorico graficoRam =
            new GraficoHistorico(HistoricoAmostra::getRamPercentual, "#4FBE8C", "%");
    private final GraficoHorarios graficoHorarios = new GraficoHorarios();
    private final VBox listaAlertas = new VBox(8);
    private final TableView<ProcessoResumo> tabelaProcessos = new TableView<>();

    private long periodoMs = VINTE_QUATRO_HORAS;
    private String periodoSelecionado = "24 horas";
    private List<HistoricoAmostra> amostrasAtuais = List.of();
    private List<AlertaHistorico> alertasAtuais = List.of();
    private List<ProcessoResumo> processosAtuais = List.of();

    public AnalisesPage(HistoricoMonitoramento historico) {
        this.historico = historico;

        getStyleClass().add("analises-scroll");
        setFitToWidth(true);
        setPannable(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        conteudo.getStyleClass().add("analises-pagina");
        conteudo.setPadding(new Insets(30, 32, 30, 32));

        Label titulo = label("Análises", "analises-titulo");
        Label subtitulo = label(
                "Entenda como o computador se comportou, encontre picos de uso e identifique sinais de sobrecarga.",
                "analises-subtitulo"
        );
        VBox cabecalho = new VBox(5, titulo, subtitulo);

        estadoHistorico = label("", "analises-status");
        indicadorEstadoHistorico = new Region();
        indicadorEstadoHistorico.setPrefSize(7, 7);
        indicadorEstadoHistorico.setMinSize(7, 7);
        indicadorEstadoHistorico.setMaxSize(7, 7);
        indicadorEstadoHistorico.getStyleClass().add("analises-status-ponto");
        HBox barraStatus = new HBox(8, indicadorEstadoHistorico, estadoHistorico);
        barraStatus.setAlignment(Pos.CENTER_LEFT);
        barraStatus.getStyleClass().add("analises-status-box");

        duasHorasBotao = botaoPeriodo("2 horas", false);
        vinteQuatroBotao = botaoPeriodo("24 horas", true);
        seteDiasBotao = botaoPeriodo("7 dias", false);
        botoesPeriodo.addAll(List.of(duasHorasBotao, vinteQuatroBotao, seteDiasBotao));
        HBox grupoPeriodo = new HBox(4, duasHorasBotao, vinteQuatroBotao, seteDiasBotao);
        grupoPeriodo.getStyleClass().add("analises-periodo-grupo");

        Button atualizar = new Button("Atualizar");
        atualizar.getStyleClass().add("analises-botao-secundario");
        atualizar.setOnAction(evento -> atualizarAnalise());

        exportarBotao = new Button("Exportar relatório");
        exportarBotao.getStyleClass().add("analises-botao-principal");
        exportarBotao.setOnAction(evento -> exportarRelatorio());
        exportarBotao.setDisable(true);

        HBox acoes = new HBox(8, atualizar, exportarBotao);
        acoes.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(grupoPeriodo, Priority.ALWAYS);
        HBox barraFerramentas = new HBox(12, grupoPeriodo, acoes);
        barraFerramentas.setAlignment(Pos.CENTER_LEFT);

        statusPeriodo = label("Aguardando dados…", "analises-nota");
        VBox periodoBox = new VBox(9, barraFerramentas, statusPeriodo, barraStatus);
        periodoBox.getStyleClass().add("analises-toolbar-card");

        cpuMediaValor = new Label("—");
        cpuPicoValor = new Label("—");
        cpuPicoDetalhe = new Label("Sem dados");
        cpuPicoDetalhe.setWrapText(true);
        ramMediaValor = new Label("—");
        ramPicoValor = new Label("—");
        ramPicoDetalhe = new Label("Sem dados");
        ramPicoDetalhe.setWrapText(true);

        HBox indicadores = new HBox(12,
                indicador("CPU média", cpuMediaValor, "no período", "analises-card-azul"),
                indicador("Pico de CPU", cpuPicoValor, cpuPicoDetalhe, "analises-card-azul"),
                indicador("RAM média", ramMediaValor, "memória utilizada", "analises-card-verde"),
                indicador("Pico de RAM", ramPicoValor, ramPicoDetalhe, "analises-card-verde")
        );
        indicadores.getStyleClass().add("analises-indicadores");

        VBox cpuCard = card(
                new VBox(3, label("Uso da CPU", "analises-card-titulo"),
                        label("Picos e variações ao longo do período", "analises-card-legenda")),
                graficoCpu
        );
        VBox ramCard = card(
                new VBox(3, label("Uso da memória RAM", "analises-card-titulo"),
                        label("Percentual de memória em uso", "analises-card-legenda")),
                graficoRam
        );
        HBox graficos = new HBox(14, cpuCard, ramCard);
        HBox.setHgrow(cpuCard, Priority.ALWAYS);
        HBox.setHgrow(ramCard, Priority.ALWAYS);
        cpuCard.setMaxWidth(Double.MAX_VALUE);
        ramCard.setMaxWidth(Double.MAX_VALUE);
        cpuCard.setMinWidth(0);
        ramCard.setMinWidth(0);

        horaMaiorUso = label("Aguardando dados para calcular o horário de pico.", "analises-hora-pico");
        resumoRede = label("Rede: sem dados no período.", "analises-card-legenda");
        VBox horarioCard = card(
                new VBox(3, label("Horários de maior uso", "analises-card-titulo"),
                        label("Média da CPU por faixa horária do dia (00h–23h), no período escolhido", "analises-card-legenda")),
                graficoHorarios,
                horaMaiorUso,
                resumoRede
        );

        avisoSemDados = label(
                "Ainda não há amostras para este período. Ative “Salvar histórico” no Dashboard e mantenha o PC Monitor aberto para começar a registrar dados.",
                "analises-sem-dados"
        );
        avisoSemDados.setWrapText(true);

        VBox alertasCard = card(
                new VBox(3, label("Alertas inteligentes", "analises-card-titulo"),
                        label("Sinais encontrados nas amostras salvas; alertas persistentes exigem leituras consecutivas.",
                                "analises-card-legenda")),
                listaAlertas
        );

        prepararTabelaProcessos();
        VBox processosCard = card(
                new VBox(3, label("Processos mais exigentes", "analises-card-titulo"),
                        label("Resumo dos processos que apareceram entre os 10 maiores consumidores registrados.",
                                "analises-card-legenda")),
                tabelaProcessos
        );

        conteudo.getChildren().addAll(
                cabecalho,
                periodoBox,
                avisoSemDados,
                indicadores,
                label("Linha do tempo", "analises-secao"),
                graficos,
                horarioCard,
                alertasCard,
                processosCard
        );
        setContent(conteudo);
        // A leitura dos CSVs é adiada até o usuário abrir a aba, para não atrasar a inicialização.
    }

    /** Recarrega do CSV apenas a janela de tempo escolhida. */
    public void atualizarAnalise() {
        long agora = System.currentTimeMillis();
        amostrasAtuais = historico.carregarAmostrasDesde(agora - periodoMs);
        graficoCpu.setAmostras(amostrasAtuais);
        graficoRam.setAmostras(amostrasAtuais);

        boolean ativo = historico.isHabilitado();
        estadoHistorico.setText(ativo
                ? "Gravação ativa · novas amostras a cada 5 segundos"
                : "Gravação pausada · exibindo somente dados já salvos");
        estadoHistorico.getStyleClass().removeAll("analises-status-ativo", "analises-status-inativo");
        estadoHistorico.getStyleClass().add(ativo ? "analises-status-ativo" : "analises-status-inativo");
        indicadorEstadoHistorico.getStyleClass().removeAll(
                "analises-status-ponto-ativo", "analises-status-ponto-inativo");
        indicadorEstadoHistorico.getStyleClass().add(
                ativo ? "analises-status-ponto-ativo" : "analises-status-ponto-inativo");

        statusPeriodo.setText(amostrasAtuais.size() + " amostra(s) analisada(s) · janela de "
                + periodoSelecionado + " · última leitura: "
                + (amostrasAtuais.isEmpty() ? "—" : formatarDataHora(amostrasAtuais.get(amostrasAtuais.size() - 1).getInstanteMs())));
        avisoSemDados.setVisible(amostrasAtuais.isEmpty());
        avisoSemDados.setManaged(amostrasAtuais.isEmpty());
        exportarBotao.setDisable(amostrasAtuais.isEmpty());

        atualizarIndicadores();
        atualizarHorarios();
        atualizarAlertas();
        atualizarTabelaProcessos();
    }

    private void atualizarIndicadores() {
        if (amostrasAtuais.isEmpty()) {
            cpuMediaValor.setText("—");
            cpuPicoValor.setText("—");
            cpuPicoDetalhe.setText("Sem dados");
            ramMediaValor.setText("—");
            ramPicoValor.setText("—");
            ramPicoDetalhe.setText("Sem dados");
            return;
        }

        double mediaCpu = amostrasAtuais.stream().mapToDouble(HistoricoAmostra::getCpuPercentual).average().orElse(0);
        double mediaRam = amostrasAtuais.stream().mapToDouble(HistoricoAmostra::getRamPercentual).average().orElse(0);
        HistoricoAmostra picoCpu = amostrasAtuais.stream()
                .max(Comparator.comparingDouble(HistoricoAmostra::getCpuPercentual)).orElseThrow();
        HistoricoAmostra picoRam = amostrasAtuais.stream()
                .max(Comparator.comparingDouble(HistoricoAmostra::getRamPercentual)).orElseThrow();

        cpuMediaValor.setText(String.format(Locale.ROOT, "%.1f%%", mediaCpu));
        cpuPicoValor.setText(String.format(Locale.ROOT, "%.1f%%", picoCpu.getCpuPercentual()));
        cpuPicoDetalhe.setText(formatarDataHora(picoCpu.getInstanteMs()));
        ramMediaValor.setText(String.format(Locale.ROOT, "%.1f%%", mediaRam));
        ramPicoValor.setText(String.format(Locale.ROOT, "%.1f%%", picoRam.getRamPercentual()));
        ramPicoDetalhe.setText(String.format(Locale.ROOT, "%s · %.1f de %.1f GB",
                formatarDataHora(picoRam.getInstanteMs()), picoRam.getRamUsadaGb(), picoRam.getRamTotalGb()));
    }

    private void atualizarHorarios() {
        double[] soma = new double[24];
        int[] contagem = new int[24];
        double downloadMax = 0;
        double uploadMax = 0;
        for (HistoricoAmostra amostra : amostrasAtuais) {
            int hora = Instant.ofEpochMilli(amostra.getInstanteMs()).atZone(ZONA_LOCAL).getHour();
            soma[hora] += amostra.getCpuPercentual();
            contagem[hora]++;
            downloadMax = Math.max(downloadMax, amostra.getDownloadMbS());
            uploadMax = Math.max(uploadMax, amostra.getUploadMbS());
        }

        double[] medias = new double[24];
        int horaPico = -1;
        for (int hora = 0; hora < 24; hora++) {
            if (contagem[hora] > 0) medias[hora] = soma[hora] / contagem[hora];
            if (contagem[hora] > 0 && (horaPico < 0 || medias[hora] > medias[horaPico])) {
                horaPico = hora;
            }
        }
        graficoHorarios.setDados(medias);
        if (horaPico < 0) {
            horaMaiorUso.setText("Não há amostras suficientes para identificar um horário de maior uso.");
        } else {
            horaMaiorUso.setText(String.format(Locale.ROOT,
                    "Maior média de CPU: %02d:00–%02d:00 · %.1f%% · %d amostra(s) nessa faixa",
                    horaPico, (horaPico + 1) % 24, medias[horaPico], contagem[horaPico]));
        }
        resumoRede.setText(String.format(Locale.ROOT,
                "Picos de tráfego registrados · download %.2f MB/s · upload %.2f MB/s",
                downloadMax, uploadMax));
    }

    private void atualizarAlertas() {
        listaAlertas.getChildren().clear();
        alertasAtuais = calcularAlertas(amostrasAtuais);

        if (amostrasAtuais.isEmpty()) {
            listaAlertas.getChildren().add(alertaVisual("info", "Aguardando histórico",
                    "Ative a gravação no Dashboard. Os alertas serão calculados a partir das leituras salvas."));
            return;
        }

        if (alertasAtuais.isEmpty()) {
            listaAlertas.getChildren().add(alertaVisual("ok", "Nenhum padrão crítico identificado",
                    "Nas amostras disponíveis, não foi detectado uso persistentemente alto de CPU/RAM nem volume acima do limite de atenção."));
            return;
        }

        for (AlertaHistorico alerta : alertasAtuais) {
            listaAlertas.getChildren().add(alertaVisual(alerta.nivel, alerta.titulo, alerta.descricao));
        }
    }

    private List<AlertaHistorico> calcularAlertas(List<HistoricoAmostra> dados) {
        List<AlertaHistorico> resultado = new ArrayList<>();
        if (dados.isEmpty()) return resultado;

        adicionarAlertaPersistente(resultado, dados, HistoricoAmostra::getCpuPercentual,
                85, 3, "CPU", "alta", "Uso da CPU elevado por períodos persistentes");
        adicionarAlertaPersistente(resultado, dados, HistoricoAmostra::getRamPercentual,
                90, 3, "RAM", "alta", "Memória RAM próxima do limite");

        double maiorVolume = 0;
        String nomeVolume = "Volume";
        long instanteVolume = 0;
        int amostrasVolumeAlta = 0;
        for (HistoricoAmostra amostra : dados) {
            boolean existeVolumeAltoNestaAmostra = false;
            for (HistoricoAmostra.VolumeHistorico volume : amostra.getVolumes()) {
                if (volume.getUsoPercentual() >= 85) existeVolumeAltoNestaAmostra = true;
                if (volume.getUsoPercentual() > maiorVolume) {
                    maiorVolume = volume.getUsoPercentual();
                    nomeVolume = volume.getNome();
                    instanteVolume = amostra.getInstanteMs();
                }
            }
            if (existeVolumeAltoNestaAmostra) amostrasVolumeAlta++;
        }
        if (maiorVolume >= 85) {
            resultado.add(new AlertaHistorico("aviso", "Armazenamento com pouco espaço livre",
                    String.format(Locale.ROOT, "%s chegou a %.1f%% de ocupação em %s. "
                                    + "Amostras com volume acima de 85%%: %d.",
                            nomeVolume, maiorVolume, formatarDataHora(instanteVolume), amostrasVolumeAlta)));
        }

        List<ProcessoResumo> resumo = resumirProcessos(dados);
        if (!resumo.isEmpty()) {
            ProcessoResumo processo = resumo.get(0);
            if (processo.getCpuMedia() >= 30 && processo.aparicoes >= 3) {
                resultado.add(new AlertaHistorico("aviso", "Processo com consumo recorrente de CPU",
                        String.format(Locale.ROOT, "%s (PID %d) teve média de %.1f%% nas %d amostras "
                                        + "em que apareceu entre os 10 principais consumidores.",
                                processo.nome, processo.pid, processo.getCpuMedia(), processo.aparicoes)));
            }
        }

        if (dados.size() < 3 && resultado.isEmpty()) {
            resultado.add(new AlertaHistorico("info", "Poucas amostras disponíveis",
                    "Ainda não há três leituras consecutivas para avaliar sobrecarga persistente. Deixe o histórico ativo por mais tempo."));
        }
        return resultado;
    }

    private void adicionarAlertaPersistente(List<AlertaHistorico> destino,
                                             List<HistoricoAmostra> dados,
                                             ToDoubleFunction<HistoricoAmostra> valor,
                                             double limite, int minimoConsecutivo,
                                             String metrica, String nivel, String titulo) {
        int sequencia = 0;
        int maiorSequencia = 0;
        int ocorrencias = 0;
        double pico = 0;
        long inicioMaior = 0;
        long fimMaior = 0;
        long inicioAtual = 0;
        long anteriorMs = 0;
        double picoAtual = 0;

        for (HistoricoAmostra amostra : dados) {
            double leitura = valor.applyAsDouble(amostra);
            boolean lacuna = anteriorMs != 0 && amostra.getInstanteMs() - anteriorMs > 12_000;
            if (lacuna || leitura < limite) {
                if (sequencia >= minimoConsecutivo) {
                    ocorrencias++;
                    if (sequencia > maiorSequencia) {
                        maiorSequencia = sequencia;
                        inicioMaior = inicioAtual;
                        fimMaior = anteriorMs;
                        pico = picoAtual;
                    }
                }
                sequencia = 0;
                picoAtual = 0;
            }
            if (leitura >= limite) {
                if (sequencia == 0) inicioAtual = amostra.getInstanteMs();
                sequencia++;
                picoAtual = Math.max(picoAtual, leitura);
            }
            anteriorMs = amostra.getInstanteMs();
        }
        if (sequencia >= minimoConsecutivo) {
            ocorrencias++;
            if (sequencia > maiorSequencia) {
                maiorSequencia = sequencia;
                inicioMaior = inicioAtual;
                fimMaior = anteriorMs;
                pico = picoAtual;
            }
        }

        if (ocorrencias > 0) {
            String descricao = String.format(Locale.ROOT,
                    "%s ficou em %.0f%% ou mais em %d sequência(s) de pelo menos %d amostras. "
                            + "Maior sequência: %d leituras (pico %.1f%%), de %s a %s.",
                    metrica, limite, ocorrencias, minimoConsecutivo, maiorSequencia, pico,
                    formatarHoraOuData(inicioMaior, dados), formatarHoraOuData(fimMaior, dados));
            destino.add(new AlertaHistorico(nivel, titulo, descricao));
        }
    }

    private String formatarHoraOuData(long instante, List<HistoricoAmostra> dados) {
        if (dados.isEmpty()) return "—";
        long duracao = dados.get(dados.size() - 1).getInstanteMs() - dados.get(0).getInstanteMs();
        return Instant.ofEpochMilli(instante).atZone(ZONA_LOCAL)
                .format(duracao > VINTE_QUATRO_HORAS
                        ? DateTimeFormatter.ofPattern("dd/MM HH:mm:ss") : FORMATO_HORA);
    }

    private void prepararTabelaProcessos() {
        tabelaProcessos.getStyleClass().add("analises-tabela");
        tabelaProcessos.setPlaceholder(label("Nenhum processo foi registrado nesse período.", "analises-card-legenda"));
        tabelaProcessos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaProcessos.setPrefHeight(300);
        tabelaProcessos.setMinHeight(260);

        TableColumn<ProcessoResumo, String> nome = coluna("Processo", ProcessoResumo::getNomeExibicao, 220);
        TableColumn<ProcessoResumo, String> media = coluna("CPU média", p -> String.format(Locale.ROOT, "%.1f%%", p.getCpuMedia()), 105);
        TableColumn<ProcessoResumo, String> pico = coluna("Pico CPU", p -> String.format(Locale.ROOT, "%.1f%%", p.picoCpu), 95);
        TableColumn<ProcessoResumo, String> memoria = coluna("RAM máx.", p -> String.format(Locale.ROOT, "%.0f MB", p.memoriaMaxMb), 110);
        TableColumn<ProcessoResumo, String> amostras = coluna("Aparições", p -> String.valueOf(p.aparicoes), 90);
        tabelaProcessos.getColumns().setAll(nome, media, pico, memoria, amostras);
    }

    private TableColumn<ProcessoResumo, String> coluna(
            String titulo, java.util.function.Function<ProcessoResumo, String> texto, double largura) {
        TableColumn<ProcessoResumo, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(celula -> new SimpleStringProperty(texto.apply(celula.getValue())));
        coluna.setPrefWidth(largura);
        return coluna;
    }

    private void atualizarTabelaProcessos() {
        processosAtuais = resumirProcessos(amostrasAtuais);
        tabelaProcessos.setItems(FXCollections.observableArrayList(processosAtuais));
    }

    private List<ProcessoResumo> resumirProcessos(List<HistoricoAmostra> dados) {
        List<ProcessoResumo> resultado = new ArrayList<>();
        for (HistoricoMonitoramento.ResumoProcesso processo
                : historico.getResumoProcessosUltimaLeitura()) {
            resultado.add(new ProcessoResumo(
                    processo.getNome(), processo.getPid(), processo.getCpuMedia(),
                    processo.getPicoCpu(), processo.getMemoriaMaxMb(), processo.getAparicoes()
            ));
        }
        resultado.sort(Comparator.comparingDouble(ProcessoResumo::getCpuMedia).reversed());
        return resultado;
    }

    private void exportarRelatorio() {
        if (amostrasAtuais.isEmpty() || getScene() == null) return;
        FileChooser seletor = new FileChooser();
        seletor.setTitle("Exportar relatório do PC Monitor");
        seletor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo CSV", "*.csv"));
        seletor.setInitialFileName("relatorio_pc_monitor.csv");
        Window janela = getScene().getWindow();
        java.io.File selecionado = seletor.showSaveDialog(janela);
        if (selecionado == null) return;

        try (BufferedWriter escritor = Files.newBufferedWriter(selecionado.toPath(), StandardCharsets.UTF_8)) {
            escritor.write("\uFEFF"); // BOM para abrir acentos corretamente no Excel.
            escreverCsv(escritor, "Relatório PC Monitor", "Período", periodoSelecionado);
            escreverCsv(escritor, "Amostras analisadas", String.valueOf(amostrasAtuais.size()));
            escreverCsv(escritor, "CPU média (%)", textoNumero(mediaCpu()));
            escreverCsv(escritor, "CPU pico (%)", textoNumero(picoCpu().getCpuPercentual()), formatarDataHora(picoCpu().getInstanteMs()));
            escreverCsv(escritor, "RAM média (%)", textoNumero(mediaRam()));
            escreverCsv(escritor, "RAM pico (%)", textoNumero(picoRam().getRamPercentual()), formatarDataHora(picoRam().getInstanteMs()));
            escreverCsv(escritor, "Horário de maior uso", horaMaiorUso.getText());
            escritor.newLine();
            escreverCsv(escritor, "ALERTAS INTELIGENTES");
            escreverCsv(escritor, "Nível", "Alerta", "Detalhes");
            for (AlertaHistorico alerta : alertasAtuais) {
                escreverCsv(escritor, alerta.nivel, alerta.titulo, alerta.descricao);
            }
            escritor.newLine();
            escreverCsv(escritor, "PROCESSOS MAIS EXIGENTES");
            escreverCsv(escritor, "Processo", "PID", "CPU média (%)", "Pico CPU (%)", "RAM máx. (MB)", "Aparições");
            for (ProcessoResumo processo : processosAtuais) {
                escreverCsv(escritor, processo.nome, String.valueOf(processo.pid),
                        textoNumero(processo.getCpuMedia()), textoNumero(processo.picoCpu),
                        textoNumero(processo.memoriaMaxMb), String.valueOf(processo.aparicoes));
            }
            escritor.newLine();
            escreverCsv(escritor, "AMOSTRAS DO PERÍODO");
            escreverCsv(escritor, "Data/hora", "CPU (%)", "RAM usada (GB)", "RAM total (GB)",
                    "RAM (%)", "Download (MB/s)", "Upload (MB/s)", "Maior uso de volume (%)");
            for (HistoricoAmostra amostra : amostrasAtuais) {
                escreverCsv(escritor, formatarDataHora(amostra.getInstanteMs()),
                        textoNumero(amostra.getCpuPercentual()), textoNumero(amostra.getRamUsadaGb()),
                        textoNumero(amostra.getRamTotalGb()), textoNumero(amostra.getRamPercentual()),
                        textoNumero(amostra.getDownloadMbS()), textoNumero(amostra.getUploadMbS()),
                        textoNumero(amostra.getMaiorUsoVolume()));
            }
            statusPeriodo.setText("Relatório exportado para: " + selecionado.getAbsolutePath());
        } catch (IOException e) {
            statusPeriodo.setText("Não foi possível exportar o relatório: " + e.getMessage());
        }
    }

    private double mediaCpu() {
        return amostrasAtuais.stream().mapToDouble(HistoricoAmostra::getCpuPercentual).average().orElse(0);
    }
    private double mediaRam() {
        return amostrasAtuais.stream().mapToDouble(HistoricoAmostra::getRamPercentual).average().orElse(0);
    }
    private HistoricoAmostra picoCpu() {
        return amostrasAtuais.stream().max(Comparator.comparingDouble(HistoricoAmostra::getCpuPercentual)).orElseThrow();
    }
    private HistoricoAmostra picoRam() {
        return amostrasAtuais.stream().max(Comparator.comparingDouble(HistoricoAmostra::getRamPercentual)).orElseThrow();
    }

    private void escreverCsv(BufferedWriter escritor, String... campos) throws IOException {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) escritor.write(';');
            escritor.write('"');
            escritor.write((campos[i] == null ? "" : campos[i]).replace("\"", "\"\""));
            escritor.write('"');
        }
        escritor.newLine();
    }

    private String textoNumero(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    private VBox indicador(String titulo, Label valor, String detalhe, String classe) {
        Label nome = label(titulo, "analises-indicador-titulo");
        valor.getStyleClass().add("analises-indicador-valor");
        Label textoDetalhe = label(detalhe, "analises-indicador-detalhe");
        VBox box = new VBox(8, nome, valor, textoDetalhe);
        box.getStyleClass().addAll("analises-indicador", classe);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox indicador(String titulo, Label valor, Label detalhe, String classe) {
        Label nome = label(titulo, "analises-indicador-titulo");
        valor.getStyleClass().add("analises-indicador-valor");
        detalhe.getStyleClass().add("analises-indicador-detalhe");
        VBox box = new VBox(8, nome, valor, detalhe);
        box.getStyleClass().addAll("analises-indicador", classe);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox card(Node... filhos) {
        VBox box = new VBox(14, filhos);
        box.getStyleClass().add("analises-card");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Button botaoPeriodo(String texto, boolean ativo) {
        Button botao = new Button(texto);
        botao.getStyleClass().add("analises-periodo");
        if (ativo) botao.getStyleClass().add("analises-periodo-ativo");
        botao.setOnAction(evento -> {
            for (Button item : botoesPeriodo) item.getStyleClass().remove("analises-periodo-ativo");
            botao.getStyleClass().add("analises-periodo-ativo");
            periodoSelecionado = texto;
            periodoMs = switch (texto) {
                case "2 horas" -> DUAS_HORAS;
                case "7 dias" -> SETE_DIAS;
                default -> VINTE_QUATRO_HORAS;
            };
            atualizarAnalise();
        });
        return botao;
    }


    private Node alertaVisual(String nivel, String titulo, String descricao) {
        Region ponto = new Region();
        ponto.setMinSize(8, 8);
        ponto.setPrefSize(8, 8);
        ponto.setMaxSize(8, 8);
        ponto.getStyleClass().add("analises-alerta-ponto-" + nivel);
        Label tituloLabel = label(titulo, "analises-alerta-titulo");
        Label descricaoLabel = label(descricao, "analises-alerta-descricao");
        descricaoLabel.setWrapText(true);
        VBox textos = new VBox(4, tituloLabel, descricaoLabel);
        HBox item = new HBox(10, ponto, textos);
        item.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(textos, Priority.ALWAYS);
        item.getStyleClass().add("analises-alerta-item");
        return item;
    }

    private String formatarDataHora(long instante) {
        return Instant.ofEpochMilli(instante).atZone(ZONA_LOCAL).format(FORMATO_DATA_HORA);
    }

    private static Label label(String texto, String classe) {
        Label label = new Label(texto);
        label.getStyleClass().add(classe);
        return label;
    }

    private static final class AlertaHistorico {
        final String nivel;
        final String titulo;
        final String descricao;
        AlertaHistorico(String nivel, String titulo, String descricao) {
            this.nivel = nivel;
            this.titulo = titulo;
            this.descricao = descricao;
        }
    }

    private static final class ProcessoResumo {
        final String nome;
        final int pid;
        final double cpuMedia;
        final double picoCpu;
        final double memoriaMaxMb;
        final int aparicoes;

        ProcessoResumo(String nome, int pid, double cpuMedia, double picoCpu,
                       double memoriaMaxMb, int aparicoes) {
            this.nome = nome;
            this.pid = pid;
            this.cpuMedia = cpuMedia;
            this.picoCpu = picoCpu;
            this.memoriaMaxMb = memoriaMaxMb;
            this.aparicoes = aparicoes;
        }
        double getCpuMedia() { return cpuMedia; }
        String getNomeExibicao() { return nome + " (PID " + pid + ")"; }
    }
}
