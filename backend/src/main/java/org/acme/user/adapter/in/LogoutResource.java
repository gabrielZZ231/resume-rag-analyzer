package org.acme.user.adapter.in;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/logout")
public class LogoutResource {

    @GET
    public Response logout(@QueryParam("reason") String reason) {
        String redirect;
        if ("rejected".equals(reason)) {
            redirect = "/login.html?rejected=true";
        } else {
            
            redirect = "/login.html";
        }

        
        return Response.seeOther(URI.create(redirect))
                .cookie(new NewCookie.Builder("quarkus-credential")
                        .maxAge(0)
                        .path("/")
                        .build())
                .build();
    }
}
