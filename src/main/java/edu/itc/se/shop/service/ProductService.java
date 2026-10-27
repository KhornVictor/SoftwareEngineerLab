package edu.itc.se.shop.service;

import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.ProductRepository;
import edu.itc.se.shop.web.dto.CreateProduct;
import edu.itc.se.shop.web.dto.ProductDto;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto> search(String q) {
        if (q == null || q.isBlank()) {
            return productRepository.findAll().stream()
                    .map(ProductDto::from)
                    .toList();
        }
        return productRepository.findByNameContaining(q.trim()).stream()
                .map(ProductDto::from)
                .toList();
    }

    public ProductDto findById(long id) {
        return productRepository.findById(id)
                .map(ProductDto::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public ProductDto create(CreateProduct cmd) {
        Product product = new Product(null, cmd.name(), cmd.price(), cmd.stock());
        long id = productRepository.insert(product);
        return new ProductDto(id, cmd.name(), cmd.price(), cmd.stock());
    }

    public ProductDto updateStock(long id, int delta) {
        Product current = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (current.stock() + delta < 0) {
            throw new InsufficientStockException(id, current.stock(), delta);
        }

        productRepository.updateStock(id, delta);
        return new ProductDto(id, current.name(), current.price(), current.stock() + delta);
    }

    public void delete(long id) {
        int affected = productRepository.delete(id);
        if (affected == 0) {
            throw new ProductNotFoundException(id);
        }
    }

    @Transactional
    public void restock(long fromId, long toId, int qty) {
        try {
            int updatedFrom = productRepository.updateStock(fromId, -qty);
            if (updatedFrom == 0) {
                throw new IllegalStateException("Insufficient stock or source product not found: " + fromId);
            }
            int updatedTo = productRepository.updateStock(toId, qty);
            if (updatedTo == 0) {
                throw new IllegalStateException("Target product not found: " + toId);
            }
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("CHECK constraint failed during restock", e);
        }
    }
}
