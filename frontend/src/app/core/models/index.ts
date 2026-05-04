export interface User {
  email: string;
  role: string;
  status: 'UNVERIFIED' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';
}

export interface AnalysisJob {
  id: string;
  resumeId: string;
  jobDescription: string;
  company: string;
  role: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  resultJson?: string;
  matchScore?: number;
  originalFileName: string;
}

export interface ResumeAnalysis {
  matchScore: number | string;
  analiseGeral: string;
  skillsIdentificadas: IdentifiedSkill[];
  gapsDeCompetencia: string[];
}

export interface IdentifiedSkill {
  skillRequisitadaNaVaga: string;
  skillEncontradaNoCurriculo: string;
  trechoComprovatorioNoCurriculo: string;
}
