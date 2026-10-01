package ua.oleg.videoarchive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VideoArchiveApplication {
    /**
     * Запускает Spring Boot-застосунок. Створює ApplicationContext, регистрирует контроллеры, сервисы, репозитории і конфигурацию безопасности, після чего запускает встроенный веб-сервер.
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        SpringApplication.run(VideoArchiveApplication.class, args);
    }
}
