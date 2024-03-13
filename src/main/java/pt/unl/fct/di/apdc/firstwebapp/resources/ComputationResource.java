package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


@Path("/utils")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8") 
public class ComputationResource {

	private static final Logger LOG = Logger.getLogger(ComputationResource.class.getName()); 
	private final Gson g = new Gson();

	private static final DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

	public ComputationResource() {} //nothing to be done here @GET

	@GET
	@Path("/hello")
	@Produces(MediaType.TEXT_PLAIN)
	public Response hello() throws IOException {
		//test 500 error
		throw new IOException("UPS");
//		LOG.fine("Saying hello!!");
//		return Response.ok().entity("Hello apdc-pei-2324 class! I hope you are having a fine day.").build();
	}
	
	@GET
	@Path("/time")
	public Response getCurrentTime() {

		LOG.fine("Replying to date request.");
		return Response.ok().entity(g.toJson(fmt.format(new Date()))).build();
	}


	//@PathParam para parametros do tipo /{id}
	@GET
	@Path("/calc")
	public Response computeResult(@QueryParam("num1") int num1,@QueryParam("num2") int num2){

		int result = num1 + num2;
		String jsonString = "{" +
				"\"num1\":"+String.valueOf(num1) +
				",\"num2\":"+String.valueOf(num2) +
				",\"result\":"+String.valueOf(result) +
				"}";

		return Response.ok().entity(jsonString).build();
	}
}
