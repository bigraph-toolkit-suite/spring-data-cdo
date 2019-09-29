package de.tudresden.inf.st.spring.data.cdo.config;

/**
 * @author Dominik Grzelak
 */
public class CdoCredentials {

    private final String userName;
    private final char[] password;
    private final String authSource;

    public CdoCredentials(final String userName, final char[] password, final String authSource) {
        this.userName = userName;
        this.password = password != null ? password.clone() : null;
        this.authSource = authSource;
//        assertParamsValid();
    }

//    private void assertParamsValid() {
//        if (userName == null) {
//            throw new IllegalArgumentException("username can not be null");
//        }
//    }

    public String getUserName() {
        return userName;
    }

    public char[] getPassword() {
        return password;
    }

    public String getAuthSource() {
        return authSource;
    }
}
