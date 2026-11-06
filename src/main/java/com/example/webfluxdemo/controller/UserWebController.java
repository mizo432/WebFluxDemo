package com.example.webfluxdemo.controller;

import com.example.webfluxdemo.model.User;
import com.example.webfluxdemo.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserWebController {

    private final UserRepository userRepository;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    public UserWebController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public Mono<String> listUsers(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                                  Model model) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        int skip = safePage * safeSize;
        Mono<Long> totalMono = userRepository.countAll();
        Flux<User> usersFlux = userRepository.findAll().skip(skip).take(safeSize);

        return totalMono.flatMap(total -> {
            int totalPages = (int) Math.ceil((double) total / safeSize);
            return usersFlux.collectList().map(users -> {
                model.addAttribute("users", users);
                model.addAttribute("currentPage", safePage);
                model.addAttribute("pageSize", safeSize);
                model.addAttribute("totalPages", totalPages);
                model.addAttribute("totalElements", total);
                model.addAttribute("hasPrevious", safePage > 0);
                model.addAttribute("hasNext", safePage + 1 < totalPages);
                return "users";
            });
        });
    }

    // htmx で部分的にリフレッシュするためのエンドポイント
    @GetMapping("/table-fragment")
    public Mono<String> listUsersFragment(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                                          Model model) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        int skip = safePage * safeSize;
        Mono<Long> totalMono = userRepository.countAll();
        Flux<User> usersFlux = userRepository.findAll().skip(skip).take(safeSize);

        return totalMono.flatMap(total -> {
            int totalPages = (int) Math.ceil((double) total / safeSize);
            return usersFlux.collectList().map(users -> {
                model.addAttribute("users", users);
                model.addAttribute("currentPage", safePage);
                model.addAttribute("pageSize", safeSize);
                model.addAttribute("totalPages", totalPages);
                model.addAttribute("totalElements", total);
                model.addAttribute("hasPrevious", safePage > 0);
                model.addAttribute("hasNext", safePage + 1 < totalPages);
                return "users-table-fragment";
            });
        });
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "user-form";
    }

    @GetMapping("/edit/{id}")
    public Mono<String> showEditForm(@PathVariable Long id, Model model) {
        return userRepository.findById(id).map(user -> {
            model.addAttribute("user", user);
            return "user-form";
        }).defaultIfEmpty("redirect:/users");
    }

    @PostMapping("/submit")
    public Mono<String> submitForm(@Valid @ModelAttribute User user, BindingResult result) {
        if (result.hasErrors()) {
            return Mono.just("user-form");
        }
        return userRepository.save(user).then(Mono.just("redirect:/users"));
    }
}
