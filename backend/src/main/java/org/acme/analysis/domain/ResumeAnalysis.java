package org.acme.analysis.domain;

import java.util.List;

public record ResumeAnalysis(
    Object matchScore,
    String analiseGeral,
    List<IdentifiedSkill> skillsIdentificadas,
    List<String> gapsDeCompetencia
) {
    public record IdentifiedSkill(
        String skillRequisitadaNaVaga,
        String skillEncontradaNoCurriculo,
        String trechoComprovatorioNoCurriculo
    ) {}

    public Integer getNormalizedScore() {
        if (matchScore == null) return 0;
        if (matchScore instanceof Number n) {
            double val = n.doubleValue();
            if (val <= 1.0 && val > 0) return (int) (val * 100);
            return (int) val;
        }
        if (matchScore instanceof String s) {
            try {
                double val = Double.parseDouble(s.replace("%", ""));
                if (val <= 1.0 && s.contains(".") && val > 0) return (int) (val * 100);
                return (int) val;
            } catch (Exception e) { return 0; }
        }
        return 0;
    }
}
