package kafka.config;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Neo4jConfiguration.class);

    // @Value("${neo4j.url}")
    private String url = "bolt://localhost:7687";
    // @Value("${neo4j.user}")
    private String user = "neo4j";
    // @Value("${neo4j.pwd}")
    private String pwd = "toto";

    @Bean(name = "neo4jDriver")
    public Driver getDriver() {
        LOGGER.info("Initialize Neo4j Driver");
        return GraphDatabase.driver(url, AuthTokens.basic(user, pwd));
    }

}
