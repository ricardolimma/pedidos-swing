package com.swing.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class OrderClientFrame extends JFrame {

    private static final Color BUTTON_COLOR = new Color(0, 120, 215);
    private static final Color TABLE_HEADER_COLOR = new Color(230, 230, 250);
    private static final Color TABLE_ROW_ALT_COLOR = new Color(245, 248, 255);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TABLE_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private final JTextField txtProduto = new JTextField();
    private final JTextField txtQuantidade = new JTextField();
    private final JButton btnEnviar = new JButton("Enviar Pedido");

    private final DefaultTableModel model =
            new DefaultTableModel(new Object[]{"ID", "Status", "Detalhe"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
    private final JTable table = new JTable(model);

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl = "http://localhost:8080/api/pedidos";

    private final Set<String> pendentes = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public OrderClientFrame() {
        super("Pedidos");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(760, 460);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initComponents();

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override public void run() { pollStatus(); }
        }, 3, 3, TimeUnit.SECONDS);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                scheduler.shutdownNow();
            }
        });
    }

    private void initComponents() {

        txtProduto.setBorder(BorderFactory.createCompoundBorder(
                txtProduto.getBorder(), new EmptyBorder(8, 8, 8, 8)));
        txtQuantidade.setBorder(BorderFactory.createCompoundBorder(
                txtQuantidade.getBorder(), new EmptyBorder(8, 8, 8, 8)));

        // Botão
        btnEnviar.setBackground(BUTTON_COLOR);
        btnEnviar.setForeground(Color.WHITE);
        btnEnviar.setFont(BUTTON_FONT);
        btnEnviar.setFocusPainted(true);
        btnEnviar.setBorder(UIManager.getBorder("Button.border"));
        btnEnviar.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                enviarPedido();
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 16, 8, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 8, 0, 8);
        gbc.gridy = 0;
        gbc.gridx = 0; form.add(new JLabel("Produto:"), gbc);
        gbc.gridx = 1; txtProduto.setPreferredSize(new Dimension(200, 32)); form.add(txtProduto, gbc);
        gbc.gridx = 2; form.add(new JLabel("Qtd:"), gbc);
        gbc.gridx = 3; txtQuantidade.setPreferredSize(new Dimension(80, 32)); form.add(txtQuantidade, gbc);
        gbc.gridx = 4; form.add(btnEnviar, gbc);
        add(form, BorderLayout.NORTH);

        table.setRowHeight(28);
        table.getTableHeader().setFont(TABLE_HEADER_FONT);
        table.getTableHeader().setBackground(TABLE_HEADER_COLOR);
        table.setFont(TABLE_FONT);
        table.setSelectionBackground(new Color(200, 220, 255));
        table.setGridColor(new Color(220, 220, 220));
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);

        final DefaultTableCellRenderer coloredStatusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? TABLE_ROW_ALT_COLOR : Color.WHITE);
                }
                String s = value == null ? "" : value.toString();
                Color fg = c.getForeground();
                if ("PROCESSANDO".equalsIgnoreCase(s) || "RECEBIDO".equalsIgnoreCase(s)) {
                    fg = new Color(180, 120, 0); // amarelo queimado
                } else if ("SUCESSO".equalsIgnoreCase(s)) {
                    fg = new Color(0, 128, 0);   // verde
                } else if ("FALHA".equalsIgnoreCase(s)) {
                    fg = new Color(178, 34, 34); // vermelho
                } else if ("DESCONHECIDO".equalsIgnoreCase(s)) {
                    fg = new Color(100, 100, 100); // cinza
                }
                c.setForeground(fg);
                return c;
            }
        };
        table.getColumnModel().getColumn(1).setCellRenderer(coloredStatusRenderer);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(8, 16, 16, 16));
        add(scroll, BorderLayout.CENTER);
    }

    static class NovoPedido {
        public String produto;
        public int quantidade;
        NovoPedido(String produto, int quantidade) {
            this.produto = produto;
            this.quantidade = quantidade;
        }
    }

    private void enviarPedido() {
        final String produto = txtProduto.getText().trim();
        final String qtdStr = txtQuantidade.getText().trim();

        if (produto.isEmpty() || qtdStr.isEmpty()) {
            showMessage("Preencha produto e quantidade.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int quantidade;
        try {
            quantidade = Integer.parseInt(qtdStr);
        } catch (NumberFormatException ex) {
            showMessage("Quantidade inválida.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (quantidade <= 0) {
            showMessage("Quantidade deve ser maior que zero.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        NovoPedido payload = new NovoPedido(produto, quantidade);

        MediaType json = MediaType.parse("application/json");
        Request req = new Request.Builder()
                .url(baseUrl)
                .addHeader("Accept", "application/json")
                .post(RequestBody.create(json, toJson(payload)))
                .build();

        btnEnviar.setEnabled(false);

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        btnEnviar.setEnabled(true);
                        showMessage("Não foi possível conectar ao servidor.\n" + e.getMessage(),
                                "Erro de conexão", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body() != null ? response.body().string() : "";

                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        btnEnviar.setEnabled(true);

                        if (code == 202) {
                            try {
                                Map<?, ?> res = mapper.readValue(body, Map.class);
                                final String id = String.valueOf(res.get("id")); // <-- usa ID do BACKEND
                                if (id == null || id.trim().isEmpty()) {
                                    showMessage("Backend retornou 202, mas sem ID no corpo.", "Aviso", JOptionPane.WARNING_MESSAGE);
                                    return;
                                }
                                txtProduto.setText("");
                                txtQuantidade.setText("");
                                model.addRow(new Object[]{id, "RECEBIDO", ""});
                                pendentes.add(id);
                            } catch (Exception ex) {
                                showMessage("Falha ao ler ID retornado pelo backend.", "Erro", JOptionPane.ERROR_MESSAGE);
                            }
                        } else if (code == 400) {
                            showMessage("Validação falhou (400).", "Erro", JOptionPane.ERROR_MESSAGE);
                        } else {
                            showMessage("Erro: HTTP " + code, "Erro", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
            }
        });
    }

    private String toJson(Object obj) {
        try { return mapper.writeValueAsString(obj); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    private void pollStatus() {
        if (pendentes.isEmpty()) return;
        List<String> ids = new ArrayList<String>(pendentes); // cópia para evitar ConcurrentModification
        for (final String id : ids) {
            Request req = new Request.Builder().url(baseUrl + "/status/" + id).get().build();
            http.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    System.out.println("[poll] falha ao consultar " + id + ": " + e.getMessage());
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    final int code = response.code();
                    if (code == 404) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() { updateOrderStatus(id, "DESCONHECIDO", ""); }
                        });
                        return;
                    }
                    if (!response.isSuccessful()) return;

                    String respBody = response.body() != null ? response.body().string() : "";
                    Map<?, ?> map;
                    try {
                        map = mapper.readValue(respBody, Map.class);
                    } catch (Exception ex) {
                        System.out.println("[poll] JSON inválido para " + id + ": " + ex.getMessage());
                        System.out.println("[poll] recebido: " + respBody);
                        return;
                    }
                    final String status = String.valueOf(map.get("status"));
                    final String detalhe = map.containsKey("mensagemErro") && map.get("mensagemErro") != null
                            ? String.valueOf(map.get("mensagemErro")) : "";

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() { updateOrderStatus(id, status, detalhe); }
                    });
                }
            });
        }
    }

    private void updateOrderStatus(String id, String status, String detalhe) {
        String s = status == null ? "" : status.trim();
        boolean finalizado = "SUCESSO".equalsIgnoreCase(s) || "FALHA".equalsIgnoreCase(s);

        for (int i = 0; i < model.getRowCount(); i++) {
            String tableId = String.valueOf(model.getValueAt(i, 0)).trim();
            if (tableId.equals(id)) {
                model.setValueAt(s.isEmpty() ? "RECEBIDO" : s, i, 1);
                model.setValueAt(detalhe == null ? "" : detalhe, i, 2);
                if (finalizado) pendentes.remove(id);
                return;
            }
        }

        model.addRow(new Object[]{id, s.isEmpty() ? "RECEBIDO" : s, detalhe == null ? "" : detalhe});
        if (finalizado) pendentes.remove(id);
    }

    private void showMessage(String message, String title, int type) {
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                new OrderClientFrame().setVisible(true);
            }
        });
    }
}
