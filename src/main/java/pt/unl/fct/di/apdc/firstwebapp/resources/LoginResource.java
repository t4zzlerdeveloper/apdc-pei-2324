package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.gson.Gson;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

    private final Gson g = new Gson();
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    public LoginResource(){ }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginData data){
        LOG.fine("Attempt to login user: " + data.username);

        if(data.username.equals("msn1000") && data.password.equals("pwd")){
            AuthToken at = new AuthToken(data.username);
            return Response.ok(g.toJson(at)).build();
        }

        return Response.status(Response.Status.FORBIDDEN).entity("Incorrect username or password.").build();
    }

    @GET
    @Path("/{username}")
    public Response checkUsernameAvailable(@PathParam("username") String username) {
        if(username.equals("msn1000")) {
            return Response.ok().entity(g.toJson(false)).build();
        } else {
            return Response.ok().entity(g.toJson(true)).build();
        }
    }
}
