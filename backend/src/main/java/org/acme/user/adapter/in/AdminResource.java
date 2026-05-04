package org.acme.user.adapter.in;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.user.application.UserAdminService;
import org.acme.user.domain.User;
import java.util.List;

@Path("/admin")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    UserAdminService userAdminService;

    @GET
    @Path("/pending-users")
    public List<User> getPendingUsers() {
        return userAdminService.getPendingUsers();
    }

    @GET
    @Path("/users")
    public List<User> getAllUsers() {
        return userAdminService.getApprovedUsers();
    }

    @POST
    @Path("/approve/{id}")
    public Response approveUser(@PathParam("id") Long id) {
        if (!userAdminService.approveUser(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/reject/{id}")
    public Response rejectUser(@PathParam("id") Long id) {
        if (!userAdminService.rejectUser(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/users/{id}/role")
    public Response updateRole(@PathParam("id") Long id, @QueryParam("role") String role) {
        if (!userAdminService.updateRole(id, role)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("/users/{id}")
    public Response deleteUser(@PathParam("id") Long id) {
        if (!userAdminService.deleteUser(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }
}
