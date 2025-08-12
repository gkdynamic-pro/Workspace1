package net.codejava.controller;

import net.codejava.dto.StudentDTO;
import net.codejava.service.StudentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService; // <-- CI: depend on service only

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }


    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }


    @GetMapping
    public List<StudentDTO> getAll(Authentication auth) {
        return studentService.getAll(auth);
    }


    @PostMapping
    public ResponseEntity<StudentDTO> create(@RequestBody StudentDTO dto, Authentication auth) {
        if (dto.getName() == null || dto.getEmail() == null || dto.getAge() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Optional<StudentDTO> saved = studentService.create(dto, auth);
        return saved.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }


    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> getById(@PathVariable Long id, Authentication auth) {
        return studentService.getById(id, auth)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> update(@PathVariable Long id,
                                             @RequestBody StudentDTO dto,
                                             Authentication auth) {
        Optional<StudentDTO> result = isAdmin(auth)
                ? studentService.updateAdmin(id, dto)
                : studentService.updateSelf(id, dto, auth);

        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        boolean ok = isAdmin(auth)
                ? studentService.deleteAdmin(id)
                : studentService.deleteSelf(id, auth);

        return ok ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }


    @GetMapping("/search")
    public ResponseEntity<PageResponse<StudentDTO>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,      // (email is supported by spec if you add it there)
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<StudentDTO> result = studentService.search(name, minAge, maxAge, pageable, auth);

        return ResponseEntity.ok(new PageResponse<>(
                result.getContent(), page, size, result.getTotalElements(), result.getTotalPages()
        ));
    }


    public static class PageResponse<T> {
        public List<T> content;
        public int page;
        public int size;
        public long totalElements;
        public int totalPages;

        public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }
    }
}
