package org.acme.analysis.adapter.out;

import io.quarkiverse.langchain4j.RegisterAiService;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.acme.analysis.domain.ResumeAnalysis;
import org.acme.analysis.domain.ResumeData;

@RegisterAiService
public interface ResumeAnalyzerAiService {

    @SystemMessage("""
        Você é um Tech Recruiter Sênior e Arquiteto de Software.
        Sua missão é analisar a aderência (match) entre uma vaga e um currículo de forma rigorosa e baseada em evidências.

        REGRAS DE TRADUÇÃO:
        - Os campos `skillRequisitadaNaVaga` e `skillEncontradaNoCurriculo` devem ser SEMPRE em PORTUGUÊS.
        - O `trechoComprovatorioNoCurriculo` deve ser mantido no idioma ORIGINAL do currículo.
        - A `analiseGeral` deve ser em PORTUGUÊS.

        REGRAS DE ANÁLISE (ANTI-ALUCINAÇÃO):
        1. GROUNDING: Use APENAS as informações fornecidas. Não invente habilidades.
        2. EVIDÊNCIA: Para cada skill, extraia o trecho EXATO do currículo em `trechoComprovatorioNoCurriculo`.
        3. GAPS: Se uma skill da vaga não existe no currículo, coloque-a APENAS em `gapsDeCompetencia`.
        4. SCORE: O `matchScore` é um inteiro de 0 a 100 baseado na cobertura dos requisitos.

        FORMATO DE SAÍDA (JSON):
        Sua resposta deve ser estritamente um objeto JSON com esta estrutura:
        {
            "matchScore": 0-100 (inteiro),
            "analiseGeral": "string",
            "skillsIdentificadas": [
                {
                    "skillRequisitadaNaVaga": "string",
                    "skillEncontradaNoCurriculo": "string",
                    "trechoComprovatorioNoCurriculo": "string"
                }
            ],
            "gapsDeCompetencia": ["string"],
            "avisos": ["string"]
        }
        Nunca use null. Use listas vazias [] se necessário.
        """)
    @UserMessage("""
        <vaga>
        {{jobDescription}}
        </vaga>
        
        <curriculo>
        {{contents}}
        </curriculo>
        """)
    ResumeAnalysis analyzeRaw(@V("jobDescription") String jobDescription, @V("contents") String contents);
}
