package com.wavii;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaviiApplicationTests {

    @Test
    void waviiApplicationConstructorTest() {
        assertDoesNotThrow(WaviiApplication::new);
    }

    @Test
    void mainMethodInvokesSpringApplicationRunTest() {
        ConfigurableApplicationContext ctx = mock(ConfigurableApplicationContext.class);
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(WaviiApplication.class, new String[]{}))
                    .thenReturn(ctx);
            WaviiApplication.main(new String[]{});
            mocked.verify(() -> SpringApplication.run(WaviiApplication.class, new String[]{}));
        }
    }
}
