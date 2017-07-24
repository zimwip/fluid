package kafka.config;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Neo4jConfiguration.class);

    @Autowired
    private Neo4jProperties config;

    @Bean(name = "neo4jDriver")
    public Driver getDriver() {
        LOGGER.info("Initialize Neo4j Driver");
        return GraphDatabase.driver(config.getUrl(), AuthTokens.basic(config.getUser(), config.getPwd()));
    }

}



