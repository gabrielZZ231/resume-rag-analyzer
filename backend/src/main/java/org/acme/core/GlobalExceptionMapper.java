package org.acme.core;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Inject
    jakarta.ws.rs.core.UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            LOG.warnf("Recurso não encontrado: %s %s", 
                uriInfo.getRequestUri().getPath(), 
                exception.getMessage());
            return ((jakarta.ws.rs.WebApplicationException) exception).getResponse();
        }

        LOG.error("Unhandled exception caught by GlobalExceptionMapper", exception);

        if (exception instanceof WebApplicationException webAppException) {
            return webAppException.getResponse();
        }

        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", exception.getMessage()))
                    .build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Ocorreu um erro inesperado no servidor. Por favor, tente novamente mais tarde."))
                .build();
    }
}
