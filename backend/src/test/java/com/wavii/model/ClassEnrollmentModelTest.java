package com.wavii.model;

import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de cobertura para la entidad ClassEnrollment.
 * Cubre getters, setters, builder, equals, hashCode y toString.
 * 
 * @author eduglezexp
 */
class ClassEnrollmentModelTest {

    @Test
    void gettersAndSettersWorkCorrectlyTest() {
        ClassEnrollment enrollment = new ClassEnrollment();
        UUID id = UUID.randomUUID();
        User teacher = User.builder().id(UUID.randomUUID()).name("Teacher").build();
        User student = User.builder().id(UUID.randomUUID()).name("Student").build();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal price = new BigDecimal("25.00");

        enrollment.setId(id);
        enrollment.setTeacher(teacher);
        enrollment.setStudent(student);
        enrollment.setTeacherName("Teacher Name");
        enrollment.setStudentName("Student Name");
        enrollment.setInstrument("Piano");
        enrollment.setCity("Madrid");
        enrollment.setProvince("Madrid");
        enrollment.setModality("ONLINE");
        enrollment.setRequestedModality("PRESENCIAL");
        enrollment.setUnitPrice(price);
        enrollment.setPaymentStatus("paid");
        enrollment.setRequestMessage("Hello");
        enrollment.setRequestAvailability("Afternoon");
        enrollment.setHoursPurchased(5);
        enrollment.setHoursUsed(2);
        enrollment.setStripePaymentIntentId("pi_123");
        enrollment.setPaymentReceiptNumber("REC-456");
        enrollment.setClassLink("http://zoom.us");
        enrollment.setCreatedAt(now);

        assertEquals(id, enrollment.getId());
        assertEquals(teacher, enrollment.getTeacher());
        assertEquals(student, enrollment.getStudent());
        assertEquals("Teacher Name", enrollment.getTeacherName());
        assertEquals("Student Name", enrollment.getStudentName());
        assertEquals("Piano", enrollment.getInstrument());
        assertEquals("Madrid", enrollment.getCity());
        assertEquals("Madrid", enrollment.getProvince());
        assertEquals("ONLINE", enrollment.getModality());
        assertEquals("PRESENCIAL", enrollment.getRequestedModality());
        assertEquals(price, enrollment.getUnitPrice());
        assertEquals("paid", enrollment.getPaymentStatus());
        assertEquals("Hello", enrollment.getRequestMessage());
        assertEquals("Afternoon", enrollment.getRequestAvailability());
        assertEquals(5, enrollment.getHoursPurchased());
        assertEquals(2, enrollment.getHoursUsed());
        assertEquals("pi_123", enrollment.getStripePaymentIntentId());
        assertEquals("REC-456", enrollment.getPaymentReceiptNumber());
        assertEquals("http://zoom.us", enrollment.getClassLink());
        assertEquals(now, enrollment.getCreatedAt());
    }

    @Test
    void builderAndDefaultsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .teacherName("T")
                .build();

        assertNotNull(enrollment.getCreatedAt());
        assertEquals(1, enrollment.getHoursPurchased());
        assertEquals(0, enrollment.getHoursUsed());
        assertEquals("T", enrollment.getTeacherName());
    }

    @Test
    void allArgsConstructorTest() {
        UUID id = UUID.randomUUID();
        User t = new User();
        User s = new User();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal price = BigDecimal.TEN;

        ClassEnrollment enrollment = new ClassEnrollment(
                id, t, s, "TN", "SN", "Inst", "City", "Prov", "Mod", "RMod",
                price, "paid", "msg", "avail", 10, 5, "pi", "rec", "link", now
        );

        assertEquals(id, enrollment.getId());
        assertEquals("TN", enrollment.getTeacherName());
        assertEquals(10, enrollment.getHoursPurchased());
    }

    @Test
    void equalsHashCodeToStringExhaustiveTest() throws Exception {
        UUID id = UUID.randomUUID();
        ClassEnrollment e1 = ClassEnrollment.builder()
                .id(id)
                .teacherName("T")
                .studentName("S")
                .instrument("P")
                .unitPrice(BigDecimal.TEN)
                .hoursPurchased(5)
                .createdAt(LocalDateTime.now())
                .build();
        ClassEnrollment e2 = ClassEnrollment.builder()
                .id(id)
                .teacherName("T")
                .studentName("S")
                .instrument("P")
                .unitPrice(BigDecimal.TEN)
                .hoursPurchased(5)
                .createdAt(e1.getCreatedAt())
                .build();

        ModelTestHelper.testEqualsAndHashCodeExhaustively(e1, e2, ClassEnrollment.class);
    }
    @Test
    void classEnrollmentBuilderCoverageTest() {
        ClassEnrollment.builder()
            .id(UUID.randomUUID())
            .teacher(new User())
            .student(new User())
            .teacherName("t")
            .studentName("s")
            .instrument("i")
            .city("c")
            .province("p")
            .modality("m")
            .requestedModality("rm")
            .unitPrice(BigDecimal.TEN)
            .paymentStatus("p")
            .requestMessage("m")
            .requestAvailability("a")
            .hoursPurchased(10)
            .hoursUsed(5)
            .stripePaymentIntentId("pi")
            .paymentReceiptNumber("r")
            .classLink("l")
            .createdAt(LocalDateTime.now())
            .build();
    }
}
