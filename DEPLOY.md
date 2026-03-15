# PriceMonitor — Deploy no Render (Plano Free)

Guia completo para subir o backend (Spring Boot) + frontend (React/Vite) + banco PostgreSQL no Render usando GitHub.

---

## Estrutura do Repositório

```
pricemonitor/               ← raiz do repo GitHub
├── src/                    ← código Java (Spring Boot)
├── pricemonitor-web/       ← código React/Vite
├── pom.xml
├── mvnw / mvnw.cmd
├── render.yaml             ← Infrastructure as Code (opcional)
└── .gitignore
```

---

## Pré-requisitos

- Conta no [GitHub](https://github.com)
- Conta no [Render](https://render.com)
- Gmail com **App Password** ativada (para envio de e-mails)

---

## Parte 1 — Subir o código no GitHub

### 1.1 — Criar repositório no GitHub

1. Acesse github.com → **New repository**
2. Nome: `pricemonitor`
3. Visibilidade: **Private** (recomendado)
4. Não inicialize com README
5. Clique em **Create repository**

### 1.2 — Enviar o código

Abra o terminal na pasta raiz do projeto (`pricemonitor/`) e execute:

```bash
git init
git add .
git commit -m "chore: initial commit"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/pricemonitor.git
git push -u origin main
```

---

## Parte 2 — Criar o banco de dados no Render

1. Acesse [render.com](https://render.com) → **New → PostgreSQL**
2. Preencha:
   - **Name:** `pricemonitor-db`
   - **Database:** `pricemonitor`
   - **User:** `pricemonitor`
   - **Plan:** Free
3. Clique em **Create Database**
4. Aguarde ficar **Available** e copie a **Internal Database URL** — você vai usar no passo seguinte

> ⚠️ O banco free do Render expira após 90 dias. Faça backup antes de expirar.

---

## Parte 3 — Deploy do Backend (Spring Boot)

1. No Render → **New → Web Service**
2. Conecte o repositório GitHub `pricemonitor`
3. Preencha:
   - **Name:** `pricemonitor-api`
   - **Root Directory:** *(deixe em branco — raiz do repo)*
   - **Runtime:** Java
   - **Build Command:** `./mvnw clean package -DskipTests`
   - **Start Command:** `java -jar target/pricemonitor-0.0.1-SNAPSHOT.jar`
   - **Plan:** Free

4. Em **Environment Variables**, adicione:

| Chave | Valor |
|-------|-------|
| `SPRING_PROFILES_ACTIVE` | `smtp,prod` |
| `DB_URL` | Internal Database URL copiada do passo anterior |
| `DB_USERNAME` | `pricemonitor` |
| `DB_PASSWORD` | senha gerada pelo Render (fica na página do banco) |
| `JWT_SECRET` | string aleatória longa (ex: use [randomkeygen.com](https://randomkeygen.com)) |
| `MAIL_USERNAME` | seu e-mail Gmail (ex: `seu@gmail.com`) |
| `MAIL_PASSWORD` | App Password do Gmail (16 caracteres sem espaços) |
| `CORS_ALLOWED_ORIGINS` | URL do frontend — preencher **depois** de criar o frontend |

5. Clique em **Create Web Service**

> 💡 Anote a URL gerada, exemplo: `https://pricemonitor-api.onrender.com`

### Como gerar o App Password do Gmail

1. Acesse [myaccount.google.com/security](https://myaccount.google.com/security)
2. Ative **Verificação em duas etapas** (obrigatório)
3. Busque por **"Senhas de app"**
4. Crie uma senha para "Outro (nome personalizado)" → `PriceMonitor`
5. Copie os 16 caracteres gerados

---

## Parte 4 — Deploy do Frontend (React/Vite)

1. No Render → **New → Static Site**
2. Conecte o mesmo repositório GitHub
3. Preencha:
   - **Name:** `pricemonitor-web`
   - **Root Directory:** `pricemonitor-web`
   - **Build Command:** `npm install && npm run build`
   - **Publish Directory:** `dist`
   - **Plan:** Free

4. Em **Environment Variables**, adicione:

| Chave | Valor |
|-------|-------|
| `VITE_API_BASE_URL` | URL do backend (ex: `https://pricemonitor-api.onrender.com`) |

5. Clique em **Create Static Site**

> 💡 Anote a URL gerada, exemplo: `https://pricemonitor-web.onrender.com`

---

## Parte 5 — Finalizar configuração do CORS

Agora que você tem a URL do frontend, volte ao backend:

1. No Render → `pricemonitor-api` → **Environment**
2. Atualize a variável:

| Chave | Valor |
|-------|-------|
| `CORS_ALLOWED_ORIGINS` | `https://pricemonitor-web.onrender.com` |

3. O Render vai redeployar automaticamente.

---

## Parte 6 — Verificar se está funcionando

Acesse a URL do frontend e tente:
1. Criar uma conta
2. Fazer login
3. Cadastrar um produto do Mercado Livre
4. Clicar em "Verificar agora"

Para ver os logs do backend: Render → `pricemonitor-api` → **Logs**

---

## ⚠️ Limitações do Plano Free

| Limitação | Detalhe |
|-----------|---------|
| **Backend hiberna** após 15 min sem requisição | O job de 30min pode não rodar se ninguém acessar o sistema. Solução: use [UptimeRobot](https://uptimerobot.com) para fazer ping a cada 10min. |
| **Banco expira** em 90 dias | Faça backup ou migre para um banco externo (ex: Supabase free tier) |
| **512MB RAM** no backend | Suficiente para este projeto |
| **Build lento** (~3-5min) no primeiro deploy | Normal |

---

## Keep-Alive com UptimeRobot (recomendado)

Para evitar que o backend hiberne e o job de preços pare de rodar:

1. Crie conta em [uptimerobot.com](https://uptimerobot.com) (free)
2. **New Monitor** → HTTP(s)
3. URL: `https://pricemonitor-api.onrender.com/actuator/health`
4. Intervalo: **5 minutos**

> Isso faz um ping a cada 5min, mantendo o backend ativo 24h.

---

## Usando o render.yaml (alternativa)

Em vez de criar os serviços manualmente, você pode usar o `render.yaml` incluído no projeto:

1. No Render → **New → Blueprint**
2. Conecte o repositório
3. O Render lê o `render.yaml` e cria tudo automaticamente
4. Preencha manualmente as variáveis marcadas com `sync: false`:
   - `MAIL_USERNAME`
   - `MAIL_PASSWORD`
   - `CORS_ALLOWED_ORIGINS`
   - `VITE_API_BASE_URL`

---

## Atualizando o projeto

Qualquer `git push` para a branch `main` dispara um novo deploy automaticamente.

```bash
git add .
git commit -m "feat: minha alteração"
git push
```
