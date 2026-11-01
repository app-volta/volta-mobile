# VOLTA — Mapeamento Oficial de Endpoints de IA, Autenticação e Mídia (QA)

Este documento mapeia todos os endpoints utilizados para conectar o **Chatbot de IA (`AssistantFragment`)** e a **Análise de Imagem por IA (`NewOccurrenceFragment` -> `CloudinaryUploader` -> `AiAnalysisFragment`)** no ambiente de QA.

---

## 1. Ambientes e Base URLs

| Serviço | Base URL | Cliente Android | Autenticação |
| :--- | :--- | :--- | :--- |
| **Core API & Auth (QA)** | `https://api.qa.54.210.1.34.sslip.io` | `ChatbotAuthClient` | `Authorization: Bearer <JWT_TOKEN>` |
| **Chatbot IA (QA)** | `https://chat.qa.54.210.1.34.sslip.io` | `ChatbotClient` | `Authorization: Bearer <JWT_TOKEN>` |
| **Cloudinary (Upload de Imagem)** | `https://api.cloudinary.com/v1_1/kgou1air` | `CloudinaryUploader` | Unsigned Upload (`upload_preset = "AI-Analysis"`) |

---

## 2. Autenticação JWT (`AuthAPI`)

| Método | Endpoint | Headers | Request Body | Response Body | Descrição |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `{api}/auth/login` | `Content-Type: application/json; charset=utf-8` | `{"email": "breno@volta.com", "password": "Volta@2026"}` *(EMPLOYEE)* ou `{"email": "volta.brasil.2026@gmail.com", "password": "Volta@2026"}` *(ADMIN)* | `{"token": "eyJhbG...", "expiresIn": 86400000}` | Autentica o operador e retorna o token JWT enviado no header `Authorization: Bearer <TOKEN>` de todas as chamadas subsequentes. Aquisição preguiçosa no 1º request e renovação automática em caso de HTTP `401`. |

---

## 3. Endpoints do Chatbot de IA (`ChatbotAPI` -> `AssistantFragment`)

| Método | Endpoint | Request Body / Path Params | Response Body | Descrição no App (`AssistantFragment`) |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `{chatbot}/v1/sessions` | *(Sem corpo)* | `{"session_id": "uuid", "created_at": "..."}` | Cria uma nova sessão de conversa com o assistente VOLTA e armazena o `session_id` no `SessionManager`. |
| `POST` | `{chatbot}/v1/chat` | `{"session_id": "ID_DA_SESSAO", "message": "Mensagem do usuário"}` | `{"session_id": "...", "response": {"answer": "...", "citations": [...], "recommended_actions": [...]}, "requires_human_validation": false}` | Envia a dúvida do operador e renderiza `response.answer`, `citations`, chips clicáveis de `recommended_actions` e badge `requires_human_validation` preservando o visual do Figma `790:1911`. |
| `GET` | `{chatbot}/v1/sessions/{session_id}/history` | Path: `session_id` | Lista de mensagens (`role`, `content`, `created_at`) | Recupera o histórico de mensagens da sessão ativa ao abrir a tela do assistente. |
| `POST` | `{chatbot}/v1/sessions/{session_id}/close` | Path: `session_id` | `{"status": "closed"}` | Encerra a sessão ativa e solicita a indexação do resumo da conversa. |

---

## 4. Pipeline de Upload e Endpoints de Análise de Imagem por IA (`CloudinaryUploader` + `AiAnalysisAPI` -> `AiAnalysisFragment`)

### 4.1. Upload da Foto ao Clicar em "Enviar para análise"
| Método | Endpoint | Parâmetros (`multipart/form-data`) | Response Body | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `https://api.cloudinary.com/v1_1/kgou1air/image/upload` | `upload_preset`: `"AI-Analysis"`<br>`folder`: `"Ocorrencia/Assets/Images"`<br>`file`: binário JPEG comprimido | `{"secure_url": "https://res.cloudinary.com/kgou1air/image/upload/...", "public_id": "..."}` | Disparado imediatamente no clique do botão **"Enviar para análise"** (`NewOccurrenceFragment`). Gera o link público HTTPS (`secure_url`) que é enviado para a API de IA em `AiAnalysisFragment`. |

