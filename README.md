# Resume RAG Analyzer 🚀 | Fullstack AI Platform

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.15-FF0000?style=for-the-badge&logo=quarkus&logoColor=white)](https://quarkus.io/)
[![Angular](https://img.shields.io/badge/Angular-18-DD0031?style=for-the-badge&logo=angular&logoColor=white)](https://angular.dev/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-AI-green?style=for-the-badge)](https://docs.langchain4j.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-PGVector-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)

Plataforma **Enterprise-Grade** de análise inteligente de currículos que utiliza **IA Generativa** e **RAG (Retrieval-Augmented Generation)**. O projeto foi arquitetado para demonstrar competências de nível, focando em escalabilidade, desacoplamento e tecnologias de ponta.

---

## 🏗️ Arquitetura e Design Patterns

O projeto foi construído sobre pilares sólidos de engenharia de software para garantir manutenibilidade e extensibilidade:

### 🔹 Backend (Hexagonal Architecture / Ports & Adapters)
Implementado com **Java 17** e **Quarkus**, o sistema utiliza uma clara separação de camadas:
- **Domain:** Modelos de domínio ricos e Records Java para imutabilidade.
- **Application:** Serviços que orquestram casos de uso, totalmente desacoplados de frameworks de entrada/saída.
- **Adapters (In/Out):** Implementações específicas para REST (JAX-RS), Persistência (Hibernate/Panache) e Mensageria (Vert.x EventBus).
- **Event-Driven:** Processamento assíncrono de currículos utilizando o EventBus do Vert.x para não bloquear o fluxo principal.

### 🔹 Frontend (Modern Angular Architecture)
Desenvolvido com **Angular 18**, aplicando as melhores práticas da comunidade:
- **Signals:** Gerenciamento de estado reativo de alta performance, eliminando a necessidade de Zone.js em muitos cenários.
- **Standalone Components:** Arquitetura modular sem a complexidade de NgModules.
- **Core/Shared/Features:** Estrutura de pastas escalável para grandes aplicações.
- **Reactive UI:** Interface construída com **Tailwind CSS** e atualizações em tempo real via **Server-Sent Events (SSE)**.

---

## 🛠️ Stack Tecnológica

### Backend
- **Quarkus 3.15 LTS:** Java nativo para nuvem com baixo consumo de memória.
- **LangChain4j:** Integração robusta com LLMs (OpenAI, Gemini, Groq).
- **Hibernate Reactive / Panache:** Persistência de dados simplificada.
- **PGVector:** Banco de dados vetorial para busca semântica eficiente.
- **Mailpit:** Mock de servidor SMTP para testes de fluxo de e-mail.

### Frontend
- **Angular 18:** Framework SPA com foco em performance.
- **RxJS:** Programação reativa para fluxos de dados complexos.
- **Signals API:** Novo paradigma de reatividade do Angular.
- **Tailwind CSS:** Design responsivo e moderno.
- **Chart.js:** Visualização de dados das análises.

---

## 🤖 Engine de Inteligência Artificial (RAG)

O diferencial deste projeto é o uso de **RAG**, que permite à IA analisar o currículo com base em fatos extraídos, reduzindo alucinações.

- **Processamento:** Extração de texto de PDF e DOCX (Apache PDFBox / POI).
- **Embeddings:** Transformação de texto em vetores numéricos armazenados no PGVector.
- **Multi-LLM:** Suporte configurável via `.env` para:
  - **OpenAI (GPT-4o)**
  - **Google Gemini (1.5 Flash)**
  - **Groq (Llama 3)**

---

## 🚀 Como Executar (Docker First)

O projeto está totalmente conteinerizado para facilitar o setup inicial.

### Pré-requisitos
- Docker e Docker Compose instalados.
- Uma API Key de um provedor de IA (OpenAI, Groq ou Gemini).

### Passo a Passo
1. Clone o repositório.
2. Configure as variáveis de ambiente:
   ```bash
   cp .env.example .env
   # Edite o .env com sua AI_API_KEY e configurações desejadas
   ```
3. Suba todos os serviços:
   ```bash
   docker compose up -d --build
   ```

### URLs de Acesso
- **Frontend:** `http://localhost:8081`
- **Backend API:** `http://localhost:8080` (Documentação via `/q/swagger-ui`)
- **Mailpit (Web UI):** `http://localhost:8025`

---

## 🧪 Estratégia de Testes

Qualidade é prioridade. O projeto conta com testes automatizados em ambas as frentes:

### Backend (JUnit 5 & RestAssured)
Foco em testes de integração para garantir que os fluxos de API e persistência funcionem. 
**Nota:** É necessário que o Docker esteja em execução, pois o projeto utiliza **Testcontainers (Dev Services)** para subir um banco de dados real durante os testes.
```bash
cd backend && ./mvnw test
```

### Frontend (Karma & Jasmine)
Testes unitários para componentes e interceptors:
```bash
cd frontend && npm test
```

---

## 🛡️ Segurança e Proteção de Dados

A segurança foi tratada como um cidadão de primeira classe no desenvolvimento desta plataforma:

### 🔐 Autenticação e Autorização
- **Stateless JWT (JSON Web Tokens):** Implementação de autenticação via JWT para garantir escalabilidade e segurança nas comunicações entre Frontend e Backend.
- **Assinatura Assimétrica (RSA):** O backend utiliza chaves privadas/públicas (`.pem`) para assinar e validar tokens, seguindo os padrões mais rigorosos de segurança.
- **RBAC (Role-Based Access Control):** Diferenciação clara entre permissões de `USER` e `ADMIN` via anotações `@RolesAllowed` no Quarkus.
- **Fluxo de Aprovação:** Novos usuários passam por um estado de `PENDING_APPROVAL`, garantindo que apenas pessoas autorizadas acessem as funcionalidades de IA.

### 🛡️ Proteção da API
- **CORS Policy:** Configuração restrita de Cross-Origin Resource Sharing, permitindo apenas origens confiáveis.
- **Tratamento de Exceções Global:** Camada de segurança que evita o vazamento de stack traces internos para o cliente através de um `GlobalExceptionMapper`.
- **Validação de Inputs:** Uso de Bean Validation (JSR 380) para garantir que dados maliciosos não cheguem às camadas de serviço.

### 🤐 Gestão de Segredos
- **Environment Variables:** Nenhuma credencial ou chave de API (OpenAI/Gemini) está "hardcoded". Tudo é injetado via variáveis de ambiente.
- **Ignore Patterns Profissionais:** Arquivos `.gitignore` e `.dockerignore` rigorosamente configurados para impedir o vazamento acidental de chaves privadas, logs ou arquivos `.env` para o controle de versão.

---

## 🎯 Funcionalidades em Destaque
- ✅ **Análise Multinível:** Diferenciação técnica entre linguagens, frameworks e infraestrutura.
- ✅ **Dashboard de Candidatos:** Comparativo visual de Match Score.
- ✅ **Relatórios PDF:** Geração de feedback qualitativo exportável.
- ✅ **Sistema de Aprovação:** Fluxo de segurança para novos usuários (Auth & Admin).

---
**Desenvolvido como um Showcase Técnico por Gabriel Silva**  
*Foco em Engenharia de Software, IA Generativa e Clean Code.*
