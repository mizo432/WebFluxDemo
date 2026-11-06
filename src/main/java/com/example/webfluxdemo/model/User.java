package com.example.webfluxdemo.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * User entity mapped to the {@code users} database table.
 * <p>
 * Represents a user with a name and email address. Validation constraints
 * are enforced via Jakarta validation annotations and manually in {@link com.example.webfluxdemo.handler.UserHandler}.
 *
 * @see com.example.webfluxdemo.repository.UserRepository
 * @see com.example.webfluxdemo.handler.UserHandler
 */
@Table("users")
public class User {

    /** Database-generated primary key. */
    @Id
    @Column("id")
    private Long id;

    /** User's display name; must be non-blank and at most 100 characters. */
    @NotBlank(message = "名前は必須です")
    @Size(max = 100, message = "名前は100文字以内で指定してください")
    @Column("name")
    private String name;

    /** User's email address; must be non-blank, valid format, and at most 200 characters. */
    @NotBlank(message = "メールは必須です")
    @Email(message = "正しいメールアドレスを指定してください")
    @Size(max = 200, message = "メールは200文字以内で指定してください")
    @Column("email")
    private String email;

    /** Default constructor for framework use. */
    public User() {
    }

    /**
     * Constructs a fully populated user.
     *
     * @param id the database identifier
     * @param name the display name
     * @param email the email address
     */
    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /**
     * Returns the database identifier.
     *
     * @return the user ID, or {@code null} for new unsaved users
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the database identifier.
     *
     * @param id the user ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the user's display name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's display name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the user's email address.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email the email
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
