package org.acme.analysis.domain;

import java.util.List;

public record ResumeAnalysis(
    Integer matchScore,
    String analiseGeral,
    List<IdentifiedSkill> skillsIdentificadas,
    List<String> gapsDeCompetencia,
    java.util.List<String> avisos
) {
    public record IdentifiedSkill(
        String skillRequisitadaNaVaga,
        String skillEncontradaNoCurriculo,
        String trechoComprovatorioNoCurriculo
    ) {}

    public Integer getNormalizedScore() {
        if (matchScore == null) return 0;
        return matchScore;
    }
}
