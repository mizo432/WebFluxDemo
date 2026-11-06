package com.example.webfluxdemo.controller;

import com.example.webfluxdemo.model.User;
import com.example.webfluxdemo.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/users")
public class UserWebController {

    private final UserRepository userRepository;

    public UserWebController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listUsers(Model model) {
        Flux<User> users = userRepository.findAll();
        model.addAttribute("users", new ReactiveDataDriverContextVariable(users, 100));
        return "users";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "user-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Mono<User> user = userRepository.findById(id);
        model.addAttribute("user", new ReactiveDataDriverContextVariable(user.flux(), 1));
        return "user-form";
    }

    @PostMapping("/submit")
    public Mono<String> submitForm(@ModelAttribute User user) {
        return userRepository.save(user).then(Mono.just("redirect:/users"));
    }
}
