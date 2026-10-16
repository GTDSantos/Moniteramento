package com.monitoramento.gui;

import com.monitoramento.hardwer.Monitoramento;
import com.monitoramento.model.ProcesInfo;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;

/** Exibe os cinco processos com maior uso de CPU no Dashboard. */
public class ProcessosView extends VBox {
    private final Monitoramento monitoramento;

    public ProcessosView(Monitoramento monitoramento) {
        this.monitoramento = monitoramento;
        atualizarProcessos();
    }

    // Usado na criação inicial da pequena lista do Dashboard.
    public void atualizarProcessos() {
        if (monitoramento == null) {
            return;
        }

        atualizarProcessos(monitoramento.getProces());
    }

    // Recebe uma coleta que já foi feita pelo programa principal.
    public void atualizarProcessos(List<ProcesInfo> recebidos) {
        if (recebidos == null) {
            return;
        }

        List<ProcesInfo> processos = new ArrayList<>(recebidos);

        // Não trata o tempo ocioso como um programa que pesa na CPU.
        processos.removeIf(processo -> {
            String nome = processo.getNome();

            return nome == null
                    || nome.equalsIgnoreCase("Idle")
                    || nome.equalsIgnoreCase("System Idle Process");
        });

        // Ordena do maior uso de CPU para o menor.
        // Usa memória como critério secundário.
        processos.sort((p1, p2) -> {
            int comparacaoCpu = Double.compare(
                    p2.getCpu(),
                    p1.getCpu()
            );

            if (comparacaoCpu != 0) {
                return comparacaoCpu;
            }

            return Long.compare(
                    p2.getMemory(),
                    p1.getMemory()
            );
        });

        getChildren().clear();

        getChildren().add(
                linhaProcesso("Processo", "Memória", "CPU", -1)
        );

        int limite = Math.min(5, processos.size());

        for (int i = 0; i < limite; i++) {
            ProcesInfo processo = processos.get(i);

            String nome = processo.getNome();
            String memoria = formatarMemoria(processo.getMemory());

            String cpu = String.format(
                    "%.1f%%",
                    Math.max(0, processo.getCpu())
            );

            getChildren().add(
                    linhaProcesso(nome, memoria, cpu, i)
            );
        }

        if (processos.isEmpty()) {
            getChildren().add(
                    label("Nenhum processo encontrado", "nota")
            );
        }
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

    private HBox linhaProcesso(String nome, String mem, String cpu, int i) {
        boolean cab = i < 0;
        Label icone = cab ? new Label() : label(nome.substring(0, 1).toUpperCase(), "proc-icone");
        if (!cab) icone.getStyleClass().add("ic-" + (i % 4));
        icone.setMinSize(30, 30);
        icone.setPrefSize(30, 30);
        icone.setAlignment(Pos.CENTER);

        Label n = label(nome, cab ? "proc-cab" : "proc-nome");
        Label m = label(mem, cab ? "proc-cab" : "proc-valor");
        Label c = label(cpu, cab ? "proc-cab" : "proc-valor");
        n.setMinWidth(0);
        n.setMaxWidth(130);
        m.setMinWidth(72);
        c.setMinWidth(44);
        m.setAlignment(Pos.CENTER_RIGHT);
        c.setAlignment(Pos.CENTER_RIGHT);
        Region g = new Region();
        HBox.setHgrow(g, Priority.ALWAYS);
        HBox r = new HBox(12, icone, n, g, m, c);
        r.setAlignment(Pos.CENTER_LEFT);
        r.getStyleClass().add("proc");
        return r;
    }

    private static Label label(String texto, String classe) {
        Label l = new Label(texto);
        l.getStyleClass().add(classe);
        return l;
    }
}
