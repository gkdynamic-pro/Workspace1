package net.codejava.repository;

import net.codejava.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    List<Student> findByOwner_Username(String username);
    Optional<Student> findByIdAndOwner_Username(Long id, String username);
}
