
package com.monitoramento.gui;

import com.monitoramento.model.NetInfo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class RedePage extends VBox {

    private static final int MAX_PONTOS = 60;

    // Indicadores principais
    private final Label downloadValor = new Label("-- MB/s");
    private final Label uploadValor = new Label("-- MB/s");
    private final Label totalValor = new Label("-- MB/s");
    private final Label estadoValor = new Label("Aguardando leitura");

    // Informações da interface
    private final Label nomeExibicaoValor = new Label("--");
    private final Label nomeTecnicoValor = new Label("--");
    private final Label macValor = new Label("--");
    private final Label ipValor = new Label("--");
    private final Label atualizacaoValor = new Label("--");

    // Histórico do tráfego
    private final List<Double> historicoDownload = new ArrayList<>();
    private final List<Double> historicoUpload = new ArrayList<>();

    private final Canvas grafico = new Canvas(800, 240);
    private final StackPane areaGrafico = new StackPane(grafico);

    public RedePage() {

        setSpacing(18);
        setPadding(new Insets(26));
        getStyleClass().add("rede-pagina");

        // Cabeçalho
        Label titulo = new Label("Rede");
        titulo.getStyleClass().add("rede-titulo");

        Label subtitulo = new Label(
                "Acompanhe o tráfego e os detalhes da interface de rede."
        );
        subtitulo.getStyleClass().add("rede-subtitulo");

        VBox cabecalho = new VBox(6, titulo, subtitulo);

        // Indicadores principais
        HBox resumo = new HBox(12,
                criarResumo(
                        "Recebido / download",
                        downloadValor,
                        "rede-verde"
                ),
                criarResumo(
                        "Enviado / upload",
                        uploadValor,
                        "rede-azul"
                ),
                criarResumo(
                        "Tráfego total",
                        totalValor,
                        "rede-claro"
                ),
                criarResumo(
                        "Estado da interface",
                        estadoValor,
                        "rede-estado"
                )
        );

        // Gráfico
        Label tituloGrafico = new Label("Histórico de tráfego");
        tituloGrafico.getStyleClass().add("rede-secao-titulo");

        Label detalheGrafico = new Label(
                "Últimos 60 segundos, aproximadamente"
        );
        detalheGrafico.getStyleClass().add("rede-subtitulo");

        HBox legenda = new HBox(
                16,
                criarLegenda("Recebido", "rede-legenda-download"),
                criarLegenda("Enviado", "rede-legenda-upload")
        );
        legenda.setAlignment(Pos.CENTER_LEFT);

        areaGrafico.setMinHeight(240);
        areaGrafico.setPrefHeight(240);
        areaGrafico.setMaxWidth(Double.MAX_VALUE);
        areaGrafico.getStyleClass().add("rede-grafico-area");



        grafico.widthProperty().addListener(
                (obs, antes, depois) -> desenharGrafico()
        );

        grafico.heightProperty().addListener(
                (obs, antes, depois) -> desenharGrafico()
        );

        VBox secaoGrafico = new VBox(
                10,
                new VBox(4, tituloGrafico, detalheGrafico),
                legenda,
                areaGrafico
        );
        secaoGrafico.getStyleClass().add("rede-secao");

        // Detalhes da interface selecionada pelo NetMonitor
        Label tituloInterface = new Label(
                "Detalhes da interface monitorada"
        );
        tituloInterface.getStyleClass().add("rede-secao-titulo");

        Label explicacaoInterface = new Label(
                "O monitor seleciona uma interface válida com tráfego "
                        + "ou utiliza a rota principal como alternativa."
        );
        explicacaoInterface.getStyleClass().add("rede-subtitulo");
        explicacaoInterface.setWrapText(true);

        VBox cartaoInterface = new VBox(14);
        cartaoInterface.getStyleClass().add("rede-cartao-interface");

        cartaoInterface.getChildren().addAll(
                criarLinhaDetalhe(
                        "Nome de exibição",
                        nomeExibicaoValor,
                        null
                ),
                criarLinhaDetalhe(
                        "Nome técnico",
                        nomeTecnicoValor,
                        null
                ),
                criarLinhaDetalhe(
                        "Endereço MAC",
                        macValor,
                        botaoCopiar(macValor)
                ),
                criarLinhaDetalhe(
                        "Endereço(s) IPv4",
                        ipValor,
                        botaoCopiar(ipValor)
                ),
                criarLinhaDetalhe(
                        "Estado informado pelo sistema",
                        atualizacaoValor,
                        null
                )
        );

        VBox secaoInterface = new VBox(
                10,
                tituloInterface,
                explicacaoInterface,
                cartaoInterface
        );
        secaoInterface.getStyleClass().add("rede-secao");

        // Área rolável
        VBox conteudo = new VBox(
                18,
                resumo,
                secaoGrafico,
                secaoInterface
        );

        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );
        scroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );
        scroll.getStyleClass().add("rede-scroll");

        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(cabecalho, scroll);

        desenharGrafico();
    }

    // Cria um indicador do resumo.
    private VBox criarResumo(
            String titulo,
            Label valor,
            String classeValor
    ) {

        Label rotulo = new Label(titulo);
        rotulo.getStyleClass().add("rede-resumo-titulo");

        valor.getStyleClass().addAll(
                "rede-resumo-valor",
                classeValor
        );

        VBox cartao = new VBox(12, rotulo, valor);
        cartao.getStyleClass().add("rede-resumo-cartao");
        cartao.setMinWidth(130);
        cartao.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(cartao, Priority.ALWAYS);

        return cartao;
    }

    private HBox criarLegenda(String texto, String classe) {

        Region marca = new Region();
        marca.setPrefSize(9, 9);
        marca.setMinSize(9, 9);
        marca.setMaxSize(9, 9);
        marca.getStyleClass().add(classe);

        Label label = new Label(texto);
        label.getStyleClass().add("rede-legenda-texto");

        HBox linha = new HBox(7, marca, label);
        linha.setAlignment(Pos.CENTER_LEFT);

        return linha;
    }

    private HBox criarLinhaDetalhe(
            String titulo,
            Label valor,
            Button acao
    ) {

        Label nome = new Label(titulo);
        nome.getStyleClass().add("rede-detalhe-titulo");
        nome.setMinWidth(190);

        valor.getStyleClass().add("rede-detalhe-valor");
        valor.setWrapText(true);

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        HBox linha;

        if (acao == null) {
            linha = new HBox(12, nome, valor);
        } else {
            linha = new HBox(12, nome, valor, espaco, acao);
        }

        linha.setAlignment(Pos.CENTER_LEFT);
        linha.getStyleClass().add("rede-detalhe-linha");

        return linha;
    }

    // Permite copiar IP ou MAC.
    private Button botaoCopiar(Label origem) {

        Button botao = new Button("Copiar");
        botao.getStyleClass().add("rede-botao-copiar");

        botao.setOnAction(event ->
                copiarTexto(origem.getText())
        );

        return botao;
    }

    private void copiarTexto(String texto) {

        if (texto == null
                || texto.isBlank()
                || texto.equals("--")) {
            return;
        }

        ClipboardContent conteudo = new ClipboardContent();
        conteudo.putString(texto);

        Clipboard.getSystemClipboard().setContent(conteudo);
    }

    /*
     * Recebe a mesma leitura de rede utilizada pelo Dashboard.
     * Isso evita consultar o monitor de rede duas vezes no mesmo ciclo.
     */
    public void atualizarRede(List<NetInfo> redes) {

        if (redes == null
                || redes.isEmpty()
                || redes.get(0) == null) {

            downloadValor.setText("0,00 MB/s");
            uploadValor.setText("0,00 MB/s");
            totalValor.setText("0,00 MB/s");

            estadoValor.setText("Indisponível");
            estadoValor.getStyleClass().removeAll(
                    "rede-estado-ativo",
                    "rede-estado-inativo"
            );
            estadoValor.getStyleClass().add("rede-estado-inativo");

            nomeExibicaoValor.setText(
                    "Nenhuma interface selecionada"
            );
            nomeTecnicoValor.setText("--");
            macValor.setText("--");
            ipValor.setText("--");

            atualizacaoValor.setText(
                    "O monitor não encontrou uma interface válida."
            );

            adicionarPontoHistorico(0, 0);
            return;
        }

        // O NetMonitor atual retorna uma interface selecionada.
        NetInfo rede = redes.get(0);

        double download = valorValido(rede.getRecv());
        double upload = valorValido(rede.getSent());

        downloadValor.setText(formatarTaxa(download));
        uploadValor.setText(formatarTaxa(upload));
        totalValor.setText(formatarTaxa(download + upload));

        String status = rede.getStatus() == null
                ? "desconhecido"
                : rede.getStatus();

        boolean ativa = status.equalsIgnoreCase("ativo")
                || status.equalsIgnoreCase("up")
                || status.equalsIgnoreCase("ativa");

        estadoValor.setText(ativa ? "Ativa" : "Inativa");

        estadoValor.getStyleClass().removeAll(
                "rede-estado-ativo",
                "rede-estado-inativo"
        );

        estadoValor.getStyleClass().add(
                ativa ? "rede-estado-ativo" : "rede-estado-inativo"
        );

        nomeExibicaoValor.setText(
                textoOuPadrao(
                        rede.getDisplay(),
                        "Nome não informado"
                )
        );

        nomeTecnicoValor.setText(
                textoOuPadrao(
                        rede.getNome(),
                        "Nome não informado"
                )
        );

        macValor.setText(
                textoOuPadrao(
                        rede.getMac(),
                        "MAC não informado"
                )
        );

        List<String> ips = rede.getIp();

        if (ips == null || ips.isEmpty()) {
            ipValor.setText("Nenhum endereço IPv4 informado");
        } else {
            ipValor.setText(String.join("   •   ", ips));
        }

        atualizacaoValor.setText(
                "Estado reportado pelo sistema: " + status
        );

        adicionarPontoHistorico(download, upload);
    }

    // Guarda somente os últimos 60 pontos.
    private void adicionarPontoHistorico(
            double download,
            double upload
    ) {

        historicoDownload.add(download);
        historicoUpload.add(upload);

        while (historicoDownload.size() > MAX_PONTOS) {
            historicoDownload.remove(0);
            historicoUpload.remove(0);
        }

        desenharGrafico();
    }

    private double valorValido(double valor) {

        if (!Double.isFinite(valor) || valor < 0) {
            return 0;
        }

        return valor;
    }

    private String formatarTaxa(double valor) {
        return String.format(
                "%.2f MB/s",
                valorValido(valor)
        );
    }

    private String textoOuPadrao(
            String valor,
            String padrao
    ) {

        return valor == null || valor.isBlank()
                ? padrao
                : valor;
    }

    // Desenha o histórico de recebimento e envio.
    private void desenharGrafico() {

        double largura = grafico.getWidth();
        double altura = grafico.getHeight();

        if (!Double.isFinite(largura)
                || !Double.isFinite(altura)
                || largura <= 0
                || altura <= 0
                || largura > 4096
                || altura > 2048) {
            return;
        }

        GraphicsContext gc = grafico.getGraphicsContext2D();

        gc.setFill(Color.web("#20252c"));
        gc.fillRect(0, 0, largura, altura);

        double esquerda = 58;
        double direita = 18;
        double topo = 28;
        double baixo = 30;

        double graficoLargura = Math.max(
                1,
                largura - esquerda - direita
        );

        double graficoAltura = Math.max(
                1,
                altura - topo - baixo
        );

        double maior = 0;

        for (double valor : historicoDownload) {
            maior = Math.max(maior, valor);
        }

        for (double valor : historicoUpload) {
            maior = Math.max(maior, valor);
        }

        double escala = maior <= 0 ? 1.0 : maior * 1.20;

        gc.setStroke(Color.web("#343b45"));
        gc.setLineWidth(1);
        gc.setFill(Color.web("#8e98a7"));

        // Linhas horizontais e valores do eixo vertical.
        for (int i = 0; i <= 3; i++) {

            double y = topo + graficoAltura * i / 3.0;

            gc.strokeLine(
                    esquerda,
                    y,
                    esquerda + graficoLargura,
                    y
            );

            double valorEixo = escala * (3 - i) / 3.0;

            gc.fillText(
                    String.format("%.2f", valorEixo),
                    5,
                    y + 4
            );
        }

        gc.fillText("Início do histórico", esquerda, altura - 8);

        gc.fillText(
                "Agora",
                esquerda + graficoLargura - 35,
                altura - 8
        );

        desenharLinha(
                gc,
                historicoDownload,
                Color.web("#4FBE8C"),
                esquerda,
                topo,
                graficoLargura,
                graficoAltura,
                escala
        );

        desenharLinha(
                gc,
                historicoUpload,
                Color.web("#5B7CFA"),
                esquerda,
                topo,
                graficoLargura,
                graficoAltura,
                escala
        );

        if (historicoDownload.isEmpty()) {
            gc.setFill(Color.web("#8e98a7"));
            gc.fillText(
                    "Aguardando medições de rede...",
                    esquerda + 10,
                    topo + 24
            );
        }
    }

    private void desenharLinha(
            GraphicsContext gc,
            List<Double> valores,
            Color cor,
            double esquerda,
            double topo,
            double largura,
            double altura,
            double escala
    ) {

        if (valores.isEmpty()) {
            return;
        }

        gc.setStroke(cor);
        gc.setLineWidth(2.0);

        if (valores.size() == 1) {

            double x = esquerda + largura;
            double y = topo + altura
                    - (valores.get(0) / escala * altura);

            gc.setFill(cor);
            gc.fillOval(x - 3, y - 3, 6, 6);

            return;
        }

        double divisor = valores.size() - 1.0;

        for (int i = 1; i < valores.size(); i++) {

            double x1 = esquerda + largura * (i - 1) / divisor;
            double x2 = esquerda + largura * i / divisor;

            double y1 = topo + altura
                    - (valores.get(i - 1) / escala * altura);

            double y2 = topo + altura
                    - (valores.get(i) / escala * altura);

            gc.strokeLine(x1, y1, x2, y2);
        }
    }
}
