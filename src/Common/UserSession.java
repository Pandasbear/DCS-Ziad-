package Common;

import java.io.Serializable;

public class UserSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String tokenSession;
    private final UserRole role;
    private final String userId;
    private final long createdTime; // in milliseconds

    public UserSession(String tokenSession, UserRole role, String userId){
        this.tokenSession = tokenSession;
        this.role = role;
        this.userId = userId;
        this.createdTime = System.currentTimeMillis();
    }

    public String getTokenSession(){
        return tokenSession;
    }

    public UserRole getRole(){
        return role;
    }

    public String getUserId(){
        return userId;
    }

    public long getCreatedTime(){
        return createdTime;
    }
}
