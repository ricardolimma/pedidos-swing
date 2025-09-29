package com.swing.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
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
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Status"}, 0);
    private final JTable table = new JTable(model);


    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl = "http://localhost:8080/api/pedidos";
    private final Set<String> pendentes = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    public OrderClientFrame() {
        super("Pedidos");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        initComponents();
        scheduler.scheduleAtFixedRate(this::pollStatus, 3, 3, TimeUnit.SECONDS);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                scheduler.shutdownNow();
            }
        });
    }


    private void initComponents() {
        // Campos de texto
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
        btnEnviar.addActionListener(e -> enviarPedido());

        // Painel de formulário
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 8, 0, 8);
        gbc.gridy = 0;
        gbc.gridx = 0;
        form.add(new JLabel("Produto:"), gbc);
        gbc.gridx = 1;
        txtProduto.setPreferredSize(new Dimension(120, 32));
        form.add(txtProduto, gbc);
        gbc.gridx = 2;
        form.add(new JLabel("Qtd:"), gbc);
        gbc.gridx = 3;
        txtQuantidade.setPreferredSize(new Dimension(60, 32));
        form.add(txtQuantidade, gbc);
        gbc.gridx = 4;
        form.add(btnEnviar, gbc);
        add(form, BorderLayout.NORTH);

        // Tabela
        table.setRowHeight(28);
        table.getTableHeader().setFont(TABLE_HEADER_FONT);
        table.getTableHeader().setBackground(TABLE_HEADER_COLOR);
        table.setFont(TABLE_FONT);
        table.setSelectionBackground(new Color(200, 220, 255));
        table.setGridColor(new Color(220, 220, 220));
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? TABLE_ROW_ALT_COLOR : Color.WHITE);
                }
                return c;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(8, 16, 16, 16));
        add(scroll, BorderLayout.CENTER);
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
        Map<String, Object> body = new HashMap<>();
        body.put("produto", produto);
        body.put("quantidade", quantidade);
        MediaType json = MediaType.parse("application/json");
        Request req = new Request.Builder()
                .url(baseUrl)
                .post(RequestBody.create(json, toJson(body)))
                .build();
        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                SwingUtilities.invokeLater(() -> showMessage(
                        "Não foi possível conectar ao servidor.\nPor favor, verifique sua conexão ou tente novamente mais tarde.",
                        "Erro de conexão", JOptionPane.ERROR_MESSAGE));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 202) {
                    Map<?, ?> res = mapper.readValue(response.body().string(), Map.class);
                    final String id = String.valueOf(res.get("id"));
                    SwingUtilities.invokeLater(() -> {
                        txtProduto.setText("");
                        txtQuantidade.setText("");
                        model.addRow(new Object[]{id, "Pendente"});
                        pendentes.add(id);
                    });
                } else {
                    final String msg = response.message();
                    SwingUtilities.invokeLater(() -> showMessage("Erro: " + msg, "Erro", JOptionPane.ERROR_MESSAGE));
                }
            }
        });
    }

    /**
     * Serializa um objeto para JSON.
     */
    private String toJson(Map<String, Object> map) {
        try {
            return mapper.writeValueAsString(map);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void pollStatus() {
        if (pendentes.isEmpty()) return;
        Request req = new Request.Builder()
                .url(baseUrl + "/status?ids=" + String.join(",", pendentes))
                .build();
        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    List<?> lista = mapper.readValue(response.body().string(), List.class);
                    for (Object o : lista) {
                        Map<?, ?> map = (Map<?, ?>) o;
                        final String id = String.valueOf(map.get("id"));
                        final String status = String.valueOf(map.get("status"));
                        SwingUtilities.invokeLater(() -> updateOrderStatus(id, status));
                    }
                }
            }
        });
    }


    private void updateOrderStatus(String id, String status) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).equals(id)) {
                model.setValueAt(status, i, 1);
                break;
            }
        }
    }


    private void showMessage(String message, String title, int type) {
        JOptionPane.showMessageDialog(this, message, title, type);
    }
}
