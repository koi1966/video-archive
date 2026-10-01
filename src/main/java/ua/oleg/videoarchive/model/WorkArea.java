package ua.oleg.videoarchive.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "workAreas")
public class WorkArea {
    @Id
    private String id;
    private String name;
    private String patch;

    /**
     * Повертає ідентифікатор об’єкта.
     * @return результат роботи методу (String)
     */
    public String getId() {
        return id;
    }

    /**
     * Встановлює ідентифікатор об’єкта.
     * @param id ідентифікатор об’єкта
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Повертає назва робочої зони.
     * @return результат роботи методу (String)
     */
    public String getName() {
        return name;
    }

    /**
     * Встановлює назва робочої зони.
     * @param name параметр методу
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Повертає фізичний шлях зберігання файлів робочої зони.
     * @return результат роботи методу (String)
     */
    public String getPatch() {
        return patch;
    }

    /**
     * Встановлює фізичний шлях зберігання файлів робочої зони.
     * @param patch фізичний шлях каталогу робочої зони
     */
    public void setPatch(String patch) {
        this.patch = patch;
    }
}
