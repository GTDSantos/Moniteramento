
package com.monitoramento.hardwer;

import com.monitoramento.model.NetInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetMonitor {

    private final HardwareAbstractionLayer hal;

    private final Map<String, Long> bytesRecebidosAnteriores =
            new HashMap<>();
    private final Map<String, Long> bytesEnviadosAnteriores =
            new HashMap<>();
    private final Map<String, Long> temposAnteriores =
            new HashMap<>();

    private String ultimaInterface = null;

    public NetMonitor(HardwareAbstractionLayer hal) {
        this.hal = hal;
    }

    public List<NetInfo> getNetInfo() {

        List<NetInfo> resultado = new ArrayList<>();

        // Consulta novamente todas as interfaces.
        List<NetworkIF> interfaces = hal.getNetworkIFs();

        long agora = System.nanoTime();

        Map<String, NetworkIF> candidatas = new HashMap<>();
        Map<String, double[]> velocidades = new HashMap<>();

        NetworkIF interfaceComMaisTrafego = null;
        double maiorTrafego = 0;

        for (NetworkIF rede : interfaces) {

            // Atualiza os contadores de rede.
            boolean atualizou = rede.updateAttributes();

            if (!atualizou) {
                continue;
            }

            String chave = chaveInterface(rede);

            long recebidos = rede.getBytesRecv();
            long enviados = rede.getBytesSent();

            double download = 0;
            double upload = 0;

            if (bytesRecebidosAnteriores.containsKey(chave)
                    && bytesEnviadosAnteriores.containsKey(chave)
                    && temposAnteriores.containsKey(chave)) {

                double segundos =
                        (agora - temposAnteriores.get(chave))
                                / 1_000_000_000.0;

                if (segundos > 0) {

                    long diferencaRecebidos = Math.max(
                            0,
                            recebidos - bytesRecebidosAnteriores.get(chave)
                    );

                    long diferencaEnviados = Math.max(
                            0,
                            enviados - bytesEnviadosAnteriores.get(chave)
                    );

                    // MB/s, para manter compatibilidade com a tela.
                    download = diferencaRecebidos
                            / (1024.0 * 1024.0 * segundos);

                    upload = diferencaEnviados
                            / (1024.0 * 1024.0 * segundos);
                }
            }

            bytesRecebidosAnteriores.put(chave, recebidos);
            bytesEnviadosAnteriores.put(chave, enviados);
            temposAnteriores.put(chave, agora);

            // Ignora interfaces inativas e endereços sem uso normal.
            if (!interfaceValida(rede)) {
                continue;
            }

            candidatas.put(chave, rede);
            velocidades.put(chave, new double[]{download, upload});

            double trafego = download + upload;

            // Durante a atividade, prioriza quem realmente
            // está movimentando mais dados.
            if (trafego > maiorTrafego) {
                maiorTrafego = trafego;
                interfaceComMaisTrafego = rede;
            }
        }

        // 1. Durante um download, seleciona a interface
        // com o maior tráfego observado.
        NetworkIF escolhida = null;

        if (interfaceComMaisTrafego != null
                && maiorTrafego > 0.00001) {
            escolhida = interfaceComMaisTrafego;
        }

        // 2. Sem tráfego significativo, tenta a rota padrão
        // que o próprio Windows escolheria para a internet.
        if (escolhida == null) {
            NetworkIF rota = encontrarInterfaceInternet(interfaces);

            if (rota != null) {
                String chave = chaveInterface(rota);

                if (candidatas.containsKey(chave)) {
                    escolhida = candidatas.get(chave);
                }
            }
        }

        // 3. Se a rota não puder ser identificada, mantém
        // a última interface válida.
        if (escolhida == null && ultimaInterface != null) {
            escolhida = candidatas.get(ultimaInterface);
        }

        // 4. Última alternativa: primeira interface válida.
        if (escolhida == null && !candidatas.isEmpty()) {
            escolhida = candidatas.values().iterator().next();
        }

        if (escolhida == null) {
            return resultado;
        }

        String chave = chaveInterface(escolhida);
        ultimaInterface = chave;

        double[] taxa = velocidades.get(chave);

        if (taxa == null) {
            return resultado;
        }

        List<String> ips = new ArrayList<>();

        for (String ip : escolhida.getIPv4addr()) {
            ips.add(ip);
        }

        String status =
                escolhida.getIfOperStatus() == NetworkIF.IfOperStatus.UP
                        ? "ativo"
                        : "inativo";

        NetInfo info = new NetInfo(
                escolhida.getName(),
                escolhida.getDisplayName(),
                escolhida.getMacaddr(),
                ips,
                status,
                taxa[0],
                taxa[1]
        );

        resultado.add(info);

        return resultado;
    }

    private String chaveInterface(NetworkIF rede) {
        return rede.getIndex() + "|" + rede.getName();
    }

    private boolean interfaceValida(NetworkIF rede) {

        if (rede.getIfOperStatus() != NetworkIF.IfOperStatus.UP) {
            return false;
        }

        // Tipos de interface: loopback (24) e túnel (131).
        if (rede.getIfType() == 24 || rede.getIfType() == 131) {
            return false;
        }

        for (String ip : rede.getIPv4addr()) {
            if (ip != null
                    && !ip.startsWith("127.")
                    && !ip.startsWith("169.254.")
                    && !ip.equals("0.0.0.0")) {
                return true;
            }
        }

        return false;
    }

    private NetworkIF encontrarInterfaceInternet(
            List<NetworkIF> interfaces) {

        try (DatagramSocket socket = new DatagramSocket()) {

            socket.connect(
                    new InetSocketAddress("8.8.8.8", 53)
            );

            String ipEscolhido =
                    socket.getLocalAddress().getHostAddress();

            for (NetworkIF rede : interfaces) {
                for (String ip : rede.getIPv4addr()) {
                    if (ipEscolhido.equals(ip)) {
                        return rede;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(
                    "Falha ao identificar a rota da internet: "
                            + e.getMessage()
            );
        }

        return null;
    }
}
