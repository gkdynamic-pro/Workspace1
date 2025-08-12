package net.codejava.controller;

import net.codejava.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AppUserRepository repo;

    public AdminController(AppUserRepository repo) {
        this.repo = repo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public List<UserSummary> listUsersLegacy() {
        return repo.findAll().stream()
                .map(u -> new UserSummary(u.getId(), u.getUsername(), u.getRoles()))
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/page")
    public ResponseEntity<PageResponse<UserSummary>> listUsersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var pageable = PageRequest.of(page, size);
        var result = repo.findAll(pageable);
        var content = result.getContent().stream()
                .map(u -> new UserSummary(u.getId(), u.getUsername(), u.getRoles()))
                .toList();

        return ResponseEntity.ok(
                new PageResponse<>(content, page, size, result.getTotalElements(), result.getTotalPages())
        );
    }

    public record UserSummary(Long id, String username, Set<String> roles) {}

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
