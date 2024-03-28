package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import com.password4j.BcryptFunction;
import com.password4j.Hash;
import com.password4j.Password;
import com.password4j.types.Bcrypt;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.logging.Logger;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {

    private final Gson g = new Gson();
    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    BcryptFunction bcrypt = BcryptFunction.getInstance(Bcrypt.B, 12);

    public RegisterResource(){ }

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doRegister(LoginData data){

        Key userKey = datastore.newKeyFactory().setKind("Person").newKey(data.username);

        Entity user = datastore.get(userKey);

        if(user != null){
            return Response.status(Response.Status.BAD_REQUEST).entity("That username is already taken!").build();
        }

        Hash hash = Password.hash(data.password)
                .addPepper("shared-secret")
                .with(bcrypt);

        Entity person = Entity.newBuilder(userKey)
                .set("password",hash.getResult())
                .set("createdAt",System.currentTimeMillis())
                .build();

        datastore.put(person);

        return Response.ok("Successfully created new account with username: " + data.username).build();

    }

    @POST
    @Path("/v2")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doRegister2(RegisterData data){

        if(data.username.length() < 3 || data.username.contains(" ")){
            return bad("Invalid username! (Must be at least 3 chars long and not contain spaces)");
        }

        if(data.name.length() < 2){
            return bad("Invalid name! (Must be at least 2 chars long)");
        }

        //limite de 10mb para cada transaçao
        //consultas, escritas e remocoes não ficam visiveis dentro da mesma transaçao
        //ex: criar novo user não fica acessivel logo, so no commit()
        // (é como se o codigo executasse atomicamente/odo no fim)
        //Existem queiries que sao apenas de leitura, devemos usa las quando possivel porque sao mais leves
        Transaction txn = datastore.newTransaction(); // muito mais lento e com grande custo - mas garante consistencia
        Key userKey = datastore.newKeyFactory().setKind("Person").newKey(data.username);
        try{

            Entity user = txn.get(userKey);

            if(user != null){
                return bad("That username is already taken!");
            }

            String regexEmail = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
            if(!data.email.matches(regexEmail)){
                return bad("Invalid email provided!");
            }

            if(!data.password.equals(data.confirmation)){
                return bad("Error, password and confirmation does not match!");
            }

            String regexPassword = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[_@#$%^&+=])(?=\\S+$).{8,}";
            if(!data.password.matches(regexPassword)){
                return bad("Invalid password provided! (Must meet secure password metrics)");
            }

            Hash hash = Password.hash(data.password)
                    .addPepper("shared-secret")
                    .with(bcrypt);

            long initLogins = 0;
            Entity person = Entity.newBuilder(userKey)
                    .set("password",hash.getResult())
                    .set("email",data.email)
                    .set("name",data.name)
                    .set("createdAt", System.currentTimeMillis())
                    .set("successful_logins",initLogins)
                    .set("failed_logins",initLogins)
                    .build();

            txn.add(person);
            txn.commit(); //confirmar transacao

            return Response.ok("Successfully created new account with username: " + data.username).build();
        }
        finally {
            //nao usamos catch porque o servidor pode nao emitir excecoes
            // mas houve algum problema a comunicar esta info com o cliente
            if(txn.isActive()){
                txn.rollback();
                return bad("That username is already taken!");
            }
        }

    }

    private Response bad(String response){
        return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }
}
