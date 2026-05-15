package com.wavii;

import com.wavii.config.CustomUserDetailsService;
import com.wavii.config.JwtAuthFilter;
import com.wavii.repository.UserRepository;
import com.wavii.repository.VerificationRequestRepository;
import com.wavii.repository.VerificationTokenRepository;
import com.wavii.service.BandListingService;
import com.wavii.service.PdfStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class WaviiApplicationTests {

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private BandListingService bandListingService;

    @MockBean
    private PdfStorageService pdfStorageService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private VerificationTokenRepository verificationTokenRepository;

    @MockBean
    private VerificationRequestRepository verificationRequestRepository;

    // @Test
    // void contextLoads() {
    //     // Context loads successfully, covering SecurityConfig and other beans.
    // }
}
