package edu.itc.se.shop.repository;

import edu.itc.se.shop.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private final JdbcClient jdbc;

    public ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Product> findAll() {
        return jdbc.sql("SELECT id, name, price, stock FROM product ORDER BY id")
                   .query(Product.class)
                   .list();
    }

    public Optional<Product> findById(long id) {
        return jdbc.sql("SELECT id, name, price, stock FROM product WHERE id = :id")
                   .param("id", id)
                   .query(Product.class)
                   .optional();
    }

    public List<Product> findByNameContaining(String pattern) {
        return jdbc.sql("SELECT id, name, price, stock FROM product WHERE LOWER(name) LIKE LOWER(:pattern) ORDER BY id")
                   .param("pattern", "%" + pattern + "%")
                   .query(Product.class)
                   .list();
    }

    public long insert(Product p) {
        var keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO product(name, price, stock) VALUES (:name, :price, :stock)")
            .paramSource(p)
            .update(keys);
        Number key = keys.getKey();
        return key != null ? key.longValue() : 0L;
    }

    public int updateStock(long id, int delta) {
        return jdbc.sql("UPDATE product SET stock = stock + :delta WHERE id = :id AND stock + :delta >= 0")
                   .param("id", id)
                   .param("delta", delta)
                   .update();
    }

    public int delete(long id) {
        return jdbc.sql("DELETE FROM product WHERE id = :id")
                   .param("id", id)
                   .update();
    }
}
