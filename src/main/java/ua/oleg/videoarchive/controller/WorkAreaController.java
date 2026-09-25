package ua.oleg.videoarchive.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.oleg.videoarchive.model.WorkArea;
import ua.oleg.videoarchive.repository.WorkAreaRepository;

@Controller
@RequestMapping("/work-areas")
public class WorkAreaController {

    private final WorkAreaRepository repository;

    public WorkAreaController(WorkAreaRepository repository) {
        this.repository = repository;
    }

    /**
     * Список рабочих областей.
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("workAreas", repository.findAll());
        return "work-areas";
    }

    /**
     * Форма добавления новой рабочей области.
     */
    @GetMapping("/new")
    public String newWorkArea(Model model) {
        WorkArea workArea = new WorkArea();
        workArea.setPatch("");

        model.addAttribute("workArea", workArea);
        return "work-area-form";
    }

    /**
     * Создание новой рабочей области.
     */
    @PostMapping
    public String create(
            @RequestParam String name,
            @RequestParam(defaultValue = "") String patch,
            Model model) {

        name = name == null ? "" : name.trim();
        patch = patch == null ? "" : patch.trim();

        if (name.isBlank()) {
            model.addAttribute("error", "Название рабочей области не может быть пустым.");

            WorkArea workArea = new WorkArea();
            workArea.setName(name);
            workArea.setPatch(patch);

            model.addAttribute("workArea", workArea);
            return "work-area-form";
        }

        if (repository.findByNameIgnoreCase(name).isPresent()) {
            model.addAttribute(
                    "error",
                    "Рабочая область с таким названием уже существует."
            );

            WorkArea workArea = new WorkArea();
            workArea.setName(name);
            workArea.setPatch(patch);

            model.addAttribute("workArea", workArea);
            return "work-area-form";
        }

        WorkArea workArea = new WorkArea();
        workArea.setName(name);
        workArea.setPatch(patch);

        repository.save(workArea);

        return "redirect:/work-areas";
    }

    /**
     * Форма редактирования.
     */
    @GetMapping("/{id}/edit")
    public String edit(
            @PathVariable String id,
            Model model) {

        WorkArea workArea = repository.findById(id).orElseThrow();

        model.addAttribute("workArea", workArea);

        return "work-area-form";
    }

    /**
     * Сохранение изменений.
     */
    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @RequestParam String name,
            @RequestParam(defaultValue = "") String patch,
            Model model) {

        WorkArea workArea = repository.findById(id).orElseThrow();

        name = name == null ? "" : name.trim();
        patch = patch == null ? "" : patch.trim();

        if (name.isBlank()) {
            model.addAttribute(
                    "error",
                    "Название рабочей области не может быть пустым."
            );

            workArea.setName(name);
            workArea.setPatch(patch);

            model.addAttribute("workArea", workArea);

            return "work-area-form";
        }

        var existing = repository.findByNameIgnoreCase(name);

        if (existing.isPresent()
                && !existing.get().getId().equals(id)) {

            model.addAttribute(
                    "error",
                    "Рабочая область с таким названием уже существует."
            );

            workArea.setName(name);
            workArea.setPatch(patch);

            model.addAttribute("workArea", workArea);

            return "work-area-form";
        }

        workArea.setName(name);
        workArea.setPatch(patch);

        repository.save(workArea);

        return "redirect:/work-areas";
    }

    /**
     * Удаление рабочей области.
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id) {

        if (repository.existsById(id)) {
            repository.deleteById(id);
        }

        return "redirect:/work-areas";
    }
}
