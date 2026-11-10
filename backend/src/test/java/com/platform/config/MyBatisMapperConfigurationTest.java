package com.platform.config;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MyBatisMapperConfigurationTest {

    @Test
    void allMapperXmlFilesLoadWithApplicationTypeHandlers() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.getTypeHandlerRegistry().register("com.platform.reservation");

        Resource[] mapperResources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/**/*.xml");

        assertFalse(mapperResources.length == 0, "MyBatis mapper XML files must be present");
        for (Resource mapperResource : mapperResources) {
            try (InputStream input = mapperResource.getInputStream()) {
                new XMLMapperBuilder(input, configuration, mapperResource.toString(),
                        configuration.getSqlFragments()).parse();
            }
        }
    }
}
