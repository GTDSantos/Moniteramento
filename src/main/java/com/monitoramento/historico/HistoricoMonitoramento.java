package com.monitoramento.historico;

import com.monitoramento.hardwer.Monitoramento;
import com.monitoramento.model.CpuInfo;
import com.monitoramento.model.DiskInfo;
import com.monitoramento.model.NetInfo;
import com.monitoramento.model.ProcesInfo;
import com.monitoramento.model.RamInfo;
import com.monitoramento.model.VolumeInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Grava, de forma opcional, amostras do monitoramento em arquivos CSV diários.
 * Uma linha representa uma leitura completa do sistema.
 */
public class HistoricoMonitoramento {

    private static final long INTERVALO_AMOSTRA_MS = 5_000L;
    private static final int DIAS_RETENCAO = 7;
    private static final int AMOSTRAS_PARA_FLUSH = 3;
    private static final String PREFIXO_ARQUIVO = "historico_monitoramento_";
    private static final DateTimeFormatter FORMATO_DIA =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String CHAVE_HABILITADO = "historico_monitoramento_habilitado";
    private static final Pattern PADRAO_OBJETO_JSON = Pattern.compile("\\{([^{}]*)\\}");
    private static final Map<String, Pattern> PADROES_NUMERICOS_JSON = Map.of(
            "download_mb_s", Pattern.compile("\"download_mb_s\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)"),
            "upload_mb_s", Pattern.compile("\"upload_mb_s\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)"),
            "usado_gb", Pattern.compile("\"usado_gb\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)"),
            "total_gb", Pattern.compile("\"total_gb\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)"),
            "uso_percentual", Pattern.compile("\"uso_percentual\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)"),
            "pid", Pattern.compile("\"pid\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)"),
            "cpu_percent", Pattern.compile("\"cpu_percent\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)"),
            "memoria_mb", Pattern.compile("\"memoria_mb\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
    );
    private static final Pattern PADRAO_NOME_JSON = Pattern.compile("\"nome\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private static final String CABECALHO =
            "timestamp_epoch_ms,timestamp_utc,cpu_percent,ram_used_gb,ram_total_gb,ram_percent,"
                    + "network_json,volumes_json,physical_disks_json,top_processes_json";

    private final Monitoramento monitoramento;
    private final Preferences preferencias =
            Preferences.userNodeForPackage(HistoricoMonitoramento.class);

    private final Path diretorio = Path.of(
            System.getProperty("user.home"), ".pcmonitor", "historico"
    );

    private BufferedWriter escritor;
    private Path arquivoAtual;
    private LocalDate diaArquivoAtual;
    private boolean habilitado;
    private long ultimaAmostraMs;
    private int amostrasDesdeFlush;
    private List<ResumoProcesso> resumoProcessosUltimaLeitura = List.of();

    public HistoricoMonitoramento(Monitoramento monitoramento) {
        this.monitoramento = monitoramento;

        // A escolha permanece salva para as próximas execuções.
        boolean habilitarAoIniciar = preferencias.getBoolean(
                CHAVE_HABILITADO, false
        );

        if (habilitarAoIniciar) {
            habilitado = abrirArquivo();

            if (!habilitado) {
                salvarPreferencia(false);
            }
        }
    }

    public synchronized boolean isHabilitado() {
        return habilitado;
    }

    /** Liga/desliga a persistência. Retorna false se não for possível ativá-la. */
    public synchronized boolean setHabilitado(boolean novoEstado) {
        if (novoEstado == habilitado) {
            return true;
        }

        if (!novoEstado) {
            habilitado = false;
            fecharEscritor();
            salvarPreferencia(false);
            return true;
        }

        if (!abrirArquivo()) {
            habilitado = false;
            salvarPreferencia(false);
            return false;
        }

        habilitado = true;
        ultimaAmostraMs = 0;
        salvarPreferencia(true);
        return true;
    }

    public Path getDiretorioHistorico() {
        return diretorio;
    }

    /** Lê as amostras salvas dentro do período pedido, sem iniciar uma coleta nova. */
    public synchronized List<HistoricoAmostra> carregarAmostrasDesde(long limiteMs) {
        List<HistoricoAmostra> resultado = new ArrayList<>();
        Map<String, ResumoProcesso> processos = new HashMap<>();
        resumoProcessosUltimaLeitura = List.of();

        // Garante que a tela enxergue também as últimas linhas ainda no buffer.
        if (escritor != null) {
            try {
                escritor.flush();
            } catch (IOException e) {
                System.err.println("Não foi possível atualizar o arquivo antes da leitura: " + e.getMessage());
            }
        }

        if (!Files.isDirectory(diretorio)) {
            return resultado;
        }

        try (Stream<Path> caminhos = Files.list(diretorio)) {
            List<Path> arquivos = caminhos
                    .filter(Files::isRegularFile)
                    .filter(caminho -> {
                        String nome = caminho.getFileName().toString();
                        return nome.startsWith(PREFIXO_ARQUIVO) && nome.endsWith(".csv");
                    })
                    .sorted()
                    .toList();

            for (Path arquivo : arquivos) {
                lerArquivoHistorico(arquivo, limiteMs, resultado, processos);
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler os arquivos de histórico: " + e.getMessage());
        }

        resultado.sort(Comparator.comparingLong(HistoricoAmostra::getInstanteMs));
        List<ResumoProcesso> ordenados = new ArrayList<>(processos.values());
        ordenados.sort(Comparator.comparingDouble(ResumoProcesso::getCpuMedia).reversed());
        if (ordenados.size() > 10) ordenados = new ArrayList<>(ordenados.subList(0, 10));
        resumoProcessosUltimaLeitura = List.copyOf(ordenados);
        return resultado;
    }

    public synchronized List<ResumoProcesso> getResumoProcessosUltimaLeitura() {
        return resumoProcessosUltimaLeitura;
    }

    private void lerArquivoHistorico(Path arquivo, long limiteMs,
                                     List<HistoricoAmostra> destino,
                                     Map<String, ResumoProcesso> processosResumo) {
        try (BufferedReader leitor = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            leitor.readLine(); // Cabeçalho CSV.
            String linha;
            while ((linha = leitor.readLine()) != null) {
                try {
                    List<String> campos = separarLinhaCsv(linha);
                    if (campos.size() < 10) continue;

                    long instante = Long.parseLong(campos.get(0));
                    if (instante < limiteMs || instante > System.currentTimeMillis()) continue;

                    double download = 0;
                    double upload = 0;
                    for (String objeto : objetosJson(campos.get(6))) {
                        download += Math.max(0, numeroJson(objeto, "download_mb_s"));
                        upload += Math.max(0, numeroJson(objeto, "upload_mb_s"));
                    }
                    // A amostra é recriada para incluir os valores de rede já interpretados.
                    HistoricoAmostra amostra = new HistoricoAmostra(
                            instante, numeroCampo(campos.get(2)),
                            numeroCampo(campos.get(3)), numeroCampo(campos.get(4)),
                            numeroCampo(campos.get(5)), download, upload
                    );

                    for (String objeto : objetosJson(campos.get(7))) {
                        amostra.adicionarVolume(new HistoricoAmostra.VolumeHistorico(
                                textoJson(objeto, "nome"),
                                numeroJson(objeto, "usado_gb"),
                                numeroJson(objeto, "total_gb"),
                                numeroJson(objeto, "uso_percentual")
                        ));
                    }

                    // Resume os processos durante a leitura, sem conservar objetos por amostra.
                    for (String objeto : objetosJson(campos.get(9))) {
                        String nome = textoJson(objeto, "nome");
                        int pid = (int) numeroJson(objeto, "pid");

                        // Ignora o processo ocioso também ao ler históricos antigos.
                        if (isProcessoOcioso(nome, pid)) {
                            continue;
                        }

                        String chave = pid + "|" + nome;
                        ResumoProcesso resumo = processosResumo.computeIfAbsent(
                                chave, ignorado -> new ResumoProcesso(nome, pid));
                        resumo.adicionar(numeroJson(objeto, "cpu_percent"),
                                numeroJson(objeto, "memoria_mb"));
                    }

                    destino.add(amostra);
                } catch (RuntimeException ignorado) {
                    // Uma linha incompleta/corrompida não impede a leitura das demais.
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao abrir o histórico " + arquivo.getFileName() + ": " + e.getMessage());
        }
    }

    private List<String> separarLinhaCsv(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreAspas = false;

        for (int i = 0; i < linha.length(); i++) {
            char caractere = linha.charAt(i);
            if (caractere == '"') {
                if (entreAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    campo.append('"');
                    i++;
                } else {
                    entreAspas = !entreAspas;
                }
            } else if (caractere == ',' && !entreAspas) {
                campos.add(campo.toString());
                campo.setLength(0);
            } else {
                campo.append(caractere);
            }
        }
        campos.add(campo.toString());
        return campos;
    }

    private List<String> objetosJson(String json) {
        List<String> objetos = new ArrayList<>();
        if (json == null || json.isBlank()) return objetos;

        Matcher matcher = PADRAO_OBJETO_JSON.matcher(json);
        while (matcher.find()) objetos.add(matcher.group());
        return objetos;
    }

    private double numeroCampo(String valor) {
        try {
            double numero = Double.parseDouble(valor);
            return Double.isFinite(numero) ? numero : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private double numeroJson(String objeto, String chave) {
        Pattern padrao = PADROES_NUMERICOS_JSON.get(chave);
        if (padrao == null) return 0;
        Matcher matcher = padrao.matcher(objeto);
        return matcher.find() ? numeroCampo(matcher.group(1)) : 0;
    }

    private String textoJson(String objeto, String chave) {
        if (!"nome".equals(chave)) return "";
        Matcher matcher = PADRAO_NOME_JSON.matcher(objeto);
        if (!matcher.find()) return "";
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\\\", "\\");
    }

    /**
     * Chamado pelo Dashboard a cada atualização. Só consulta atividade física
     * dos discos e grava no disco a cada 5 segundos, se o histórico estiver ligado.
     */
    public synchronized void registrarSeNecessario(
            CpuInfo cpu,
            RamInfo ram,
            List<VolumeInfo> volumes,
            List<NetInfo> redes,
            List<ProcesInfo> processos
    ) {
        if (!habilitado || escritor == null || cpu == null || ram == null) {
            return;
        }

        long agora = System.currentTimeMillis();
        LocalDate hoje = LocalDate.now();

        // Cada dia tem seu próprio CSV: evita reescrever arquivos grandes na interface.
        if (diaArquivoAtual == null || !hoje.equals(diaArquivoAtual)) {
            fecharEscritor();

            if (!abrirArquivo()) {
                habilitado = false;
                salvarPreferencia(false);
                return;
            }
        }

        if (ultimaAmostraMs != 0
                && agora - ultimaAmostraMs < INTERVALO_AMOSTRA_MS) {
            return;
        }

        try {
            // O trabalho adicional de consultar discos físicos ocorre só por amostra.
            List<DiskInfo> discos = monitoramento.getDiskUsage();

            String linha = agora + ","
                    + csv(Instant.ofEpochMilli(agora).toString()) + ","
                    + numero(cpu.getCpuLoad()) + ","
                    + numero(ram.getUsageMemory()) + ","
                    + numero(ram.getTotaMemory()) + ","
                    + numero(ram.getPorctMemory()) + ","
                    + csv(redeParaJson(redes)) + ","
                    + csv(volumesParaJson(volumes)) + ","
                    + csv(discosParaJson(discos)) + ","
                    + csv(processosParaJson(processos));

            escritor.write(linha);
            escritor.newLine();
            ultimaAmostraMs = agora;
            amostrasDesdeFlush++;

            // Evita flush em cada amostra. Ao pausar ou fechar, tudo é descarregado.
            if (amostrasDesdeFlush >= AMOSTRAS_PARA_FLUSH) {
                escritor.flush();
                amostrasDesdeFlush = 0;
            }

        } catch (IOException e) {
            System.err.println(
                    "Erro ao salvar o histórico de monitoramento: "
                            + e.getMessage()
            );
            fecharEscritor();
            habilitado = false;
            salvarPreferencia(false);
        }
    }

    private boolean abrirArquivo() {
        try {
            Files.createDirectories(diretorio);

            LocalDate hoje = LocalDate.now();
            limparArquivosAntigos(hoje);

            arquivoAtual = diretorio.resolve(
                    PREFIXO_ARQUIVO + FORMATO_DIA.format(hoje) + ".csv"
            );

            boolean arquivoNovo = !Files.exists(arquivoAtual)
                    || Files.size(arquivoAtual) == 0;

            escritor = Files.newBufferedWriter(
                    arquivoAtual,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            if (arquivoNovo) {
                escritor.write(CABECALHO);
                escritor.newLine();
                escritor.flush();
            }

            diaArquivoAtual = hoje;
            return true;

        } catch (IOException e) {
            System.err.println(
                    "Não foi possível abrir o arquivo de histórico: "
                            + e.getMessage()
            );
            fecharEscritor();
            return false;
        }
    }

    /** Apaga somente arquivos diários anteriores à janela de retenção. */
    private void limparArquivosAntigos(LocalDate hoje) throws IOException {
        LocalDate primeiroDiaMantido = hoje.minusDays(DIAS_RETENCAO - 1L);

        try (DirectoryStream<Path> arquivos = Files.newDirectoryStream(
                diretorio,
                PREFIXO_ARQUIVO + "*.csv"
        )) {
            for (Path caminho : arquivos) {
                String nome = caminho.getFileName().toString();

                if (!nome.startsWith(PREFIXO_ARQUIVO)
                        || !nome.endsWith(".csv")) {
                    continue;
                }

                String data = nome.substring(
                        PREFIXO_ARQUIVO.length(),
                        nome.length() - ".csv".length()
                );

                try {
                    LocalDate diaArquivo = LocalDate.parse(data, FORMATO_DIA);
                    if (diaArquivo.isBefore(primeiroDiaMantido)) {
                        Files.deleteIfExists(caminho);
                    }
                } catch (RuntimeException ignorado) {
                    // Não remove arquivos com nomes que não sigam o formato esperado.
                }
            }
        }
    }

    private String redeParaJson(List<NetInfo> redes) {
        StringBuilder json = new StringBuilder("[");
        boolean primeiro = true;

        if (redes != null) {
            for (NetInfo rede : redes) {
                if (rede == null) {
                    continue;
                }

                if (!primeiro) {
                    json.append(',');
                }
                primeiro = false;

                // Não salvamos IP/MAC no histórico; guardamos o necessário para analisar o tráfego.
                json.append("{\"nome\":").append(jsonTexto(rede.getNome()))
                        .append(",\"nome_exibicao\":").append(jsonTexto(rede.getDisplay()))
                        .append(",\"estado\":").append(jsonTexto(rede.getStatus()))
                        .append(",\"download_mb_s\":").append(numero(rede.getRecv()))
                        .append(",\"upload_mb_s\":").append(numero(rede.getSent()))
                        .append('}');
            }
        }

        return json.append(']').toString();
    }

    private String volumesParaJson(List<VolumeInfo> volumes) {
        StringBuilder json = new StringBuilder("[");
        boolean primeiro = true;

        if (volumes != null) {
            for (VolumeInfo volume : volumes) {
                if (volume == null) {
                    continue;
                }

                if (!primeiro) {
                    json.append(',');
                }
                primeiro = false;

                double total = Math.max(0, volume.getTotal());
                double livre = Math.max(0, Math.min(total, volume.getLivre()));
                double usado = total - livre;

                json.append("{\"nome\":").append(jsonTexto(volume.getNome()))
                        .append(",\"total_gb\":").append(numero(total))
                        .append(",\"livre_gb\":").append(numero(livre))
                        .append(",\"usado_gb\":").append(numero(usado))
                        .append(",\"uso_percentual\":")
                        .append(numero(volume.getPorctVolume()))
                        .append('}');
            }
        }

        return json.append(']').toString();
    }

    private String discosParaJson(List<DiskInfo> discos) {
        StringBuilder json = new StringBuilder("[");
        boolean primeiro = true;

        if (discos != null) {
            for (DiskInfo disco : discos) {
                if (disco == null) {
                    continue;
                }

                if (!primeiro) {
                    json.append(',');
                }
                primeiro = false;

                json.append("{\"modelo\":").append(jsonTexto(disco.getModelo()))
                        .append(",\"leitura_mb_s\":").append(numero(disco.getRead()))
                        .append(",\"escrita_mb_s\":").append(numero(disco.getWrite()))
                        .append('}');
            }
        }

        return json.append(']').toString();
    }

    /**
     * System Idle Process representa o tempo em que a CPU está ociosa,
     * não um aplicativo consumindo CPU. Por isso não entra no histórico.
     */
    private boolean isProcessoOcioso(String nome, int pid) {
        if (pid == 0) {
            return true;
        }
        if (nome == null) {
            return false;
        }

        String nomeNormalizado = nome.trim().replaceAll("\\s+", " ");
        return nomeNormalizado.equalsIgnoreCase("Idle")
                || nomeNormalizado.equalsIgnoreCase("System Idle Process");
    }

    private String processosParaJson(List<ProcesInfo> processos) {
        StringBuilder json = new StringBuilder("[");
        boolean primeiro = true;
        List<ProcesInfo> principais = new ArrayList<>();

        if (processos != null) {
            for (ProcesInfo processo : processos) {
                if (processo != null
                        && !isProcessoOcioso(processo.getNome(), processo.getId())) {
                    principais.add(processo);
                }
            }
        }

        principais.sort(
                Comparator.comparingDouble(ProcesInfo::getCpu).reversed()
        );

        int limite = Math.min(10, principais.size());
        for (int i = 0; i < limite; i++) {
            ProcesInfo processo = principais.get(i);

            if (!primeiro) {
                json.append(',');
            }
            primeiro = false;

            json.append("{\"nome\":").append(jsonTexto(processo.getNome()))
                    .append(",\"pid\":").append(processo.getId())
                    .append(",\"cpu_percent\":").append(numero(processo.getCpu()))
                    .append(",\"memoria_mb\":").append(processo.getMemory())
                    .append('}');
        }

        return json.append(']').toString();
    }

    private String jsonTexto(String valor) {
        if (valor == null) {
            return "null";
        }

        StringBuilder json = new StringBuilder("\"");

        for (int i = 0; i < valor.length(); i++) {
            char caractere = valor.charAt(i);

            switch (caractere) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (caractere < 0x20) {
                        json.append(String.format(Locale.ROOT, "\\u%04x", (int) caractere));
                    } else {
                        json.append(caractere);
                    }
                }
            }
        }

        return json.append('"').toString();
    }

    private String csv(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    private String numero(double valor) {
        if (!Double.isFinite(valor)) {
            return "0";
        }
        return Double.toString(valor);
    }

    private void salvarPreferencia(boolean valor) {
        preferencias.putBoolean(CHAVE_HABILITADO, valor);
        try {
            preferencias.flush();
        } catch (BackingStoreException e) {
            System.err.println(
                    "Não foi possível salvar a preferência do histórico: "
                            + e.getMessage()
            );
        }
    }

    private void fecharEscritor() {
        if (escritor == null) {
            return;
        }

        try {
            escritor.flush();
            escritor.close();
        } catch (IOException e) {
            System.err.println(
                    "Erro ao fechar o histórico: " + e.getMessage()
            );
        } finally {
            escritor = null;
            amostrasDesdeFlush = 0;
            diaArquivoAtual = null;
        }
    }

    /** Fecha os recursos sem alterar a preferência salva para a próxima execução. */
    public synchronized void fechar() {
        fecharEscritor();
    }

    /** Estatísticas agregadas de um processo para a última leitura histórica. */
    public static final class ResumoProcesso {
        private final String nome;
        private final int pid;
        private double somaCpu;
        private double picoCpu;
        private double memoriaMaxMb;
        private int aparicoes;

        private ResumoProcesso(String nome, int pid) {
            this.nome = nome == null || nome.isBlank() ? "Processo desconhecido" : nome;
            this.pid = pid;
        }

        private void adicionar(double cpu, double memoriaMb) {
            double cpuSegura = Double.isFinite(cpu) ? Math.max(0, cpu) : 0;
            double memoriaSegura = Double.isFinite(memoriaMb) ? Math.max(0, memoriaMb) : 0;
            somaCpu += cpuSegura;
            picoCpu = Math.max(picoCpu, cpuSegura);
            memoriaMaxMb = Math.max(memoriaMaxMb, memoriaSegura);
            aparicoes++;
        }

        public String getNome() { return nome; }
        public int getPid() { return pid; }
        public double getCpuMedia() { return aparicoes == 0 ? 0 : somaCpu / aparicoes; }
        public double getPicoCpu() { return picoCpu; }
        public double getMemoriaMaxMb() { return memoriaMaxMb; }
        public int getAparicoes() { return aparicoes; }
    }

}
