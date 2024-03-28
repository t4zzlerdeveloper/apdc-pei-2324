package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {

    public String username,email,password,confirmation,name;

    public RegisterData(){

    }

    public RegisterData(String username,String email,String password,String confirmation, String name){
        this.username = username;
        this.email = email;
        this.password = password;
        this.confirmation = confirmation;
        this.name = name;
    }
}
