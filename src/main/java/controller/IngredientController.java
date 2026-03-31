package controller;

import entity.Ingredient;
import entity.Unit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.IngredientRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientRepository ingredientRepository;

    public IngredientController(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @GetMapping
    public List<Ingredient> getAllIngredients() {
        return ingredientRepository.findAllIngredients();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getIngredientById(@PathVariable Integer id) {
        Ingredient ingredient = ingredientRepository.findIngredientById(id);
        if (ingredient == null) {
            return ResponseEntity.status(404).body("Ingredient.id=" + id + " is not found");
        }
        return ResponseEntity.ok(ingredient);
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getStock(@PathVariable Integer id,
                                                        @RequestParam(required = false) String at,
                                                        @RequestParam(required = false) Unit unit) {
        if (at == null || unit == null) {
            return ResponseEntity.badRequest().body("Either mandatory query parameter `at` or `unit` is not provided.");
        }
        Ingredient ingredient = ingredientRepository.findIngredientById(id);
        if (ingredient == null) {
            return ResponseEntity.status(404).body("Ingredient.id=" + id + " is not found");
        }
        Timestamp timestamp = Timestamp.valueOf(at); // Assume at is in format yyyy-MM-dd HH:mm:ss
        double value = ingredientRepository.getStockValueAtTime(id, timestamp, unit);
        return ResponseEntity.ok(Map.of("unit", unit.name(), "value", value));
    }
}