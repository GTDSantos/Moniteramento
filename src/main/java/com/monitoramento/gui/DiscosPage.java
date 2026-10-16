
package com.monitoramento.gui;

import com.monitoramento.hardwer.Monitoramento;
import com.monitoramento.model.DiskInfo;
import com.monitoramento.model.VolumeInfo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class DiscosPage extends VBox {

    private final Monitoramento monitoramento;

    // Resumo geral
    private final Label totalValor = new Label("Carregando...");
    private final Label usadoValor = new Label("Carregando...");
    private final Label livreValor = new Label("Carregando...");
    private final Label alertasValor = new Label("Carregando...");

    private final Label resumoAtividade = new Label("Leitura: -- | Escrita: --");

    // Listas visuais
    private final VBox listaVolumes = new VBox(12);
    private final VBox listaDiscos = new VBox(12);

    private final List<VolumeCartao> cartoesVolumes = new ArrayList<>();
    private final List<DiscoCartao> cartoesDiscos = new ArrayList<>();

    private List<String> chavesVolumes = new ArrayList<>();
    private List<String> chavesDiscos = new ArrayList<>();

    private boolean estruturaVolumesCriada;
    private boolean estruturaDiscosCriada;

    public DiscosPage(Monitoramento monitoramento) {

        this.monitoramento = monitoramento;

        setSpacing(18);
        setPadding(new Insets(26));

        getStyleClass().add("discos-pagina");

        // Cabeçalho
        Label titulo = new Label("Discos");
        titulo.getStyleClass().add("discos-titulo");

        Label subtitulo = new Label(
                "Monitore o armazenamento e a atividade dos seus discos."
        );
        subtitulo.getStyleClass().add("discos-subtitulo");

        VBox cabecalho = new VBox(6, titulo, subtitulo);

        // Cartões de resumo
        HBox resumo = new HBox(12);

        resumo.getChildren().addAll(
                criarResumo("Capacidade total", totalValor),
                criarResumo("Espaço utilizado", usadoValor),
                criarResumo("Espaço livre", livreValor),
                criarResumo("Volumes em atenção", alertasValor)
        );

        // Seção de volumes lógicos
        Label tituloVolumes = new Label("Volumes de armazenamento");
        tituloVolumes.getStyleClass().add("discos-secao-titulo");

        VBox secaoVolumes = new VBox(
                12,
                tituloVolumes,
                listaVolumes
        );

        // Seção de discos físicos
        Label tituloDiscos = new Label("Atividade dos discos físicos");
        tituloDiscos.getStyleClass().add("discos-secao-titulo");

        resumoAtividade.getStyleClass().add("discos-atividade-total");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        HBox cabecalhoDiscos = new HBox(
                12,
                tituloDiscos,
                espaco,
                resumoAtividade
        );
        cabecalhoDiscos.setAlignment(Pos.CENTER_LEFT);

        Label explicacao = new Label(
                "As taxas de leitura e escrita são calculadas em MB/s."
        );
        explicacao.getStyleClass().add("discos-subtitulo");

        VBox secaoDiscos = new VBox(
                12,
                cabecalhoDiscos,
                explicacao,
                listaDiscos
        );

        // Conteúdo que pode ser rolado
        VBox conteudo = new VBox(
                22,
                resumo,
                secaoVolumes,
                secaoDiscos
        );

        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("discos-scroll");

        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(cabecalho, scroll);

        // Primeira leitura
        atualizarDiscos();
    }

    // Cria um cartão do resumo
    private VBox criarResumo(String titulo, Label valor) {

        Label nome = new Label(titulo);
        nome.getStyleClass().add("discos-resumo-titulo");

        valor.getStyleClass().add("discos-resumo-valor");

        VBox card = new VBox(12, nome, valor);
        card.getStyleClass().add("discos-resumo-card");

        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }

    // Atualiza os dados sem reconstruir os cartões a cada segundo
    public void atualizarDiscos() {

        List<VolumeInfo> volumesRecebidos =
                monitoramento.getDiskVolume();

        List<VolumeInfo> volumes = volumesRecebidos == null
                ? List.of()
                : volumesRecebidos;

        List<DiskInfo> discosRecebidos =
                monitoramento.getDiskUsage();

        List<DiskInfo> discos = discosRecebidos == null
                ? List.of()
                : discosRecebidos;

        prepararCartoesVolumes(volumes);
        prepararCartoesDiscos(discos);

        long totalGeral = 0;
        long livreGeral = 0;
        int volumesEmAtencao = 0;

        // Atualiza os volumes e calcula o resumo
        for (int i = 0; i < volumes.size(); i++) {

            VolumeInfo volume = volumes.get(i);

            cartoesVolumes.get(i).atualizar(volume);

            long total = Math.max(0, volume.getTotal());

            long livre = Math.max(
                    0,
                    Math.min(total, volume.getLivre())
            );

            totalGeral += total;
            livreGeral += livre;

            double porcentagem = limitarPercentual(
                    volume.getPorctVolume()
            );

            if (porcentagem >= 85) {
                volumesEmAtencao++;
            }
        }

        long usadoGeral = Math.max(0, totalGeral - livreGeral);

        totalValor.setText(formatarGB(totalGeral));
        usadoValor.setText(formatarGB(usadoGeral));
        livreValor.setText(formatarGB(livreGeral));

        alertasValor.setText(
                volumesEmAtencao
                        + (volumesEmAtencao == 1
                        ? " volume"
                        : " volumes")
        );

        // Atualiza os discos físicos
        double leituraTotal = 0;
        double escritaTotal = 0;

        for (int i = 0; i < discos.size(); i++) {

            DiskInfo disco = discos.get(i);

            cartoesDiscos.get(i).atualizar(disco);

            leituraTotal += Math.max(0, disco.getRead());
            escritaTotal += Math.max(0, disco.getWrite());
        }

        resumoAtividade.setText(
                "Leitura: " + formatarTaxa(leituraTotal)
                        + "  |  Escrita: " + formatarTaxa(escritaTotal)
        );
    }

    // Monta os cartões dos volumes somente quando a lista muda
    private void prepararCartoesVolumes(List<VolumeInfo> volumes) {

        List<String> novasChaves = new ArrayList<>();

        for (int i = 0; i < volumes.size(); i++) {
            novasChaves.add(nomeVolume(volumes.get(i), i));
        }

        if (estruturaVolumesCriada
                && chavesVolumes.equals(novasChaves)) {
            return;
        }

        estruturaVolumesCriada = true;
        chavesVolumes = novasChaves;

        listaVolumes.getChildren().clear();
        cartoesVolumes.clear();

        if (volumes.isEmpty()) {
            listaVolumes.getChildren().add(
                    mensagemVazia("Nenhum volume encontrado.")
            );
            return;
        }

        for (int i = 0; i < volumes.size(); i++) {

            VolumeCartao cartao =
                    new VolumeCartao(novasChaves.get(i));

            cartoesVolumes.add(cartao);
            listaVolumes.getChildren().add(cartao.getView());
        }
    }

    // Monta os cartões dos discos físicos somente quando a lista muda
    private void prepararCartoesDiscos(List<DiskInfo> discos) {

        List<String> novasChaves = new ArrayList<>();

        for (int i = 0; i < discos.size(); i++) {
            novasChaves.add(nomeDisco(discos.get(i), i));
        }

        if (estruturaDiscosCriada
                && chavesDiscos.equals(novasChaves)) {
            return;
        }

        estruturaDiscosCriada = true;
        chavesDiscos = novasChaves;

        listaDiscos.getChildren().clear();
        cartoesDiscos.clear();

        if (discos.isEmpty()) {
            listaDiscos.getChildren().add(
                    mensagemVazia("Nenhum disco físico encontrado.")
            );
            return;
        }

        for (int i = 0; i < discos.size(); i++) {

            DiscoCartao cartao =
                    new DiscoCartao(novasChaves.get(i), i + 1);

            cartoesDiscos.add(cartao);
            listaDiscos.getChildren().add(cartao.getView());
        }
    }

    private Label mensagemVazia(String texto) {

        Label mensagem = new Label(texto);
        mensagem.getStyleClass().add("discos-vazio");

        return mensagem;
    }

    private String nomeVolume(VolumeInfo volume, int indice) {

        String nome = volume.getNome();

        if (nome == null || nome.isBlank()) {
            return "Volume " + (indice + 1);
        }

        return nome;
    }

    private String nomeDisco(DiskInfo disco, int indice) {

        String modelo = disco.getModelo();

        if (modelo == null || modelo.isBlank()) {
            return "Disco físico " + (indice + 1);
        }

        return modelo;
    }

    private double limitarPercentual(double valor) {

        if (!Double.isFinite(valor)) {
            return 0;
        }

        return Math.max(0, Math.min(100, valor));
    }

    private String formatarGB(long valor) {
        return String.format("%,d GB", Math.max(0, valor));
    }

    private String formatarTaxa(double valor) {

        if (!Double.isFinite(valor) || valor < 0) {
            valor = 0;
        }

        return String.format("%.2f MB/s", valor);
    }

    // Cartão individual de um volume lógico
    private class VolumeCartao {

        private final String chave;

        private final Label nome = new Label();
        private final Label percentual = new Label();
        private final Label estado = new Label();

        private final Label usado = new Label();
        private final Label livre = new Label();
        private final Label capacidade = new Label();

        private final ProgressBar barra = new ProgressBar(0);

        private final VBox view;

        private VolumeCartao(String chave) {

            this.chave = chave;
            nome.setText(chave);

            nome.getStyleClass().add("discos-card-nome");
            percentual.getStyleClass().add("discos-card-percentual");

            Region espaco = new Region();
            HBox.setHgrow(espaco, Priority.ALWAYS);

            HBox topo = new HBox(
                    12,
                    nome,
                    espaco,
                    percentual,
                    estado
            );
            topo.setAlignment(Pos.CENTER_LEFT);

            barra.setMaxWidth(Double.MAX_VALUE);
            barra.getStyleClass().add("discos-barra");

            usado.getStyleClass().add("discos-info");
            livre.getStyleClass().add("discos-info");
            capacidade.getStyleClass().add("discos-info");

            Region espacoInfo = new Region();
            HBox.setHgrow(espacoInfo, Priority.ALWAYS);

            HBox informacoes = new HBox(
                    12,
                    usado,
                    livre,
                    espacoInfo,
                    capacidade
            );
            informacoes.setAlignment(Pos.CENTER_LEFT);

            view = new VBox(15, topo, barra, informacoes);
            view.getStyleClass().add("discos-card");
        }

        private void atualizar(VolumeInfo volume) {

            double total = Math.max(0, volume.getTotal());

            double livreValor = Math.max(
                    0,
                    Math.min(total, volume.getLivre())
            );

            double usadoValor = Math.max(0, total - livreValor);

            double porcentagem = limitarPercentual(
                    volume.getPorctVolume()
            );

            nome.setText(chave);

            percentual.setText(
                    String.format("%.1f%% utilizado", porcentagem)
            );

            usado.setText("Usado: " + formatarGB((long) usadoValor));
            livre.setText("Livre: " + formatarGB((long) livreValor));

            capacidade.setText(
                    "Capacidade: " + formatarGB((long) total)
            );

            barra.setProgress(porcentagem / 100.0);

            barra.getStyleClass().remove("discos-barra-alerta");

            if (porcentagem >= 85) {
                barra.getStyleClass().add("discos-barra-alerta");
            }

            estado.getStyleClass().removeAll(
                    "discos-status-normal",
                    "discos-status-atencao",
                    "discos-status-critico"
            );

            if (porcentagem >= 95) {
                estado.setText("Crítico");
                estado.getStyleClass().add("discos-status-critico");

            } else if (porcentagem >= 85) {
                estado.setText("Atenção");
                estado.getStyleClass().add("discos-status-atencao");

            } else {
                estado.setText("Normal");
                estado.getStyleClass().add("discos-status-normal");
            }
        }

        private VBox getView() {
            return view;
        }
    }

    // Cartão individual de um disco físico
    private class DiscoCartao {

        private final String modelo;
        private final Label identificacao = new Label();
        private final Label leitura = new Label();
        private final Label escrita = new Label();
        private final Label atividade = new Label();

        private final VBox view;

        private DiscoCartao(String modelo, int indice) {

            this.modelo = modelo;

            Label nome = new Label(modelo);
            nome.getStyleClass().add("discos-card-nome");
            nome.setWrapText(true);

            identificacao.setText("Disco físico " + indice);
            identificacao.getStyleClass().add("discos-card-percentual");

            HBox topo = new HBox(12, nome, identificacao);
            topo.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(nome, Priority.ALWAYS);

            Label tituloLeitura = new Label("Leitura");
            tituloLeitura.getStyleClass().add("discos-metrica-titulo");
            leitura.getStyleClass().add("discos-metrica-valor");

            Label tituloEscrita = new Label("Escrita");
            tituloEscrita.getStyleClass().add("discos-metrica-titulo");
            escrita.getStyleClass().add("discos-metrica-valor");

            VBox leituraCard = new VBox(8, tituloLeitura, leitura);
            leituraCard.getStyleClass().add("discos-metrica");

            VBox escritaCard = new VBox(8, tituloEscrita, escrita);
            escritaCard.getStyleClass().add("discos-metrica");

            HBox metricas = new HBox(12, leituraCard, escritaCard);
            HBox.setHgrow(leituraCard, Priority.ALWAYS);
            HBox.setHgrow(escritaCard, Priority.ALWAYS);

            atividade.getStyleClass().add("discos-atividade");

            view = new VBox(14, topo, metricas, atividade);
            view.getStyleClass().add("discos-card");
        }

        private void atualizar(DiskInfo disco) {

            double taxaLeitura = Math.max(0, disco.getRead());
            double taxaEscrita = Math.max(0, disco.getWrite());

            leitura.setText(formatarTaxa(taxaLeitura));
            escrita.setText(formatarTaxa(taxaEscrita));

            if (taxaLeitura + taxaEscrita < 0.01) {
                atividade.setText(
                        "Sem atividade de leitura/escrita detectável"
                );
            } else {
                atividade.setText(
                        "Atividade de leitura/escrita detectada"
                );
            }
        }

        private VBox getView() {
            return view;
        }
    }
}
