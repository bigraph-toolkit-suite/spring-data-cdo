package de.tudresden.inf.st.spring.data.cdo;

import org.springframework.dao.InvalidDataAccessApiUsageException;

public class InvalidCdoApiUsageException extends InvalidDataAccessApiUsageException {
    public InvalidCdoApiUsageException(String msg) {
        super(msg);
    }

    public InvalidCdoApiUsageException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
