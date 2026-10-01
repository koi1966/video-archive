package ua.oleg.videoarchive.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    /**
     * Открывает главную страницу застосунку. Обрабатывает оба входных URL — `/` і `/index` — і повертає ім’я Thymeleaf-шаблона главной страницы.
     * @return результат роботи методу (String)
     */
    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }
}
