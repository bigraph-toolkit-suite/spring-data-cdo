package de.tudresden.inf.st.spring.data.cdo;

import org.springframework.dao.DataAccessException;

/**
 * @author Dominik Grzelak
 */
public class CreateResourceFailedException extends DataAccessException {

    public CreateResourceFailedException(String msg) {
        super(msg);
    }

    public CreateResourceFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
