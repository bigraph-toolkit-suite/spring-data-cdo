package org.bigraphs.spring.data.cdo;

/**
 * @author Dominik Grzelak
 */
public class CdoServerSessionPool {
    private volatile boolean closing;
    private volatile boolean closed;

}
