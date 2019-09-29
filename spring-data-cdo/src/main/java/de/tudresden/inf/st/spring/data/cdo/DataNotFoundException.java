package de.tudresden.inf.st.spring.data.cdo;

import org.springframework.dao.DataAccessException;

/**
 * This exception is thrown if certain expected data could not be retrieved in the storage, e.g.
 * when looking up specific data via a known identifier.
 * <p>
 * A possible reason might be that the data to be retrieved doesn't longer exist in the storage because it was removed
 * within another transaction or session.
 *
 * @author Dominik Grzelak
 */
public class DataNotFoundException extends DataAccessException {

    public DataNotFoundException(String msg) {
        super(msg);
    }

    public DataNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
