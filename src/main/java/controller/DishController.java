package controller;

import entity.Dish;
import entity.DishCreationRequest;
import entity.DishIngredient;
import entity.DishTypeEnum;
import entity.Ingredient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.DishRepository;
import repository.IngredientRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;

    public DishController(DishRepository dishRepository, IngredientRepository ingredientRepository) {
        this.dishRepository = dishRepository;
        this.ingredientRepository = ingredientRepository;
    }

    @GetMapping
    public List<Dish> getAllDishes(
            @RequestParam(required = false) Double priceUnder,
            @RequestParam(required = false) Double priceOver,
            @RequestParam(required = false) String name) {
        return dishRepository.findAllDishesWithFilters(priceUnder, priceOver, name);
    }

    @PostMapping
    public ResponseEntity<?> createDishes(@RequestBody List<DishCreationRequest> requests) {
        try {
            List<Dish> createdDishes = new ArrayList<>();
            for (DishCreationRequest request : requests) {
                if (dishRepository.existsByName(request.getName())) {
                    return ResponseEntity.badRequest().body("Dish.name=" + request.getName() + " already exists");
                }
                Dish dish = new Dish();
                dish.setName(request.getName());
                dish.setPrice(request.getPrice());
                DishTypeEnum dishType = mapCategoryToEnum(request.getCategory());
                dish.setDishType(dishType);
                dish.setDishIngredients(new ArrayList<>()); // empty list
                Dish savedDish = dishRepository.save(dish);
                createdDishes.add(savedDish);
            }
            return ResponseEntity.status(201).body(createdDishes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    private DishTypeEnum mapCategoryToEnum(String category) {
        switch (category.toLowerCase()) {
            case "entrée":
                return DishTypeEnum.STARTER;
            case "résistance":
                return DishTypeEnum.MAIN;
            case "dessert":
                return DishTypeEnum.DESSERT;
            default:
                throw new IllegalArgumentException("Invalid category: " + category);
        }
    }

    @PutMapping("/{id}/ingredients")
    public ResponseEntity<String> updateDishIngredients(@PathVariable Integer id, @RequestBody(required = false) List<DishIngredient> dishIngredients) {
        Dish dish = dishRepository.findDishById(id);
        if (dish == null) {
            return ResponseEntity.status(404).body("Dish.id=" + id + " is not found");
        }
        if (dishIngredients == null) {
            return ResponseEntity.badRequest().body("Request body is required");
        }
        // Filter dishIngredients to only those with existing ingredients
        List<DishIngredient> validDishIngredients = dishIngredients.stream()
                .filter(di -> di.getIngredient() != null && di.getIngredient().getId() != null)
                .filter(di -> ingredientRepository.findIngredientById(di.getIngredient().getId()) != null)
                .collect(Collectors.toList());
        // Update
        dishRepository.updateDishIngredients(id, validDishIngredients);
        return ResponseEntity.ok().build();
    }
}