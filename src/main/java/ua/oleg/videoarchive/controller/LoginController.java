package ua.oleg.videoarchive.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    /**
     * Виконує операцію `GetMapping` у рамках класу `LoginController`.
     * @param login( параметр методу
     * @return результат роботи методу (@)
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
