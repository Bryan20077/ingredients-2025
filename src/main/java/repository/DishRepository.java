package repository;

import config.DataSourceProvider;
import entity.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class DishRepository {

    private final DataSourceProvider dataSourceProvider;

    public DishRepository(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    public List<Dish> findAllDishes() {
        String sql = "select id, name, selling_price, dish_type from dish";
        List<Dish> dishes = new ArrayList<>();
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Dish dish = new Dish();
                dish.setId(rs.getInt("id"));
                dish.setName(rs.getString("name"));
                dish.setPrice(rs.getDouble("selling_price"));
                dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                dish.setDishIngredients(findIngredientsByDishId(rs.getInt("id")));
                dishes.add(dish);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dishes;
    }

    public Dish findDishById(Integer id) {
        String sql = "select id, name, selling_price, dish_type from dish where id = ?";
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Dish dish = new Dish();
                    dish.setId(rs.getInt("id"));
                    dish.setName(rs.getString("name"));
                    dish.setPrice(rs.getDouble("selling_price"));
                    dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                    dish.setDishIngredients(findIngredientsByDishId(id));
                    return dish;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private List<DishIngredient> findIngredientsByDishId(Integer idDish) {
        String sql = "select ingredient.id, ingredient.name, ingredient.price, ingredient.category, di.required_quantity, di.unit from ingredient join dish_ingredient di on di.id_ingredient = ingredient.id where id_dish = ?";
        List<DishIngredient> dishIngredients = new ArrayList<>();
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDish);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setId(rs.getInt("id"));
                    ingredient.setName(rs.getString("name"));
                    ingredient.setPrice(rs.getDouble("price"));
                    ingredient.setCategory(CategoryEnum.valueOf(rs.getString("category")));

                    DishIngredient dishIngredient = new DishIngredient();
                    dishIngredient.setIngredient(ingredient);
                    dishIngredient.setQuantity(rs.getDouble("required_quantity"));
                    dishIngredient.setUnit(Unit.valueOf(rs.getString("unit")));

                    dishIngredients.add(dishIngredient);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dishIngredients;
    }

    // For PUT /dishes/{id}/ingredients
    public void updateDishIngredients(Integer dishId, List<DishIngredient> dishIngredients) {
        detachIngredients(dishId);
        attachIngredients(dishId, dishIngredients);
    }

    private void detachIngredients(Integer dishId) {
        String sql = "DELETE FROM dish_ingredient where id_dish = ?";
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void attachIngredients(Integer dishId, List<DishIngredient> dishIngredients) {
        String sql = "insert into dish_ingredient (id, id_ingredient, id_dish, required_quantity, unit) values (?, ?, ?, ?, ?::unit)";
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (DishIngredient di : dishIngredients) {
                ps.setInt(1, getNextSerialValue(conn, "dish_ingredient", "id"));
                ps.setInt(2, di.getIngredient().getId());
                ps.setInt(3, dishId);
                ps.setDouble(4, di.getQuantity());
                ps.setString(5, di.getUnit().name());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName) throws SQLException {
        // Similar to original
        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        if (sequenceName == null) {
            throw new IllegalArgumentException("No sequence found for " + tableName + "." + columnName);
        }
        updateSequenceNextValue(conn, tableName, columnName, sequenceName);
        String nextValSql = "SELECT nextval(?)";
        try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private String getSerialSequenceName(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT pg_get_serial_sequence(?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private void updateSequenceNextValue(Connection conn, String tableName, String columnName, String sequenceName) throws SQLException {
        String setValSql = String.format("SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))", sequenceName, columnName, tableName);
        try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
            ps.executeQuery();
        }
    }

    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM dish WHERE name = ?";
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public Dish save(Dish dish) {
        String sql = "INSERT INTO dish (id, name, selling_price, dish_type) VALUES (?, ?, ?, ?::dish_type_enum)";
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int id = getNextSerialValue(conn, "dish", "id");
            dish.setId(id);
            ps.setInt(1, id);
            ps.setString(2, dish.getName());
            ps.setDouble(3, dish.getPrice());
            ps.setString(4, dish.getDishType().name());
            ps.executeUpdate();
            return dish;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Dish> findAllDishesWithFilters(Double priceUnder, Double priceOver, String name) {
        StringBuilder sql = new StringBuilder("SELECT id, name, selling_price, dish_type FROM dish WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (priceUnder != null) {
            sql.append(" AND selling_price < ?");
            params.add(priceUnder);
        }
        if (priceOver != null) {
            sql.append(" AND selling_price > ?");
            params.add(priceOver);
        }
        if (name != null && !name.trim().isEmpty()) {
            sql.append(" AND name ILIKE ?");
            params.add("%" + name + "%");
        }

        List<Dish> dishes = new ArrayList<>();
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Dish dish = new Dish();
                    dish.setId(rs.getInt("id"));
                    dish.setName(rs.getString("name"));
                    dish.setPrice(rs.getDouble("selling_price"));
                    dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                    dish.setDishIngredients(findIngredientsByDishId(rs.getInt("id")));
                    dishes.add(dish);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dishes;
    }
}