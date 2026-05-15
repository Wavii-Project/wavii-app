package com.wavii.controller;

import com.wavii.dto.bulletin.BulletinBoardResponse;
import com.wavii.dto.bulletin.BulletinTeacherResponse;
import com.wavii.dto.bulletin.BulletinUpdateRequest;
import com.wavii.model.User;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import com.wavii.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulletinControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BulletinController bulletinController;

    private User teacher;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(UUID.randomUUID());
        teacher.setName("Prof. Ana");
        teacher.setRole(Role.PROFESOR_CERTIFICADO);
        teacher.setSubscription(Subscription.SCHOLAR);
        teacher.setBio("Profesora de piano");
        teacher.setInstrument("Piano");
        teacher.setPricePerHour(new BigDecimal("30.00"));

        ReflectionTestUtils.setField(bulletinController, "appBaseUrl", "http://localhost:8080");
    }

    // ── getTeachers ──────────────────────────────────────────────────

    @Test
    void getTeachersReturnsFullBoardForScholarUserTest() {
        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Juan");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setSubscription(Subscription.SCHOLAR);
        teacher2.setBio("Profesor de guitarra");
        teacher2.setInstrument("Guitarra");
        teacher2.setPricePerHour(new BigDecimal("25.00"));

        when(userRepository.findByRoleIn(List.of(Role.PROFESOR_PARTICULAR, Role.PROFESOR_CERTIFICADO)))
                .thenReturn(List.of(teacher, teacher2));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, null, null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().hasFullAccess());
        assertTrue(result.getBody().canPublish());
        assertEquals(2, result.getBody().teachers().size());
        assertEquals("Prof. Ana", result.getBody().teachers().get(0).name());
        assertEquals("profesor_certificado", result.getBody().teachers().get(0).role());
    }

    @Test
    void getTeachersLimitsBoardForNonScholarUserTest() {
        User freeUser = new User();
        freeUser.setSubscription(Subscription.FREE);

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher, teacher, teacher));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(freeUser, null, null, null, null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().hasFullAccess());
        assertEquals(3, result.getBody().teachers().size());
        assertEquals(1, result.getBody().hiddenCount());
    }

    @Test
    void getTeachersNullUserHasNoFullAccessTest() {
        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of());

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(null, null, null, null, null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().hasFullAccess());
        assertFalse(result.getBody().canPublish());
        assertEquals(0, result.getBody().hiddenCount());
    }

    @Test
    void getTeachersFiltersByQueryMatchingNameTest() {
        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Carlos");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setBio("Guitarra flamenca");
        teacher2.setInstrument("Guitarra");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher2));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, "carlos", null, null, null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().teachers().size());
        assertEquals("Prof. Carlos", result.getBody().teachers().get(0).name());
    }

    @Test
    void getTeachersFiltersByQueryMatchingInstrumentTest() {
        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Carlos");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setBio("Clases de guitarra");
        teacher2.setInstrument("Guitarra");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher2));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, "piano", null, null, null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().teachers().size());
        assertEquals("Prof. Ana", result.getBody().teachers().get(0).name());
    }

    @Test
    void getTeachersFiltersByInstrumentTest() {
        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Carlos");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setInstrument("Guitarra");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher2));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, "Guitarra", null, null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().teachers().size());
        assertEquals("Prof. Carlos", result.getBody().teachers().get(0).name());
    }

    @Test
    void getTeachersInstrumentTodosReturnsAllTest() {
        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Carlos");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setInstrument("Guitarra");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher2));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, "Todos", null, null, null, null);

        assertEquals(2, result.getBody().teachers().size());
    }

    @Test
    void getTeachersFiltersByRoleCertificadosTest() {
        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Carlos");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setInstrument("Guitarra");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher2));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, "Certificados", null, null, null);

        assertEquals(1, result.getBody().teachers().size());
        assertEquals("profesor_certificado", result.getBody().teachers().get(0).role());
    }

    @Test
    void getTeachersFiltersByRoleParticularsTest() {
        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Carlos");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setInstrument("Guitarra");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher2));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, "Particulares", null, null, null);

        assertEquals(1, result.getBody().teachers().size());
        assertEquals("profesor_particular", result.getBody().teachers().get(0).role());
    }

    @Test
    void getTeachersFiltersByRoleExactMatchTest() {
        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Carlos");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setInstrument("Guitarra");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher2));

        // exact role string that doesn't match "Certificados" or "Particulares" branches
        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, "profesor_certificado", null, null, null);

        assertEquals(1, result.getBody().teachers().size());
    }

    @Test
    void getTeachersFiltersByModalityTest() {
        teacher.setClassModality("ONLINE");

        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Carlos");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setInstrument("Guitarra");
        teacher2.setClassModality("PRESENCIAL");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher2));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, null, "ONLINE", null, null);

        assertEquals(1, result.getBody().teachers().size());
        assertEquals("Prof. Ana", result.getBody().teachers().get(0).name());
    }

    @Test
    void getTeachersModalityNullTeacherReturnsFalseTest() {
        teacher.setClassModality(null);

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, null, "ONLINE", null, null);

        assertEquals(0, result.getBody().teachers().size());
    }

    @Test
    void getTeachersFiltersByAvailabilityTest() {
        teacher.setAvailabilityPreference("MORNING");

        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Carlos");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setInstrument("Guitarra");
        teacher2.setAvailabilityPreference("AFTERNOON");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher2));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, null, null, "MORNING", null);

        assertEquals(1, result.getBody().teachers().size());
        assertEquals("Prof. Ana", result.getBody().teachers().get(0).name());
    }

    @Test
    void getTeachersAvailabilityNullTeacherReturnsFalseTest() {
        teacher.setAvailabilityPreference(null);

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, null, null, "MORNING", null);

        assertEquals(0, result.getBody().teachers().size());
    }

    @Test
    void getTeachersFiltersByCityTest() {
        teacher.setCity("Madrid");

        User teacher2 = new User();
        teacher2.setId(UUID.randomUUID());
        teacher2.setName("Prof. Carlos");
        teacher2.setRole(Role.PROFESOR_PARTICULAR);
        teacher2.setInstrument("Guitarra");
        teacher2.setCity("Barcelona");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher, teacher2));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, null, null, null, "madrid");

        assertEquals(1, result.getBody().teachers().size());
        assertEquals("Prof. Ana", result.getBody().teachers().get(0).name());
    }

    @Test
    void getTeachersSortsCertifiedBeforeParticularTest() {
        User particular = new User();
        particular.setId(UUID.randomUUID());
        particular.setName("Aaa Particular");
        particular.setRole(Role.PROFESOR_PARTICULAR);
        particular.setInstrument("Guitarra");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(particular, teacher));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, null, null, null, null);

        assertEquals("Prof. Ana", result.getBody().teachers().get(0).name());
        assertEquals("profesor_certificado", result.getBody().teachers().get(0).role());
    }

    @Test
    void getTeachersBoardHasCorrectMetadataTest() {
        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, null, null, null, null, null, null);

        assertEquals(3, result.getBody().visibleLimit());
        assertEquals("scholar", result.getBody().requiredPlan());
        assertEquals(1, result.getBody().totalCount());
        assertEquals(0, result.getBody().hiddenCount());
    }

    @Test
    void getTeachersQueryMatchesBioTest() {
        teacher.setBio("Clases de piano clasico");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, "clasico", null, null, null, null, null);

        assertEquals(1, result.getBody().teachers().size());
    }

    @Test
    void getTeachersQueryMatchesCityTest() {
        teacher.setCity("Sevilla");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, "sevilla", null, null, null, null, null);

        assertEquals(1, result.getBody().teachers().size());
    }

    @Test
    void getTeachersQueryMatchesProvinceTest() {
        teacher.setProvince("Andalucia");

        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(teacher));

        ResponseEntity<BulletinBoardResponse> result = bulletinController.getTeachers(teacher, "andalucia", null, null, null, null, null);

        assertEquals(1, result.getBody().teachers().size());
    }

    // ── getTeacher (by id) ───────────────────────────────────────────

    @Test
    void getTeacherFoundReturnsTrueTest() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        ResponseEntity<?> result = bulletinController.getTeacher(teacher.getId());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertInstanceOf(BulletinTeacherResponse.class, result.getBody());
    }

    @Test
    void getTeacherNotFoundReturnsNotFoundTest() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<?> result = bulletinController.getTeacher(id);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("Profesor no encontrado", ((Map<?, ?>) result.getBody()).get("message"));
    }

    @Test
    void getTeacherFoundButIsUserRoleReturnsNotFoundTest() {
        User regularUser = new User();
        regularUser.setId(UUID.randomUUID());
        regularUser.setRole(Role.USUARIO);
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        ResponseEntity<?> result = bulletinController.getTeacher(regularUser.getId());

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void getTeacherParticularReturnsOkTest() {
        User particular = new User();
        particular.setId(UUID.randomUUID());
        particular.setName("Prof. Particular");
        particular.setRole(Role.PROFESOR_PARTICULAR);
        when(userRepository.findById(particular.getId())).thenReturn(Optional.of(particular));

        ResponseEntity<?> result = bulletinController.getTeacher(particular.getId());

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ── updateProfile ────────────────────────────────────────────────

    @Test
    void updateProfileTeacherScholarUpdatesSuccessfullyTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Violin",
                new BigDecimal("40.00"),
                "Experto en violin clasico",
                "Madrid",
                null,
                null,
                null,
                "Madrid",
                "ana@wavii.app",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                "PRESENCIAL"
        );
        when(userRepository.save(teacher)).thenReturn(teacher);

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertInstanceOf(BulletinTeacherResponse.class, result.getBody());
        BulletinTeacherResponse response = (BulletinTeacherResponse) result.getBody();
        assertEquals("Violin", response.instrument());
        assertEquals(new BigDecimal("40.00"), response.pricePerHour());
        verify(userRepository).save(teacher);
    }

    @Test
    void updateProfileUsuarioForbiddenTest() {
        User student = new User();
        student.setRole(Role.USUARIO);
        student.setSubscription(Subscription.FREE);
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("10.00"),
                "bio",
                "Sevilla",
                null,
                null,
                null,
                "Sevilla",
                "profe@test.com",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                null
        );

        ResponseEntity<?> result = bulletinController.updateProfile(student, request);

        assertEquals(403, result.getStatusCodeValue());
        assertEquals("scholar", ((Map<?, ?>) result.getBody()).get("requiredPlan"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateProfileTeacherWithoutScholarForbiddenTest() {
        teacher.setSubscription(Subscription.PLUS);
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                null,
                null,
                "Sevilla",
                null,
                null,
                null,
                "Sevilla",
                "profe@test.com",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                null
        );

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(403, result.getStatusCodeValue());
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateProfilePresentialWithoutCityReturnsBadRequestTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                null,
                null,
                null,
                null,
                "Madrid",
                "ana@wavii.app",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                "PRESENCIAL"
        );

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(400, result.getStatusCodeValue());
    }

    @Test
    void updateProfileAmbasModalityWithoutCityReturnsBadRequestTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "   ",
                null,
                null,
                null,
                "Madrid",
                "ana@wavii.app",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                "AMBAS"
        );

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(400, result.getStatusCodeValue());
    }

    @Test
    void updateProfileOnlineModalityNoCityRequiredTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                null,
                null,
                null,
                null,
                "Madrid",
                "ana@wavii.app",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                "ONLINE"
        );
        when(userRepository.save(teacher)).thenReturn(teacher);

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateProfileMissingProvinceReturnsBadRequestTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "Madrid",
                null,
                null,
                null,
                null,
                "ana@wavii.app",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                "ONLINE"
        );

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(400, result.getStatusCodeValue());
        assertEquals("La provincia es obligatoria", ((Map<?, ?>) result.getBody()).get("message"));
    }

    @Test
    void updateProfileMissingContactEmailReturnsBadRequestTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "Madrid",
                null,
                null,
                null,
                "Madrid",
                null,
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                "ONLINE"
        );

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(400, result.getStatusCodeValue());
        assertEquals("El correo de contacto es obligatorio", ((Map<?, ?>) result.getBody()).get("message"));
    }

    @Test
    void updateProfileMissingContactPhoneReturnsBadRequestTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "Madrid",
                null,
                null,
                null,
                "Madrid",
                "ana@wavii.app",
                null,
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                "ONLINE"
        );

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(400, result.getStatusCodeValue());
        assertEquals("El número de contacto es obligatorio", ((Map<?, ?>) result.getBody()).get("message"));
    }

    @Test
    void updateProfileInvalidEmailReturnsBadRequestTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "Madrid",
                null,
                null,
                null,
                "Madrid",
                "not-an-email",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                "ONLINE"
        );

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(400, result.getStatusCodeValue());
        assertEquals("El correo de contacto no es válido", ((Map<?, ?>) result.getBody()).get("message"));
    }

    @Test
    void updateProfileInvalidPhoneReturnsBadRequestTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "Madrid",
                null,
                null,
                null,
                "Madrid",
                "ana@wavii.app",
                "abc",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                "ONLINE"
        );

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(400, result.getStatusCodeValue());
        assertEquals("El número de contacto no es válido", ((Map<?, ?>) result.getBody()).get("message"));
    }

    @Test
    void updateProfileUsuarioRoleGetsUpgradedToParticularTest() {
        User student = new User();
        student.setId(UUID.randomUUID());
        student.setName("Student A");
        student.setRole(Role.USUARIO);
        student.setSubscription(Subscription.SCHOLAR);

        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "Madrid",
                null,
                null,
                null,
                "Madrid",
                "student@wavii.app",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "ANYTIME",
                null,
                "ONLINE"
        );
        when(userRepository.save(student)).thenReturn(student);

        ResponseEntity<?> result = bulletinController.updateProfile(student, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Role.PROFESOR_PARTICULAR, student.getRole());
    }

    // @Test
    // void updateProfileWithSocialUrlsAndImagesTest() {
    //     BulletinUpdateRequest request = new BulletinUpdateRequest(
    //             "Piano",
    //             new BigDecimal("30.00"),
    //             "Bio",
    //             "Madrid",
    //             40.4168,
    //             -3.7038,
    //             "Calle Mayor 1",
    //             "Madrid",
    //             "ana@wavii.app",
    //             "+34-600-000-000",
    //             "https://instagram.com/ana",
    //             "https://tiktok.com/ana",
    //             "https://youtube.com/ana",
    //             "https://facebook.com/ana",
    //             "https://cdn.wavii.app/banner.jpg",
    //             List.of("https://cdn.wavii.app/place1.jpg", "  ", null),
    //             "MORNING",
    //             "Mañanas disponible",
    //             "PRESENCIAL"
    //     );
    //     when(userRepository.save(teacher)).thenReturn(teacher);

    //     ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

    //     assertEquals(HttpStatus.OK, result.getStatusCode());
    //     verify(userRepository).save(teacher);
    // }

    @Test
    void updateProfileAvailabilityAfternoonNormalizesTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "Madrid",
                null,
                null,
                null,
                "Madrid",
                "ana@wavii.app",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "AFTERNOON",
                null,
                "ONLINE"
        );
        when(userRepository.save(teacher)).thenReturn(teacher);

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateProfileAvailabilityCustomNormalizesTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "Madrid",
                null,
                null,
                null,
                "Madrid",
                "ana@wavii.app",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "CUSTOM",
                "Solo fines de semana",
                "ONLINE"
        );
        when(userRepository.save(teacher)).thenReturn(teacher);

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateProfileAvailabilityInvalidSetsNullTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "Madrid",
                null,
                null,
                null,
                "Madrid",
                "ana@wavii.app",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                "INVALID_AVAILABILITY",
                null,
                "ONLINE"
        );
        when(userRepository.save(teacher)).thenReturn(teacher);

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNull(teacher.getAvailabilityPreference());
    }

    @Test
    void updateProfileNullAvailabilitySetsNullTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Piano",
                new BigDecimal("30.00"),
                "Bio",
                "Madrid",
                null,
                null,
                null,
                "Madrid",
                "ana@wavii.app",
                "600000000",
                null,
                null,
                null,
                null,
                null,
                List.<String>of(),
                null,
                null,
                "ONLINE"
        );
        when(userRepository.save(teacher)).thenReturn(teacher);

        ResponseEntity<?> result = bulletinController.updateProfile(teacher, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ── uploadImage ──────────────────────────────────────────────────

    @Test
    void uploadImageEmptyFileReturnsBadRequestTest() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        ResponseEntity<?> result = bulletinController.uploadImage(emptyFile, "banner");

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("El archivo no puede estar vacío", ((Map<?, ?>) result.getBody()).get("message"));
    }

    @Test
    void uploadImageIOExceptionReturnsInternalServerErrorTest() throws Exception {
        // Use a mock that throws on getInputStream()
        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenThrow(new java.io.IOException("disk full"));

        ResponseEntity<?> result = bulletinController.uploadImage(mockFile, "banner");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("No se pudo guardar la imagen", ((Map<?, ?>) result.getBody()).get("message"));
    }
}
