package com.wavii.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorsConfigTest {

    @Test
    void corsFilterCreatesFilterBeanTest() {
        CorsConfig corsConfig = new CorsConfig();
        CorsFilter filter = corsConfig.corsFilter();
        assertNotNull(filter);
    }
}
