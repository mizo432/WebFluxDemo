package com.example.webfluxdemo.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public class User {

    @Id
    private Long id;

    @NotBlank(message = "名前は必須です")
    @Size(max = 100, message = "名前は100文字以内で指定してください")
    private String name;

    @NotBlank(message = "メールは必須です")
    @Email(message = "正しいメールアドレスを指定してください")
    @Size(max = 200, message = "メールは200文字以内で指定してください")
    private String email;

    public User() {
    }

    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
