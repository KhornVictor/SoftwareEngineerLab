package edu.itc.se.shop.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProduct(
        @NotBlank @Size(max = 80) String name,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @PositiveOrZero int stock
) {
}
