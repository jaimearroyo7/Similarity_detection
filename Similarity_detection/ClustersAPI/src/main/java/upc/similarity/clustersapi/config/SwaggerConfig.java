package upc.similarity.clustersapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import upc.similarity.clustersapi.RestApiController;

@Configuration
@PropertySource("classpath:swagger.properties")
@ComponentScan(basePackageClasses = RestApiController.class)
@EnableSwagger2
public class SwaggerConfig {

    private static final String	LICENSE_TEXT	    = "License";
    private static final String	title		    = "CLUSTERS COMPONENT";
    private static final String	description	    =
            "<p>The component is based in tf-idf numerical statistic. The aim of the API is to group the entry requirements in clusters which elements have a high similarity."+
            "</p>" +
            "<p>There are three operations: </p>" +
            "<ul>" +
            "<li>BuildModel: Builds a model with the input requirements</li>" +
            "<li>ComputeClusters: Generates a set of clusters with the input requirements</li>" +
            "<li>ClearDatabase: Deletes all data from the database</li></ul>" +
            "<p>The API uses UTF-8 charset.</p>";

    /**
     * API Documentation Generation.
     * @return
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(false)
                .apiInfo(apiInfo())
                .pathMapping("/")
                .select()
                .paths(PathSelectors.any())
                .apis(RequestHandlerSelectors.basePackage("upc.similarity.clustersapi")).paths(PathSelectors.regex("/upc.*"))
                .build().tags(new Tag("Clusters Service", ""));
    }
    /**
     * Informtion that appear in the API Documentation Head.
     *
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title(title).description(description).license(LICENSE_TEXT)
                .contact(new Contact("UPC-GESSI (OPENReq)", "http://openreq.eu/", ""))
                .build();
    }
}