package com.wavii.repository;

import com.wavii.model.TeacherReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeacherReportRepository extends JpaRepository<TeacherReport, UUID> {
}
