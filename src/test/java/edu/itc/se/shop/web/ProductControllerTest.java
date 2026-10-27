package edu.itc.se.shop.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.itc.se.shop.service.ProductNotFoundException;
import edu.itc.se.shop.service.ProductService;
import edu.itc.se.shop.web.dto.CreateProduct;
import edu.itc.se.shop.web.dto.ProductDto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@Import(ApiExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService service;

    @Test
    void listProductsReturns200WithList() throws Exception {
        when(service.search(null)).thenReturn(List.of(
                new ProductDto(1L, "Laptop", new BigDecimal("999.99"), 10),
                new ProductDto(2L, "Mouse", new BigDecimal("25.50"), 50)
        ));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[1].name").value("Mouse"));
    }

    @Test
    void getProductByIdNotFoundReturns404() throws Exception {
        when(service.findById(999L)).thenThrow(new ProductNotFoundException(999L));

        mvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product 999 not found"));
    }

    @Test
    void createWithInvalidBodyReturns400WithTwoFieldErrors() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "price": 0, "stock": 10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void createWithInvalidBodyReturns400WithThreeFieldErrors() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "price": 0, "stock": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void createProductReturns201WithLocationHeader() throws Exception {
        ProductDto created = new ProductDto(6L, "USB-C cable", new BigDecimal("7.90"), 25);
        when(service.create(any(CreateProduct.class))).thenReturn(created);

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "USB-C cable", "price": 7.90, "stock": 25}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/6"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.name").value("USB-C cable"))
                .andExpect(jsonPath("$.price").value(7.90))
                .andExpect(jsonPath("$.stock").value(25));
    }
}
