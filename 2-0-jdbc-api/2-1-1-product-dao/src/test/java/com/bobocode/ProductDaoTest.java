package com.bobocode;

import com.bobocode.dao.ProductDao;
import com.bobocode.dao.ProductDaoImpl;
import com.bobocode.exception.DaoOperationException;
import com.bobocode.model.Product;
import com.bobocode.util.JdbcUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductDaoTest {
    private static ProductDao productDao;
    private static DataSource dataSource;

    @BeforeAll
    static void init() throws SQLException {
        dataSource = JdbcUtil.createDefaultInMemoryH2DataSource();
        createAccountTable(dataSource);
        productDao = new ProductDaoImpl(dataSource);
    }

    private static void createAccountTable(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            Statement createTableStatement = connection.createStatement();
            createTableStatement.execute("CREATE TABLE IF NOT EXISTS products (\n" +
                    "  id            SERIAL NOT NULL,\n" +
                    "  name     VARCHAR(255) NOT NULL,\n" +
                    "  producer     VARCHAR(255) NOT NULL,\n" +
                    "  price       DECIMAL(19, 4),\n" +
                    "  expiration_date      TIMESTAMP NOT NULL,\n" +
                    "  creation_time TIMESTAMP NOT NULL DEFAULT now(),\n" +
                    "\n" +
                    "  CONSTRAINT products_pk PRIMARY KEY (id)\n" +
                    ");\n" +
                    "\n");
        }
    }

    private Product generateTestProduct() {
        return Product.builder()
                .name(RandomStringUtils.randomAlphabetic(10))
                .producer(RandomStringUtils.randomAlphabetic(20))
                .price(BigDecimal.valueOf(RandomUtils.nextInt(10, 100)))
                .expirationDate(LocalDate.ofYearDay(LocalDate.now().getYear() + RandomUtils.nextInt(1, 5),
                        RandomUtils.nextInt(1, 365)))
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("Save a product")
    void save() {

        Product fanta = createTestFantaProduct();
        // todo: check
        int productsCountBeforeInsert = findAllFromDataBase().size();
        productDao.save(fanta);
        List<Product> products = findAllFromDataBase();

        assertNotNull(fanta.getId());
        assertThat(productsCountBeforeInsert + 1).isEqualTo(products.size());
        assertTrue(products.contains(fanta));
    }

    private List<Product> findAllFromDataBase() {
        try (Connection connection = dataSource.getConnection()) {
            return findAllProducts(connection);
        } catch (SQLException e) {
            throw new DaoOperationException("Error finding all products", e);
        }
    }

    private List<Product> findAllProducts(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM products;");
        return collectToList(resultSet);
    }

    private List<Product> collectToList(ResultSet resultSet) throws SQLException {
        List<Product> products = new ArrayList<>();
        while (resultSet.next()) {
            Product product = parseRow(resultSet);
            products.add(product);
        }
        return products;
    }

    private Product parseRow(ResultSet resultSet) {
        try {
            return createFromResultSet(resultSet);
        } catch (SQLException e) {
            throw new DaoOperationException("Cannot parse row to create product instance", e);
        }
    }

    private Product createFromResultSet(ResultSet resultSet) throws SQLException {
        Product product = new Product();
        product.setId(resultSet.getLong("id"));
        product.setName(resultSet.getString("name"));
        product.setProducer(resultSet.getString("producer"));
        product.setPrice(resultSet.getBigDecimal("price"));
        product.setExpirationDate(resultSet.getDate("expiration_date").toLocalDate());
        product.setCreationTime(resultSet.getTimestamp("creation_time").toLocalDateTime());
        return product;
    }

    @Test
    @Order(2)
    @DisplayName("Save throws an exception when product ID is invalid")
    void saveInvalidProduct() {
        Product invalidTestProduct = createInvalidTestProduct();

        try {
            productDao.save(invalidTestProduct);
            fail("Exception wasn't thrown");
        } catch (Exception e) {
            assertThat(DaoOperationException.class).isEqualTo(e.getClass());
            assertThat(String.format("Error saving product: %s", invalidTestProduct)).isEqualTo(e.getMessage());
        }
    }

    private Product createTestFantaProduct() {
        return Product.builder()
                .name("Fanta")
                .producer("The Coca-Cola Company")
                .price(BigDecimal.valueOf(22))
                .expirationDate(LocalDate.of(2020, Month.APRIL, 14)).build();
    }

    private Product createInvalidTestProduct() {
        return Product.builder()
                .name("INVALID")
                .price(BigDecimal.valueOf(22))
                .expirationDate(LocalDate.of(2020, Month.APRIL, 14)).build();
    }


    @Test
    @Order(3)
    @DisplayName("Find all the products")
    void findAll() {
        List<Product> newProducts = createTestProductList();
        List<Product> oldProducts = productDao.findAll();
        //    todo: test
        newProducts.forEach(this::saveIntoDataBase);

        List<Product> products = productDao.findAll();

        assertTrue(products.containsAll(newProducts));
        assertTrue(products.containsAll(oldProducts));
        assertThat(oldProducts.size() + newProducts.size()).isEqualTo(products.size());

    }

    private void saveIntoDataBase(Product product) {
        Objects.requireNonNull(product);
        try (Connection connection = dataSource.getConnection()) {
            saveProduct(product, connection);
        } catch (SQLException e) {
            throw new DaoOperationException(String.format("Error saving product: %s", product), e);
        }
    }

    private void saveProduct(Product product, Connection connection) throws SQLException {
        PreparedStatement insertStatement = prepareInsertStatement(product, connection);
        insertStatement.executeUpdate();
        Long id = fetchGeneratedId(insertStatement);
        product.setId(id);
    }

    private PreparedStatement prepareInsertStatement(Product product, Connection connection) {
        try {
            PreparedStatement insertStatement = connection
                    .prepareStatement("INSERT INTO products(name, producer, price, expiration_date) VALUES (?, ?, ?, ?);",
                            PreparedStatement.RETURN_GENERATED_KEYS);
            fillProductStatement(product, insertStatement);
            return insertStatement;
        } catch (SQLException e) {
            throw new DaoOperationException(String.format("Cannot prepare statement for product: %s", product), e);
        }
    }

    private void fillProductStatement(Product product, PreparedStatement updateStatement) throws SQLException {
        updateStatement.setString(1, product.getName());
        updateStatement.setString(2, product.getProducer());
        updateStatement.setBigDecimal(3, product.getPrice());
        updateStatement.setDate(4, Date.valueOf(product.getExpirationDate()));
    }

    private Long fetchGeneratedId(PreparedStatement insertStatement) throws SQLException {
        ResultSet generatedKeys = insertStatement.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getLong(1);
        } else {
            throw new DaoOperationException("Can not obtain product ID");
        }
    }

    private List<Product> createTestProductList() {
        return List.of(
                Product.builder()
                        .name("Sprite")
                        .producer("The Coca-Cola Company")
                        .price(BigDecimal.valueOf(18))
                        .expirationDate(LocalDate.of(2020, Month.MARCH, 24)).build(),
                Product.builder()
                        .name("Cola light")
                        .producer("The Coca-Cola Company")
                        .price(BigDecimal.valueOf(21))
                        .expirationDate(LocalDate.of(2020, Month.JANUARY, 11)).build(),
                Product.builder()
                        .name("Snickers")
                        .producer("Mars Inc.")
                        .price(BigDecimal.valueOf(16))
                        .expirationDate(LocalDate.of(2019, Month.DECEMBER, 3)).build()
        );
    }

    @Test
    @Order(4)
    @DisplayName("Find a product by ID")
    void findById() {
        Product testProduct = generateTestProduct();

        // todo: test
        saveIntoDataBase(testProduct);

        Product product = productDao.findOne(testProduct.getId());

        assertThat(testProduct).isEqualTo(product);
        assertThat(testProduct.getName()).isEqualTo(product.getName());
        assertThat(testProduct.getProducer()).isEqualTo(product.getProducer());
        assertThat(testProduct.getPrice().setScale(2)).isEqualTo(product.getPrice().setScale(2));
        assertThat(testProduct.getExpirationDate()).isEqualTo(product.getExpirationDate());
    }

    @Test
    @Order(5)
    @DisplayName("Find throws an exception when a product ID doesn't exist")
    void findByNotExistingId() {
        long invalidId = -1L;
        try {
            productDao.findOne(invalidId);
            fail("Exception was't thrown");
        } catch (Exception e) {
            assertThat(DaoOperationException.class).isEqualTo(e.getClass());
            assertThat(String.format("Product with id = %d does not exist", invalidId)).isEqualTo(e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Update a product")
    void update() {
        Product testProduct = generateTestProduct();

        // todo: test
        saveIntoDataBase(testProduct);
        List<Product> productsBeforeUpdate = findAllFromDataBase();

        testProduct.setName("Updated name");
        testProduct.setProducer("Updated producer");
        testProduct.setPrice(BigDecimal.valueOf(666));
        testProduct.setExpirationDate(LocalDate.of(2020, Month.JANUARY, 1));
        productDao.update(testProduct);

//        todo: test
        List<Product> products = findAllFromDataBase();
//        todo: replace
        Product updatedProduct = findOneFromDatabase(testProduct.getId());

        assertThat(productsBeforeUpdate.size()).isEqualTo(products.size());
        assertTrue(completelyEquals(testProduct, updatedProduct));
        productsBeforeUpdate.remove(testProduct);
        products.remove(testProduct);
        assertTrue(deepEquals(productsBeforeUpdate, products));
    }

    private boolean completelyEquals(Product productBeforeUpdate, Product productAfterUpdate) {
        return productBeforeUpdate.getName().equals(productAfterUpdate.getName())
                && productBeforeUpdate.getProducer().equals(productAfterUpdate.getProducer())
                && productBeforeUpdate.getPrice().setScale(2).equals(productAfterUpdate.getPrice().setScale(2))
                && productBeforeUpdate.getExpirationDate().equals(productAfterUpdate.getExpirationDate());
    }

    private boolean deepEquals(List<Product> productsBeforeUpdate, List<Product> productsAfterUpdate) {
        return productsAfterUpdate.stream()
                .allMatch(product -> remainedTheSame(product, productsBeforeUpdate));
    }

    private boolean remainedTheSame(Product productAfterUpdate, List<Product> productsBeforeUpdate) {
        Product productBeforeUpdate = findById(productsBeforeUpdate, productAfterUpdate.getId());
        return completelyEquals(productAfterUpdate, productBeforeUpdate);
    }

    private Product findById(List<Product> products, Long id) {
        return products.stream().filter(p -> p.getId().equals(id)).findFirst().get();
    }

    private Product findOneFromDatabase(Long id) {
        Objects.requireNonNull(id);
        try (Connection connection = dataSource.getConnection()) {
            return findProductById(id, connection);
        } catch (SQLException e) {
            throw new DaoOperationException(String.format("Error finding product by id = %d", id), e);
        }
    }

    private Product findProductById(Long id, Connection connection) throws SQLException {
        PreparedStatement selectByIdStatement = prepareSelectByIdStatement(id, connection);
        ResultSet resultSet = selectByIdStatement.executeQuery();
        if (resultSet.next()) {
            return parseRow(resultSet);
        } else {
            throw new DaoOperationException(String.format("Product with id = %d does not exist", id));
        }
    }

    private PreparedStatement prepareSelectByIdStatement(Long id, Connection connection) {
        try {
            PreparedStatement selectByIdStatement = connection
                    .prepareStatement("SELECT * FROM products WHERE id = ?;");
            selectByIdStatement.setLong(1, id);
            return selectByIdStatement;
        } catch (SQLException e) {
            throw new DaoOperationException(String.format("Cannot prepare select by id statement for id = %d", id), e);
        }
    }

    @Test
    @Order(7)
    @DisplayName("Update throws an exception when a product hasn't been stored")
    void updateNotStored() {
        Product notStoredProduct = generateTestProduct();

        try {
            productDao.update(notStoredProduct);
            fail("Exception wasn't thrown");
        } catch (Exception e) {
            assertThat(DaoOperationException.class).isEqualTo(e.getClass());
            assertThat("Cannot find a product without ID").isEqualTo(e.getMessage());
        }
    }

    @Test
    @Order(8)
    @DisplayName("Update throws an exception when a product ID is invalid")
    void updateProductWithInvalidId() {
        Product testProduct = generateTestProduct();
        long invalidId = -1L;
        testProduct.setId(invalidId);

        try {
            productDao.update(testProduct);
            fail("Exception wasn't thrown");
        } catch (Exception e) {
            assertThat(DaoOperationException.class).isEqualTo(e.getClass());
            assertThat(String.format("Product with id = %d does not exist", invalidId)).isEqualTo(e.getMessage());
        }
    }

    @Test
    @Order(9)
    @DisplayName("Remove a product")
    void remove() {
        Product testProduct = generateTestProduct();

        // todo: test
        saveIntoDataBase(testProduct);
        List<Product> productsBeforeRemove = productDao.findAll();

        productDao.remove(testProduct);
        List<Product> products = productDao.findAll();

        assertThat(productsBeforeRemove.size() - 1).isEqualTo(products.size());
        assertFalse(products.contains(testProduct));
    }

    @Test
    @Order(10)
    @DisplayName("Remove throws an exception when a product hasn't been stored")
    void removeNotStored() {
        Product notStoredProduct = generateTestProduct();

        try {
            productDao.remove(notStoredProduct);
            fail("Exception wasn't thrown");
        } catch (Exception e) {
            assertThat(DaoOperationException.class).isEqualTo(e.getClass());
            assertThat("Cannot find a product without ID").isEqualTo(e.getMessage());
        }
    }

    @Test
    @Order(11)
    @DisplayName("Remove throws an exception when a product ID is invalid")
    void removeProductWithInvalidId() {
        Product testProduct = generateTestProduct();
        long invalidId = -1L;
        testProduct.setId(invalidId);

        try {
            productDao.remove(testProduct);
            fail("Exception wasn't thrown");
        } catch (Exception e) {
            assertThat(DaoOperationException.class).isEqualTo(e.getClass());
            assertThat(String.format("Product with id = %d does not exist", invalidId)).isEqualTo(e.getMessage());
        }
    }
}