package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import com.password4j.BcryptFunction;
import com.password4j.Password;
import com.password4j.types.Bcrypt;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
import pt.unl.fct.di.apdc.firstwebapp.util.PaginatedSessions;
import pt.unl.fct.di.apdc.firstwebapp.util.Session;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

    public static final long TIME_24H = 1000*60*60*24; //24h

    public static final String CURSOR = "cursor";

    @Context
    private HttpHeaders headers;
    @Context
    private HttpServletRequest request;

    private final Gson g = new Gson();
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    BcryptFunction bcrypt = BcryptFunction.getInstance(Bcrypt.B, 12);

    public LoginResource(){ }

    @POST
    @Path("/v2")
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginData data){

        Transaction txn = datastore.newTransaction();
        Key userKey = datastore.newKeyFactory().setKind("Person").newKey(data.username);
        try{
            Entity user = txn.get(userKey);

            if(user == null){
                return bad("User with the provided username does not exist!");
            }

            String hash = user.getString("password");
            boolean verified = Password.check(data.password, hash)
                    .addPepper("shared-secret")
                    .with(bcrypt);

            if(verified){
                AuthToken at = new AuthToken(data.username);

                Key sessionKey = datastore.newKeyFactory()
                        .addAncestor(PathElement.of("Person", at.username)).
                        setKind("Session").
                        newKey(at.tokenID);


                String country = headers.getHeaderString("X-AppEngine-Country");
                String region = headers.getHeaderString("X-AppEngine-Region");
                String city = headers.getHeaderString("X-AppEngine-City");
                String latLong = headers.getHeaderString("X-Appengine-CityLatLong");

                if (country == null) {
                    country = "Unknown";
                }
                if (region == null) {
                    region = "Unknown";
                }
                if (city == null) {
                    city = "Unknown";
                }
                if (latLong == null) {
                    latLong = "0,0";
                }

                Entity session = Entity.newBuilder(sessionKey)
                        .set("country", country)
                        .set("region", region)
                        .set("city", city)
                        .set("latLong", latLong)
                        .set("ip", request.getRemoteAddr())
                        .set("host", request.getRemoteHost())
                        .set("expirationDate", at.expirationDate)
                        .set("creationDate", at.creationDate)
                        .build();

                txn.add(session);
                txn.commit();
                //addLoginAttempt(user,true);

                return Response.ok(g.toJson(at)).build();
            }

            //addLoginAttempt(user,false);
            return bad("Incorrect password!");
        }
        catch (Exception e){
            txn.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        finally {
            //nao usamos catch porque o servidor pode nao emitir excecoes
            // mas houve algum problema a comunicar esta info com o cliente
            if(txn.isActive()){
                txn.rollback();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        }

    }

    @POST
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLastLogins(LoginData data) {

        //TODO: Adicionar transações (só de leitura)
        //Transaction txn = datastore.newTransaction();
        Key userKey = datastore.newKeyFactory().setKind("Person").newKey(data.username);

        Entity user = datastore.get(userKey);

        if(user == null){
            return bad("User with the provided username does not exist!");
        }

        String hash = user.getString("password");
        boolean verified = Password.check(data.password, hash)
                .addPepper("shared-secret")
                .with(bcrypt);

        if(verified) {

            long last24h = System.currentTimeMillis() - TIME_24H;

            //Existem queries que apenas retornam as chaves, nesse caso metemos  <Key> em vez de<Entity>
            Query<Entity> query =
                    Query.newEntityQueryBuilder()
                            //É possivel fazer queries sem Kind, mas isso implica varrer a bd toda!!!
                            .setKind("Session")
                            .setFilter(
                                    StructuredQuery.CompositeFilter.and(
                                        StructuredQuery.PropertyFilter.ge("creationDate", last24h),
                                        StructuredQuery.PropertyFilter.hasAncestor(userKey)
                                    )
                            )
                            .setOrderBy(StructuredQuery.OrderBy.desc("creationDate"))
                            //.setLimit(n);
                            //Devemos impor sempre o limite
                            .build();


            //Usar Cursors para paginação e evitar reads excessivos!!!!!!!!
            QueryResults<Entity> sessions = datastore.run(query);

            List<Session> sessionsArr = new ArrayList<>();
            //Aqui devemos ter paginação em vez de obter todos os resultados
            while (sessions.hasNext()) {
                Entity entity = sessions.next();
                Session session = new Session(
                        entity.getString("city"),
                        entity.getString("country"),
                        entity.getString("host"),
                        entity.getString("ip"),
                        entity.getString("latLong"),
                        entity.getString("region"),
                        entity.getLong("creationDate"),
                        entity.getLong("expirationDate")
                );

                sessionsArr.add(session);
            }

            return Response.ok().entity(sessionsArr).build();
        }

        return bad("Incorrect password!");

    }

    @POST
    @Path("/user/pagination")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLastLoginsPages(LoginData data, @QueryParam(CURSOR) String cursor) {

        //TODO: Adicionar transações (só de leitura)

        Key userKey = datastore.newKeyFactory().setKind("Person").newKey(data.username);
        Entity user = datastore.get(userKey);

        if(user == null){
            return bad("User with the provided username does not exist!");
        }

        String hash = user.getString("password");
        boolean verified = Password.check(data.password, hash)
                .addPepper("shared-secret")
                .with(bcrypt);

        if(verified) {

            long last24h = System.currentTimeMillis() - TIME_24H;
            Query<Entity> query;
            if(cursor != null){
                query =
                        Query.newEntityQueryBuilder()
                                //É possivel fazer queries sem Kind, mas isso implica varrer a bd toda!!!
                                .setKind("Session")
                                .setFilter(
                                        StructuredQuery.CompositeFilter.and(
                                                StructuredQuery.PropertyFilter.ge("creationDate", last24h),
                                                StructuredQuery.PropertyFilter.hasAncestor(userKey)
                                        )
                                )
                                .setOrderBy(StructuredQuery.OrderBy.desc("creationDate"))
                                .setLimit(3)
                                .setStartCursor(Cursor.fromUrlSafe(cursor))
                                .build();
            }
            else{
                query =
                        Query.newEntityQueryBuilder()
                                //É possivel fazer queries sem Kind, mas isso implica varrer a bd toda!!!
                                .setKind("Session")
                                .setFilter(
                                        StructuredQuery.CompositeFilter.and(
                                                StructuredQuery.PropertyFilter.ge("creationDate", last24h),
                                                StructuredQuery.PropertyFilter.hasAncestor(userKey)
                                        )
                                )
                                .setOrderBy(StructuredQuery.OrderBy.desc("creationDate"))
                                .setLimit(3)
                                .build();
            }



            //Usar Cursors para paginação e evitar reads excessivos!!!!!!!!
            QueryResults<Entity> sessions = datastore.run(query);

            List<Session> sessionsArr = new ArrayList<>();
            //Aqui devemos ter paginação em vez de obter todos os resultados
            while (sessions.hasNext()) {
                Entity entity = sessions.next();
                Session session = new Session(
                        entity.getString("city"),
                        entity.getString("country"),
                        entity.getString("host"),
                        entity.getString("ip"),
                        entity.getString("latLong"),
                        entity.getString("region"),
                        entity.getLong("creationDate"),
                        entity.getLong("expirationDate")
                );

                sessionsArr.add(session);
            }

            String cursorString = sessions.getCursorAfter().toUrlSafe();
            return Response.ok().entity(new PaginatedSessions(sessionsArr,cursorString)).build();
        }

        return bad("Incorrect password!");

    }

    @GET
    @Path("/{username}")
    public Response checkUsernameAvailable(@PathParam("username") String username) {
        Key userKey = datastore.newKeyFactory().setKind("Person").newKey(username);
        Entity user = datastore.get(userKey);

        if(user != null) {
            return Response.ok().entity(g.toJson(false)).build();
        } else {
            return Response.ok().entity(g.toJson(true)).build();
        }
    }

//    private void addLoginAttempt(Entity user,boolean success){
//        long newSucLogins = user.getLong("successful_logins");
//        long newFailLogins = user.getLong("failed_logins");
//        if(success){
//           newSucLogins++;
//        }
//        else{
//           newFailLogins++;
//        }
//
//        Entity newUser = Entity.newBuilder(user.getKey())
//                .set("password",user.getString("password"))
//                .set("email",user.getString("email"))
//                .set("name",user.getString("name"))
//                .set("createdAt",user.getTimestamp("createdAt"))
//                .set("successful_logins",newSucLogins)
//                .set("failed_logins",newFailLogins)
//                .build();
//
//        datastore.put(newUser);
//    }

    private Response bad(String response){
        return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }
}