### 4.2. Endpoints de Análise da Imagem e Relatório de IA (`AiAnalysisAPI`)
| Método | Endpoint | Request Body / Path Params | Response Body | Descrição no App (`AiAnalysisFragment`) |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `{api}/occurrences/{id}/analysis` | `{"photoUrl": "<secure_url>", "imageUrl": "<secure_url>", "foto_url": "<secure_url>", "setor": "...", "descricao": "..."}` | `AiAnalysisJSON` (`material`, `quantidade_estimada`, `unidade`, `contaminacao`, `unidades`, `confianca`, `observacoes`) | Envia a `secure_url` gerada pelo Cloudinary para análise direta da imagem pela IA. |
| `GET` | `{api}/areas/mine` | *(Sem corpo)* | `AreaResponseDTO[]` (`id`, `companyId`, `sectorName`, `locationDescription`) | Consulta os setores/áreas da empresa autenticada para vincular o `areaId` ao incidente. |
| `GET` | `{api}/waste-types` | *(Sem corpo)* | `WasteTypeResponseDTO[]` (`id`, `category`, `description`, `defaultRiskLevel`, `color`, `wasteClass`) | Consulta o catálogo de tipos de resíduo (CONAMA 275/2001 / NBR 10004) para vincular o `wasteTypeId`. |
| `POST` | `{api}/incidents` | `IncidentRequestDTO`: `{"areaId": "uuid", "wasteTypeId": "uuid", "photoUrl": "<secure_url>", "employeeDescription": "...", "contaminationLevel": "LOW\|MEDIUM\|HIGH", "estimatedQuantity": 38.0, "priority": "LOW\|MEDIUM\|HIGH\|CRITICAL"}` | `IncidentResponseDTO` (`id`, `photoUrl`, `status`, `registeredAt`, ...) | Registra o incidente enviando a `photoUrl` (`secure_url` do Cloudinary) para processamento pela IA no backend QA. |
| `GET` | `{api}/incidents/{id}/ai-report` | Path: `id` (UUID do incidente) | `AiReportResponseDTO`: `{"id": "uuid", "incidentId": "uuid", "detectedWasteType": "...", "aiContaminationLevel": "LOW\|MEDIUM\|HIGH", "recommendations": "...", "reportText": "...", "generatedAt": "..."}` | Retorna o relatório gerado pela IA para o incidente/imagem, convertido via `AiAnalysisJSON.fromAiReport(...)` para popular a tela de Análise da IA (Figma `851:3792`). |
| `GET` | `{api}/incidents/{id}/scoring-context` | Path: `id` (UUID do incidente) | `IncidentScoringContextDTO` (`incidentId`, `priority`, `wasteType`, `contaminationLevel`, `origin`, `aiReport`) | Retorna o contexto da ocorrência e resumo do relatório da IA usado no cálculo de compatibilidade (AIC) de cooperativas. |

---

## 5. Endpoints de Predição e Visão Computacional por IA (`ChatbotAPI` -> `AiAnalysisFragment`)

| Método | Endpoint | Headers / Params | Request Body | Response Body | Descrição no App (`AiAnalysisFragment`) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `{chatbot}/v1/occurrences/predict` | `Content-Type: multipart/form-data`<br>`Authorization: Bearer <TOKEN>` | `file`: Binário da imagem capturada (`image/jpeg` ou `image/png`) | `AnaliseResiduoIA`: `{"detected_waste_type": "...", "ai_contamination_level": "BAIXO\|MEDIO\|ALTO", "estimated_quantity_kg": 38.0, "recommendations": "...", "report_text": "...", "mobile_summary": "...", "requires_human_validation": true, "analysis_id": "uuid", "provenance_token": "..."}` | **Endpoint Oficial de Visão de IA (Gemini Vision + Judge LLM)**. Recebe a foto capturada pelo funcionário e executa análise profunda de tipo de resíduo, contaminação visual, volume e gera recomendações com token de proveniência criptográfica. |
| `POST` | `{chatbot}/v1/occurrences/drafts` | `Content-Type: application/json`<br>`Authorization: Bearer <TOKEN>` | `{"area_id": "uuid", "employee_description": "...", "priority": "MEDIA", "ai_data": <AnaliseResiduoIA>}` | `{"draft_id": "uuid", "status": "AGUARDANDO_VALIDACAO"}` | Grava o rascunho de ocorrência auditado com os dados e proveniência gerados pelo Gemini. |
| `POST` | `{chatbot}/v1/occurrences/drafts/{draft_id}/approve` | `Content-Type: application/json`<br>`Authorization: Bearer <TOKEN>` | *(Sem corpo)* | `{"status": "APROVADO", "incident_id": "uuid"}` | Homologa a ocorrência no banco transacional pelo responsável técnico da planta (compliance). |
