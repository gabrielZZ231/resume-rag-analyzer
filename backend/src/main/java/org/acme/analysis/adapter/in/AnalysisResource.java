package org.acme.analysis.adapter.in;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.MultipartForm;
import jakarta.ws.rs.PathParam;
import org.acme.analysis.adapter.in.dto.FileUploadForm;
import org.acme.analysis.application.AnalysisService;
import org.acme.analysis.domain.AnalysisJob;
import java.util.UUID;
import java.util.List;

@Path("/analyze")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalysisResource {

    @Inject
    AnalysisService analysisService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response startAnalysis(@MultipartForm FileUploadForm form) {
        List<AnalysisJob> jobs = analysisService.startAnalysis(form);
        return Response.accepted(jobs).build();
    }

    @GET
    @Path("/{id}")
    public Response getAnalysisStatus(@PathParam("id") UUID id) {
        AnalysisJob job = analysisService.getById(id);
        return Response.ok(job).build();
    }
}
