package com.monitoramento;

import com.monitoramento.gui.MonitoramentoApp;
import javafx.application.Application;

/** Ponto de entrada alternativo do PC Monitor. */
public final class Main {

    private Main() {
        // Esta classe não precisa ser instanciada.
    }

    public static void main(String[] args) {
        Application.launch(MonitoramentoApp.class, args);
    }
}
