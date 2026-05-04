package org.acme.notification.adapter.in;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.notification.application.NotificationService;
import org.acme.notification.domain.Notification;
import java.util.List;

@Path("/notifications")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationResource {

    @Inject
    NotificationService notificationService;

    @GET
    public List<Notification> list() {
        return notificationService.list();
    }

    @GET
    @Path("/unread-count")
    @Produces(MediaType.TEXT_PLAIN)
    public long unreadCount() {
        return notificationService.countUnread();
    }

    @POST
    @Path("/{id}/read")
    @Transactional
    public Response markAsRead(@PathParam("id") Long id) {
        notificationService.markAsRead(id);
        return Response.ok().build();
    }

    @POST
    @Path("/read-all")
    @Transactional
    public Response markAllAsRead() {
        notificationService.markAllAsRead();
        return Response.ok().build();
    }
}
