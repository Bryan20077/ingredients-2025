package repository;

import config.DataSourceProvider;
import entity.*;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class IngredientRepository {

    private final DataSourceProvider dataSourceProvider;

    public IngredientRepository(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    public List<Ingredient> findAllIngredients() {
        String sql = "select id, name, price, category from ingredient";
        List<Ingredient> ingredients = new ArrayList<>();
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Ingredient ingredient = new Ingredient();
                ingredient.setId(rs.getInt("id"));
                ingredient.setName(rs.getString("name"));
                ingredient.setPrice(rs.getDouble("price"));
                ingredient.setCategory(CategoryEnum.valueOf(rs.getString("category")));
                ingredient.setStockMovementList(findStockMovementsByIngredientId(rs.getInt("id")));
                ingredients.add(ingredient);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ingredients;
    }

    public Ingredient findIngredientById(Integer id) {
        String sql = "select id, name, price, category from ingredient where id = ?";
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setId(rs.getInt("id"));
                    ingredient.setName(rs.getString("name"));
                    ingredient.setPrice(rs.getDouble("price"));
                    ingredient.setCategory(CategoryEnum.valueOf(rs.getString("category")));
                    ingredient.setStockMovementList(findStockMovementsByIngredientId(id));
                    return ingredient;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<StockMovement> findStockMovementsByIngredientId(Integer id) {
        String sql = "select id, quantity, unit, type, creation_datetime from stock_movement where id_ingredient = ?";
        List<StockMovement> stockMovementList = new ArrayList<>();
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement stockMovement = new StockMovement();
                    stockMovement.setId(rs.getInt("id"));
                    stockMovement.setType(MovementTypeEnum.valueOf(rs.getString("type")));
                    stockMovement.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());

                    StockValue stockValue = new StockValue();
                    stockValue.setQuantity(rs.getDouble("quantity"));
                    stockValue.setUnit(Unit.valueOf(rs.getString("unit")));
                    stockMovement.setValue(stockValue);

                    stockMovementList.add(stockMovement);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return stockMovementList;
    }

    // For stock value at time
    public double getStockValueAtTime(Integer ingredientId, Timestamp at, Unit unit) {
        String sql = "select sum(case when type = 'IN' then quantity else -quantity end) as total from stock_movement where id_ingredient = ? and creation_datetime <= ? and unit = ?";
        try (Connection conn = dataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ps.setTimestamp(2, at);
            ps.setString(3, unit.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0.0;
    }

    // Other methods like save, but for now, since endpoints are GET, maybe not needed yet.
}