# Resume RAG Analyzer 🚀 | Plataforma de IA para Recrutamento e Seleção (RH)

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.15-FF0000?style=for-the-badge&logo=quarkus&logoColor=white)](https://quarkus.io/)
[![Angular](https://img.shields.io/badge/Angular-18-DD0031?style=for-the-badge&logo=angular&logoColor=white)](https://angular.dev/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-AI-green?style=for-the-badge)](https://docs.langchain4j.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-PGVector-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)

O **Resume RAG Analyzer** é uma plataforma de análise inteligente de currículos projetada para transformar o fluxo de trabalho de profissionais de **Recrutamento e Seleção (RH)**. Utilizando **IA Generativa** e **RAG (Retrieval-Augmented Generation)**, o sistema automatiza a triagem técnica e comportamental, permitindo uma análise profunda e comparativa de candidatos em escala.

---

## 🎯 Objetivo e Funcionalidades Principais

O foco principal da plataforma é reduzir o tempo gasto na triagem manual de currículos, garantindo que os melhores talentos sejam identificados através de critérios objetivos e análise semântica.

### ⚡ Análise em Paralelo e Classificação
- **Processamento em Lote:** Permite o upload simultâneo de múltiplos currículos para uma mesma vaga.
- **Extração de Pontos-Chave:** A IA identifica competências técnicas, frameworks, anos de experiência e soft skills.
- **Ranking por Relevância:** Classifica automaticamente os candidatos com base em um *Match Score* calculado contra a descrição da vaga.
- **Análise Baseada em Evidências:** A IA não apenas pontua, mas justifica a classificação citando trechos específicos do currículo.

### 🛡️ Triagem de Contas e Segurança
Para manter a organização e a governança da empresa, o sistema inclui um fluxo de controle de acesso:
- **Aprovação Administrativa:** Novos cadastros entram em modo `PENDING_APPROVAL`, exigindo que um administrador aprove o acesso.
- **Organização por Perfis:** Separação clara entre administradores (que gerenciam usuários) e recrutadores (que realizam as análises).
- **Segurança de Dados:** Autenticação via **Stateless JWT** com assinatura assimétrica (RSA).
- **Zero Config Security:** O sistema gera automaticamente as chaves RSA necessárias ao iniciar via Docker, garantindo segurança imediata sem configuração manual.

---

## 🏗️ Arquitetura e Design Patterns

O projeto foi construído sobre pilares sólidos de engenharia de software para garantir escalabilidade e manutenibilidade:

### 🔹 Backend (Hexagonal Architecture / Ports & Adapters)
Implementado com **Java 17** e **Quarkus**:
- **Domain-Driven Design (DDD):** Modelos de domínio ricos e regras de negócio isoladas.
- **Event-Driven:** Processamento assíncrono de currículos utilizando o **Vert.x EventBus** para garantir que a interface nunca fique travada durante análises complexas.
- **Persistência Vetorial:** Utilização do **PGVector** para buscas semânticas rápidas, permitindo que a IA "consulte" o currículo de forma eficiente.

### 🔹 Frontend (Modern Angular Architecture)
Desenvolvido com **Angular 18**:
- **Signals & RxJS:** Gerenciamento de estado reativo de alta performance.
- **Server-Sent Events (SSE):** Atualização em tempo real do status das análises sem necessidade de refresh.
- **Interface Responsiva:** Construída com **Tailwind CSS**, focada na experiência do recrutador.

---

## 🛠️ Stack Tecnológica

### Backend
- **Quarkus 3.15 LTS:** Framework Java otimizado para nuvem.
- **LangChain4j:** Orquestração de LLMs (OpenAI, Gemini, Groq).
- **Hibernate Reactive:** Acesso a dados não bloqueante.
- **PostgreSQL + PGVector:** Banco de dados relacional e vetorial.

### Frontend
- **Angular 18:** Framework SPA moderno.
- **Tailwind CSS:** Estilização utilitária e responsiva.
- **Chart.js:** Visualização gráfica dos resultados e comparativos de candidatos.

---

## 🤖 Engine de Inteligência Artificial (RAG Profissional)

O diferencial deste projeto é o uso de uma **Engine de RAG Otimizada**, projetada para alta performance:

- **Chunking Estratégico:** Divisão inteligente de documentos para que o contexto técnico nunca seja fragmentado.
- **Batch Embedding:** Conversão de múltiplos currículos em vetores em operações otimizadas de rede.
- **Multi-Provedor:** Suporte flexível para OpenAI (GPT-4), Google Gemini ou modelos locais (via Ollama).
- **Robustez Determinística:** Utiliza `JSON Mode` para garantir que as análises sejam sempre estruturadas e fáceis de visualizar no dashboard.

---

## 🚀 Como Executar (Docker First)

O projeto está totalmente conteinerizado para facilitar o setup inicial.

### Pré-requisitos
- Docker e Docker Compose.
- Uma API Key de um provedor de IA (OpenAI ou Gemini).

### Passo a Passo
1. Clone o repositório.
2. Configure as variáveis de ambiente:
   ```bash
   cp .env.example .env
   # Edite o .env com sua AI_API_KEY
   ```
3. Suba o ambiente:
   ```bash
   docker compose up -d --build
   ```

### URLs de Acesso
- **Frontend:** `http://localhost:8081`
- **Backend API:** `http://localhost:8080/q/swagger-ui`
- **Mailpit (E-mails):** `http://localhost:8025`

### 🔑 Credenciais Padrão (Ambiente de Desenvolvimento)
Para facilitar o primeiro acesso no ambiente local, o sistema pré-configura as seguintes credenciais:

- **Usuário Admin:** `admin@admin.com` (ou o valor definido em `ADMIN_EMAIL` no `.env`)
- **Senha Admin:** `admin123` (ou o valor definido em `ADMIN_PASSWORD` no `.env`)

---

## 🔒 Segurança e Boas Práticas

### Uso de Valores Padrão em Variáveis de Ambiente
No arquivo `docker-compose.yml`, você notará o uso do padrão `${VAR:-default}`. 

- **Por que usar?** Isso facilita o "Ready to Run". Um novo desenvolvedor pode subir o projeto sem configurar nada e ele funcionará imediatamente com as credenciais acima.
- **Risco em Produção:** Manter senhas padrão como `admin123` ou `quarkus` (banco de dados) em um servidor exposto é uma vulnerabilidade crítica. 
- **Recomendação:** Em ambientes produtivos, **nunca** utilize os valores padrão. Remova os valores após o `:-` no seu arquivo de deploy ou garanta que o arquivo `.env` contenha senhas fortes e únicas.

### Chaves JWT
O projeto utiliza chaves RSA para assinatura de tokens. O script `backend/generate-keys.sh` é executado automaticamente no primeiro boot (se as chaves não existirem) ou pode ser rodado manualmente. As chaves são armazenadas em `backend/src/main/resources/jwt/` e estão no `.gitignore` para evitar vazamentos acidentais.

---

## 🧪 Estratégia de Testes

- **Backend:** Testes de integração com **JUnit 5** e **Testcontainers** (banco real no teste).
- **Frontend:** Testes unitários com **Karma** e **Jasmine**.

---
**Desenvolvido como um Showcase Técnico por Gabriel Silva**  
*Foco em Engenharia de Software, IA Generativa e Soluções para RH.*
