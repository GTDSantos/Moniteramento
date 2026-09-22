package com.monitoramento.hardwer;

import com.monitoramento.model.ProcesInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessosMonitor {
    private final OperatingSystem os;
    private final CentralProcessor processor;
    private final Map<Integer, OSProcess> prevProcess = new HashMap<>();

    public ProcessosMonitor(OperatingSystem os, CentralProcessor processor){
        this.os = os;
        this.processor = processor;

    }

    public List<ProcesInfo> getProcesInfo(){

        List<ProcesInfo> resultado = new ArrayList<>();
        List<OSProcess> listProcess = os.getProcesses();

        for (OSProcess process : listProcess){
            double cpu = 0;
            String nome = process.getName();
            int id = process.getProcessID();
            long memory = process.getResidentMemory() / (1024 * 1024);
            OSProcess anterior = prevProcess.get(process.getProcessID());
            if (anterior != null) {
                cpu = process.getProcessCpuLoadBetweenTicks(anterior) / processor.getLogicalProcessorCount() * 100;
            }

            prevProcess.put(process.getProcessID(),process);
            ProcesInfo info = new ProcesInfo(nome,id,memory,cpu);

            resultado.add(info);
        }
        return resultado;
    }
}
