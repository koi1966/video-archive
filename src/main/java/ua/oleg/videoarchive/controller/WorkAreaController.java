package ua.oleg.videoarchive.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.oleg.videoarchive.model.WorkArea;
import ua.oleg.videoarchive.repository.AppUserRepository;
import ua.oleg.videoarchive.repository.WorkAreaRepository;

@Controller
@RequestMapping("/work-areas")
public class WorkAreaController {
    private final WorkAreaRepository workAreas;
    private final AppUserRepository users;

    public WorkAreaController(WorkAreaRepository workAreas, AppUserRepository users) {
        this.workAreas = workAreas;
        this.users = users;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("workAreas", workAreas.findAll());
        return "work-areas";
    }

    @GetMapping("/new")
    public String newWorkArea(Model model) {
        WorkArea workArea = new WorkArea();
        workArea.setPatch("");
        model.addAttribute("workArea", workArea);
        return "work-area-form";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam(defaultValue = "") String patch,
                         Model model) {
        name = name == null ? "" : name.trim();
        patch = patch == null ? "" : patch.trim();

        if (name.isBlank()) {
            return formError(model, null, name, patch, "Название района не может быть пустым.");
        }
        if (workAreas.findByNameIgnoreCase(name).isPresent()) {
            return formError(model, null, name, patch, "Такой район уже существует.");
        }

        WorkArea workArea = new WorkArea();
        workArea.setName(name);
        workArea.setPatch(patch);
        workAreas.save(workArea);
        return "redirect:/work-areas";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        WorkArea workArea = workAreas.findById(id).orElseThrow();
        model.addAttribute("workArea", workArea);
        return "work-area-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable String id,
                         @RequestParam String name,
                         @RequestParam(defaultValue = "") String patch,
                         Model model) {
        WorkArea workArea = workAreas.findById(id).orElseThrow();
        name = name == null ? "" : name.trim();
        patch = patch == null ? "" : patch.trim();

        if (name.isBlank()) {
            return formError(model, id, name, patch, "Название района не может быть пустым.");
        }

        var existing = workAreas.findByNameIgnoreCase(name);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            return formError(model, id, name, patch, "Такой район уже существует.");
        }

        workArea.setName(name);
        workArea.setPatch(patch);
        workAreas.save(workArea);
        return "redirect:/work-areas";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, Model model) {
        if (!workAreas.existsById(id)) {
            return "redirect:/work-areas";
        }

        long assignedUsers = users.countByWorkAreaId(id);
        if (assignedUsers > 0) {
            model.addAttribute("workAreas", workAreas.findAll());
            model.addAttribute("error", "Нельзя удалить район: он назначен пользователям (" + assignedUsers + ").");
            return "work-areas";
        }

        workAreas.deleteById(id);
        return "redirect:/work-areas";
    }

    private String formError(Model model, String id, String name, String patch, String error) {
        WorkArea workArea = new WorkArea();
        workArea.setId(id);
        workArea.setName(name);
        workArea.setPatch(patch);
        model.addAttribute("workArea", workArea);
        model.addAttribute("error", error);
        return "work-area-form";
    }
}
