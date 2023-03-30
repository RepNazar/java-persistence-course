package com.bobocode.dao;

import com.bobocode.exception.DaoOperationException;
import com.bobocode.model.Product;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProductDaoImpl implements ProductDao {

    private final DataSource dataSource;

    public ProductDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Product product) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO products(name, producer, price, expiration_date)" +
                    "VALUES(?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, product.getName());
            preparedStatement.setString(2, product.getProducer());
            preparedStatement.setBigDecimal(3, product.getPrice());
            preparedStatement.setDate(4, Date.valueOf(product.getExpirationDate()));
            preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                product.setId(generatedKeys.getLong("id"));
            } else {
                throw new DaoOperationException(String.format("Cannot fetch id of updated product: %s", product));
            }
        } catch (SQLException e) {
            throw new DaoOperationException(String.format("Error saving product: %s", product));
        }
    }

    @Override
    public List<Product> findAll() {
        try (Connection connection = dataSource.getConnection()) {
            Statement statement = connection.createStatement();
            statement.executeQuery("SELECT * FROM products");
            ResultSet resultSet = statement.getResultSet();
            return getProducts(resultSet);
        } catch (SQLException e) {
            throw new DaoOperationException("Error retrieving list of products");
        }
    }

    private static List<Product> getProducts(ResultSet resultSet) throws SQLException {
        List<Product> products = new ArrayList<>();
        while (resultSet.next()) {
            Product product = new Product();
            product.setId(resultSet.getLong("id"));
            product.setName(resultSet.getString("name"));
            product.setProducer(resultSet.getString("producer"));
            product.setPrice(resultSet.getBigDecimal("price"));
            product.setExpirationDate(resultSet.getDate("expiration_date").toLocalDate());
            product.setCreationTime(resultSet.getObject("creation_time", LocalDateTime.class));
            products.add(product);
        }
        return products;
    }

    @Override
    public Product findOne(Long id) {
        Objects.requireNonNull(id);
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM products WHERE id = ?");
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            List<Product> products = getProducts(resultSet);
            if (products.isEmpty()) {
                throw new DaoOperationException("Error no product with id:" + id);
            }
            return products.get(0);
        } catch (SQLException e) {
            throw new DaoOperationException("Error finding product by id:" + id);
        }
    }

    @Override
    public void update(Product product) {
        if (product.getId() == null) {
            throw new DaoOperationException("Error product id is null");
        }
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE products " +
                    "SET name = ?, producer = ?, price = ?, expiration_date = ? " +
                    "WHERE id = ?");
            preparedStatement.setString(1, product.getName());
            preparedStatement.setString(2, product.getProducer());
            preparedStatement.setBigDecimal(3, product.getPrice());
            preparedStatement.setDate(4, Date.valueOf(product.getExpirationDate()));
            preparedStatement.setLong(5, product.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DaoOperationException(String.format("Error updating product: %s", product));
        }
    }

    @Override
    public void remove(Product product) {
        if (product.getId() == null) {
            throw new DaoOperationException("Error product id is null");
        }
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM products WHERE id = ?");
            preparedStatement.setLong(1, product.getId());
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new DaoOperationException(String.format("Error deleting product: %s", product));
        }
    }
}
