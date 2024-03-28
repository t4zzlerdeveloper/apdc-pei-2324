package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.List;

public class PaginatedSessions {

    public List<Session> sessions;
    public String cursor;

    public PaginatedSessions(){

    }

    public PaginatedSessions(List<Session> sessions, String cursor){
        this.sessions = sessions;
        this.cursor = cursor;
    }
}
