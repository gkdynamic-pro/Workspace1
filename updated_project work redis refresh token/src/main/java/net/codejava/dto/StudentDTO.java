package net.codejava.dto;

public class StudentDTO {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String ownerUsername; // visible for admin

    public StudentDTO() {}

    public StudentDTO(Long id, String name, String email, Integer age, String ownerUsername) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.ownerUsername = ownerUsername;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
}
