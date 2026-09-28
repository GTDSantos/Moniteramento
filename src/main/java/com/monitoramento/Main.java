package com.monitoramento;
import com.monitoramento.gui.MonitoramentoApp;
import com.monitoramento.hardwer.Monitoramento;
import javafx.stage.Stage;
import oshi.hardware.*;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class    Main {



    static void main() {
        SystemInfoProvider si = SystemInfoFactory.create();
        Monitoramento monitoramento = new Monitoramento(si);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        String nome = monitoramento.getCpu().getNome();
        long totaMemory = monitoramento.getRam().getTotaMemory();
        Stage stage = new Stage();
        MonitoramentoApp tela = new MonitoramentoApp();
        tela.start(stage);
    }

}
