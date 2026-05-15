package com.wavii.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    @Test
    void asyncConfigInstantiationDoesNotThrowTest() {
        assertDoesNotThrow(AsyncConfig::new);
    }

    @Test
    void asyncConfigIsInstanceOfAsyncConfigTest() {
        AsyncConfig config = new AsyncConfig();
        assertNotNull(config);
        assertInstanceOf(AsyncConfig.class, config);
    }
}
