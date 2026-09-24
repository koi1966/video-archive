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
    public String newArea(Model model) {
        model.addAttribute("workArea", new WorkArea());
        return "work-area-form";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam(required = false, defaultValue = "") String patch,
                         Model model) {
        name = clean(name);
        patch = patch == null ? "" : patch.trim();
        if (name.isBlank()) return formError(model, new WorkArea(), "Название района не может быть пустым.");
        if (workAreas.existsByNameIgnoreCase(name)) {
            WorkArea area = new WorkArea();
            area.setName(name); area.setPatch(patch);
            return formError(model, area, "Такой район уже существует.");
        }
        WorkArea area = new WorkArea();
        area.setName(name);
        area.setPatch(patch);
        workAreas.save(area);
        return "redirect:/work-areas";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        model.addAttribute("workArea", workAreas.findById(id).orElseThrow());
        return "work-area-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable String id,
                         @RequestParam String name,
                         @RequestParam(required = false, defaultValue = "") String patch,
                         Model model) {
        WorkArea area = workAreas.findById(id).orElseThrow();
        name = clean(name);
        patch = patch == null ? "" : patch.trim();
        if (name.isBlank()) return formError(model, area, "Название района не может быть пустым.");
        if (workAreas.existsByNameIgnoreCaseAndIdNot(name, id)) {
            area.setName(name); area.setPatch(patch);
            return formError(model, area, "Такой район уже существует.");
        }
        area.setName(name);
        area.setPatch(patch);
        workAreas.save(area);
        return "redirect:/work-areas";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, Model model) {
        if (users.existsByWorkAreaId(id)) {
            model.addAttribute("workAreas", workAreas.findAll());
            model.addAttribute("error", "Нельзя удалить район: он назначен одному или нескольким пользователям.");
            return "work-areas";
        }
        workAreas.deleteById(id);
        return "redirect:/work-areas";
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String formError(Model model, WorkArea area, String error) {
        model.addAttribute("workArea", area);
        model.addAttribute("error", error);
        return "work-area-form";
    }
}
