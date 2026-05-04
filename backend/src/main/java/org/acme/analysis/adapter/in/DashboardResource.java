package org.acme.analysis.adapter.in;

import jakarta.inject.Inject;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.transaction.Transactional;
import org.acme.analysis.adapter.out.AnalysisEventEmitter;
import org.acme.analysis.application.PdfReportService;
import org.acme.analysis.domain.AnalysisJob;
import org.acme.user.domain.User;
import org.acme.analysis.adapter.in.dto.AnalysisDashboardDTO;
import org.jboss.resteasy.reactive.RestStreamElementType;
import io.smallrye.mutiny.Multi;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Path("/dashboard")
@RolesAllowed({"USER", "ADMIN"})
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    AnalysisEventEmitter eventEmitter;

    @GET
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> streamUpdates() {
        return eventEmitter.getUpdates();
    }

    @GET
    @Path("/me")
    public User getCurrentUser() {
        return User.findByEmail(securityIdentity.getPrincipal().getName());
    }

    @DELETE
    @Path("/jobs")
    @Transactional
    public Response deleteAnalysisGroup(@QueryParam("company") String company, @QueryParam("role") String role) {
        if (company == null || role == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        long deleted = AnalysisJob.delete("company = ?1 and role = ?2", company, role);
        return Response.ok(Map.of("deleted", deleted)).build();
    }

    @Inject
    PdfReportService pdfReportService;

    @GET
    @Path("/report")
    @Produces("application/pdf")
    public Response exportReport(@QueryParam("company") String company, @QueryParam("role") String role) {
        if (company == null || role == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        List<AnalysisJob> jobs = AnalysisJob.list("company = ?1 and role = ?2 order by matchScore desc", company, role);
        if (jobs.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        byte[] pdf = pdfReportService.generateReport(company, role, jobs);
        
        String fileName = String.format("Relatorio_%s_%s.pdf", 
            company.replaceAll("\\s+", "_"), 
            role.replaceAll("\\s+", "_"));

        return Response.ok(pdf)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
    }

    @GET
    @Path("/jobs")
    public Map<String, Map<String, List<AnalysisDashboardDTO>>> getHierarchicalJobs() {
        
        List<AnalysisJob> allJobs = AnalysisJob.list("order by createdAt desc");
        
        return allJobs.stream()
            .collect(Collectors.groupingBy(
                job -> (job.company == null || job.company.isBlank()) ? "Empresa não informada" : job.company,
                LinkedHashMap::new, 
                Collectors.groupingBy(
                    job -> (job.role == null || job.role.isBlank()) ? "Cargo não informado" : job.role,
                    LinkedHashMap::new, 
                    Collectors.mapping(AnalysisDashboardDTO::new, Collectors.toList())
                )
            ));
    }
}
