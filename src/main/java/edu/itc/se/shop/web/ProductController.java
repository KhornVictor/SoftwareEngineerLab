package edu.itc.se.shop.web;

import edu.itc.se.shop.service.ProductService;
import edu.itc.se.shop.web.dto.CreateProduct;
import edu.itc.se.shop.web.dto.ProductDto;
import edu.itc.se.shop.web.dto.UpdateStock;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductDto> list(@RequestParam(required = false) String q) {
        return service.search(q);
    }

    @GetMapping("/{id}")
    public ProductDto getById(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@RequestBody @Valid CreateProduct cmd) {
        ProductDto created = service.create(cmd);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @PatchMapping("/{id}/stock")
    public ProductDto updateStock(@PathVariable long id, @RequestBody UpdateStock body) {
        return service.updateStock(id, body.delta());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
