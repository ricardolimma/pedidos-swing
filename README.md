# Cliente Swing – Pedidos (OrderClientFrame)

Aplicativo **Java Swing** que envia pedidos para um backend HTTP e faz **polling** periódico para consultar o **status** de processamento.  
Ele usa **OkHttp** para as chamadas HTTP e **Jackson** para serialização JSON.

> **Arquitetura resumida**
> - **Cliente (Swing)**: envia `POST /api/pedidos` e, a cada 3s, chama `GET /api/pedidos/status/{id}` para atualizar a tabela.
> - **Backend (Spring Boot)**: recebe o pedido, retorna `202 Accepted` com o `id`, e processa o pedido de forma assíncrona (ex.: via RabbitMQ). O status é exposto por HTTP.

---

## ✨ Funcionalidades

- Formulário com **produto** e **quantidade**.
- Gera um **UUID** local para o pedido e envia para o backend (`POST /api/pedidos`).
- Exibe os pedidos e seus **status** em uma tabela, com **polling** a cada 3 segundos.
- Atualização do status em tempo real (até um estado final, como **SUCESSO** ou **FALHA**).

---

## 🧩 Tecnologias

- **Java 8**
- **Swing** (UI)
- **OkHttp** (cliente HTTP)
- **Jackson** (JSON)
- **Executors/ScheduledExecutorService** (agendamento do polling)

---

## 🗂️ Estrutura do Cliente

Classe principal do cliente: `com.swing.view.OrderClientFrame`

- **Campos principais**
    - `baseUrl`: URL base do backend (padrão: `http://localhost:8080/api/pedidos`)
    - `OkHttpClient http`: cliente HTTP
    - `ObjectMapper mapper`: serialização JSON
    - `ScheduledExecutorService scheduler`: agenda `pollStatus()` a cada 3s
    - `DefaultTableModel model` + `JTable table`: renderizam `ID` e `Status`
    - `Set<String> pendentes`: controla IDs em acompanhamento

- **Fluxo**
    1. Usuário preenche **Produto** e **Qtd** e clica **Enviar Pedido**.
    2. Cliente cria um `UUID`, monta o JSON e faz `POST /api/pedidos`.
    3. Se o backend responder **202**, o pedido entra na tabela como **ENVIADO, AGUARDANDO PROCESSO** e o ID é adicionado a `pendentes`.
    4. O **polling** chama `GET /api/pedidos/status/{id}` para cada ID pendente e atualiza a tabela conforme o retorno.

---

## 🔌 Endpoints esperados no Backend

O cliente assume os seguintes endpoints:

- **POST `/api/pedidos`**  
  **Body** (JSON):
  ```json
  {
    "id": "4c8a3312-7c5e-4c4a-8c3f-0b2c9f1f2c11",
    "produto": "Teclado Mecânico",
    "quantidade": 2,
    "dataCriacao": "2025-09-29T12:34:56"
  }
