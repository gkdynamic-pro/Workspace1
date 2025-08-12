package net.codejava.service;

import net.codejava.dto.StudentDTO;
import net.codejava.model.AppUser;
import net.codejava.model.Student;
import net.codejava.repository.AppUserRepository;
import net.codejava.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final AppUserRepository userRepository;

    public StudentService(StudentRepository studentRepository, AppUserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    /* -------------------- helpers -------------------- */

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private StudentDTO toDTO(Student s, boolean includeOwner) {
        return new StudentDTO(
                s.getId(),
                s.getName(),
                s.getEmail(),
                s.getAge(),
                includeOwner && s.getOwner() != null ? s.getOwner().getUsername() : null
        );
    }

    private void applyPatch(Student s, StudentDTO dto) {
        if (dto.getName() != null) s.setName(dto.getName());
        if (dto.getEmail() != null) s.setEmail(dto.getEmail());
        if (dto.getAge() != null) s.setAge(dto.getAge());
    }

    private Specification<Student> buildSpec(String name, Integer minAge, Integer maxAge, String ownerUsernameOrNull) {
        return (root, q, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (StringUtils.hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase().trim() + "%"));
            }
            if (minAge != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("age"), minAge));
            }
            if (maxAge != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("age"), maxAge));
            }
            if (ownerUsernameOrNull != null) {
                // join owner and filter by username
                predicates.add(cb.equal(root.join("owner").get("username"), ownerUsernameOrNull));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /* -------------------- read APIs -------------------- */


    public List<StudentDTO> getAll(Authentication auth) {
        boolean admin = isAdmin(auth);
        if (admin) {
            return studentRepository.findAll().stream().map(s -> toDTO(s, true)).toList();
        } else {
            String username = auth.getName();
            return studentRepository.findByOwner_Username(username).stream().map(s -> toDTO(s, false)).toList();
        }
    }


    public Page<StudentDTO> search(String name, Integer minAge, Integer maxAge, Pageable pageable, Authentication auth) {
        boolean admin = isAdmin(auth);
        String owner = admin ? null : auth.getName();
        var spec = buildSpec(name, minAge, maxAge, owner);
        return studentRepository.findAll(spec, pageable).map(s -> toDTO(s, admin));
    }

    public Optional<StudentDTO> getById(Long id, Authentication auth) {
        boolean admin = isAdmin(auth);
        Optional<Student> s = admin
                ? studentRepository.findById(id)
                : studentRepository.findByIdAndOwner_Username(id, auth.getName());
        return s.map(st -> toDTO(st, admin));
    }

    /* -------------------- write APIs -------------------- */

    @Transactional
    public Optional<StudentDTO> create(StudentDTO dto, Authentication auth) {
        if (auth == null) return Optional.empty();
        Optional<AppUser> userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty()) return Optional.empty();

        Student s = new Student(dto.getName(), dto.getEmail(), dto.getAge(), userOpt.get());
        Student saved = studentRepository.save(s);
        return Optional.of(toDTO(saved, isAdmin(auth)));
    }


    @Transactional
    public Optional<StudentDTO> updateAdmin(Long id, StudentDTO dto) {
        return studentRepository.findById(id).map(s -> {
            applyPatch(s, dto);
            return toDTO(studentRepository.save(s), true);
        });
    }


    @Transactional
    public Optional<StudentDTO> updateSelf(Long id, StudentDTO dto, Authentication auth) {
        return studentRepository.findByIdAndOwner_Username(id, auth.getName()).map(s -> {
            applyPatch(s, dto);
            return toDTO(studentRepository.save(s), false);
        });
    }


    @Transactional
    public boolean deleteAdmin(Long id) {
        if (!studentRepository.existsById(id)) return false;
        studentRepository.deleteById(id);
        return true;
    }


    @Transactional
    public boolean deleteSelf(Long id, Authentication auth) {
        Optional<Student> s = studentRepository.findByIdAndOwner_Username(id, auth.getName());
        if (s.isEmpty()) return false;
        studentRepository.delete(s.get());
        return true;
    }
}
