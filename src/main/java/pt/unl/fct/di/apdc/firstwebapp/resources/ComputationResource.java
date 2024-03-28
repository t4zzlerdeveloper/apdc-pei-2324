package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.gson.Gson;
import com.google.protobuf.Timestamp;


@Path("/utils")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8") 
public class ComputationResource {

	private static final Logger LOG = Logger.getLogger(ComputationResource.class.getName());
	private final Gson g = new Gson();

	private static final DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

	public ComputationResource() {
	} //nothing to be done here @GET

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
	public Response computeResult(@QueryParam("num1") int num1, @QueryParam("num2") int num2) {

		int result = num1 + num2;
		String jsonString = "{" +
				"\"num1\":" + String.valueOf(num1) +
				",\"num2\":" + String.valueOf(num2) +
				",\"result\":" + String.valueOf(result) +
				"}";

		return Response.ok().entity(jsonString).build();
	}

	@POST
	@Path("/compute")
	public Response triggerExecuteComputeTask() throws IOException {
		String projectId = "hello-world-415917";
		String queueName = "Default";
		String location = "europe-west6";
		LOG.log(Level.INFO, projectId + " :: " + queueName + " :: " + location);
		try (CloudTasksClient client = CloudTasksClient.create()) {
			String queuePath = QueueName.of(projectId, location,
					queueName).toString();
			Task.Builder taskBuilder =
					Task.newBuilder().setAppEngineHttpRequest(AppEngineHttpRequest.newBuilder()
							.setRelativeUri("/rest/utils/compute").setHttpMethod(HttpMethod.POST)
							.build());
			taskBuilder.setScheduleTime(Timestamp.newBuilder().setSeconds(Instant.now(Clock
					.systemUTC()).getEpochSecond()));
			client.createTask(queuePath, taskBuilder.build());
		}
		return Response.ok().build();
	}
}

