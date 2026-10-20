package com.monitoramento.gui;

import com.monitoramento.hardwer.Monitoramento;
import com.monitoramento.historico.HistoricoMonitoramento;
import com.monitoramento.model.CpuInfo;
import com.monitoramento.model.NetInfo;
import com.monitoramento.model.ProcesInfo;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import oshi.SystemInfo;
import oshi.spi.SystemInfoProvider;

import java.util.List;
import java.net.InetAddress;
/** Inicializa e controla as telas do PC Monitor. */
public class MonitoramentoApp extends Application {

    private Monitoramento monitoramento;
    private HistoricoMonitoramento historicoMonitoramento;

    private DashboardView dashboardView;
    private ProcessosPage processosPage;
    private RedePage redePage;
    private AnalisesPage analisesPage;
    private Timeline timeline;
    private DiscosPage discosPage;
    private MenuLateral menuLateral;
    @Override
    public void start(Stage stage) {
        SystemInfoProvider si = new SystemInfo();

        monitoramento = new Monitoramento(si);
        historicoMonitoramento = new HistoricoMonitoramento(monitoramento);
        discosPage = new DiscosPage(monitoramento);
        CpuInfo cpu = monitoramento.getCpu();
        System.out.println(cpu.getNome());
        redePage = new RedePage();
        // Cria as telas.
        dashboardView = new DashboardView(
                monitoramento,
                historicoMonitoramento
        );
        processosPage = new ProcessosPage();
        analisesPage = new AnalisesPage(historicoMonitoramento);

        BorderPane root = new BorderPane();

        // O menu avisa quando o usuário troca de aba.
        menuLateral = new MenuLateral(
                aba -> selecionarAba(aba, root),
                monitoramento
        );

        root.setLeft(menuLateral);

        // A primeira tela é o Dashboard.
        Node paginaInicial = dashboardView.getView();

        root.setCenter(paginaInicial);
        BorderPane.setMargin(
                paginaInicial,
                new Insets(14, 14, 14, 0)
        );

        Scene scene = new Scene(root, 1200, 740);

        scene.getStylesheets().add(
                getClass()
                        .getResource("/com/monitoramento/style.css")
                        .toExternalForm()
        );

        stage.setTitle("PC Monitor");
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();

        /*
         * Faz uma única coleta de processos a cada segundo.
         * As duas telas recebem a mesma lista.
         */
        timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        evento -> {
                            List<ProcesInfo> processos =
                                    monitoramento.getProces();

                            List<NetInfo> redes =
                                    monitoramento.getNet();

                            dashboardView.atualizarDados(processos, redes);

                            processosPage.atualizarProcessos(processos);

                            redePage.atualizarRede(redes);
                            menuLateral.atualizarInformacoesMaquina();
                        }
                )
        );

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    // Decide qual tela será exibida.
    private void selecionarAba(String aba, BorderPane root) {
        Node pagina = switch (aba) {
            case "Dashboard" -> dashboardView.getView();

            case "Processos" -> processosPage;

            case "Discos" -> {
                discosPage.atualizarDiscos();
                yield discosPage;
            }

            case "Rede" -> redePage;

            case "Análises" -> {
                analisesPage.atualizarAnalise();
                yield analisesPage;
            }

            default -> dashboardView.getView();
        };

        root.setCenter(pagina);

        BorderPane.setMargin(
                pagina,
                new Insets(14, 14, 14, 0)
        );
    }

    // Tela temporária para as abas ainda não implementadas.
    private VBox paginaEmDesenvolvimento(String nome) {
        Label titulo = new Label(nome);
        titulo.getStyleClass().add("titulo");

        Label mensagem = new Label(
                "Esta tela será implementada futuramente."
        );

        mensagem.getStyleClass().add("subtitulo");

        VBox pagina = new VBox(12, titulo, mensagem);
        pagina.getStyleClass().addAll("folha", "conteudo");

        return pagina;
    }

    @Override
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }

        if (dashboardView != null) {
            dashboardView.fecharHistorico();
        }

        if (historicoMonitoramento != null) {
            historicoMonitoramento.fechar();
        }
    }
}