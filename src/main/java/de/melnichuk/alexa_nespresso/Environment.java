package de.melnichuk.alexa_nespresso;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Environment {
    private static final Logger LOGGER = LoggerFactory.getLogger(Environment.class);
    private static final Environment instance = new Environment();

    private final String username;
    private final String password;

    private Environment() {
        this.username = System.getenv("nespresso_username");
        this.password = System.getenv("nespresso_password");

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("ENV: \n{}", System.getenv());
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }


    public static Environment getInstance() {
        return instance;
    }
}
