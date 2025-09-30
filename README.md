# Cliente Desktop (Swing) — Pedidos

Aplicação **Java 8 + Swing** que envia pedidos para o **backend** e realiza **polling** do status por HTTP.

> ⚠️ **Atenção:** Este cliente **depende do backend**. Sem o serviço Spring Boot rodando, o app não funciona.
> Siga primeiro o README do backend aqui: **<https://github.com/ricardolimma/pedidos-backend>**

---

## 🧩 Tecnologias
- **Java 8**
- **Swing** (UI)
- **OkHttp** (cliente HTTP)
- **Jackson** (JSON)
- **Executors / ScheduledExecutorService** (polling periódico)

---

## 🚦 Pré-requisitos
- **JDK 8** instalado e configurado no PATH
- **Maven 3.8+**
- **Backend rodando** (clonado e iniciado a partir do repositório indicado acima)

---

## 🔗 Conectando ao Backend
O cliente usa por padrão a URL:
```
http://localhost:8080/api/pedidos
```
Se o backend estiver em outra máquina/porta, edite a constante `baseUrl` na classe:
```
src/main/java/com/swing/view/OrderClientFrame.java
```

---

## ▶️ Como rodar o cliente
```bash
# dentro do diretório do projeto Swing
mvn -q exec:java
```
ou empacote e rode:
```bash
mvn -q clean package
java -jar target/pedidos-swing-0.0.1.jar
```

---

## 🧪 Fluxo de teste manual
1. **Garanta o backend no ar** (veja o README do backend).
2. Abra o app Swing.
3. Informe *Produto* e *Quantidade* e clique em **Enviar Pedido**.
4. Veja o **ID** aparecer na tabela e o **status** evoluir (RECEBIDO → PROCESSANDO → SUCESSO/FALHA).

---

## 🛠️ Configuração opcional
- **Timeouts / Proxy HTTP**: se necessário, configure um `OkHttpClient` customizado no `OrderClientFrame`.
- **Cores/estilo**: ajuste os renderers da tabela para o tema que preferir.

---

## ❓Resolução de problemas
- **“Erro de conexão” ao enviar pedido** → o backend não está acessível no `baseUrl` informado.
- **Status não muda para SUCESSO/FALHA** → confirme que o cliente está usando **o ID retornado pelo backend (HTTP 202)** para fazer o polling.
- **HTTP 400 ao enviar** → verifique os campos obrigatórios (*produto* e *quantidade > 0*).


