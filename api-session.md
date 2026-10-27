# API Session: Product REST API

This document records HTTP requests and responses exercising all endpoints of `/api/products`, including success and error scenarios.

---

## 1. GET /api/products (List All Products)

### Request
```bash
curl -i -X GET http://localhost:8080/api/products
```

### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json
Transfer-Encoding: chunked

[
  {"id":1,"name":"Laptop","price":999.99,"stock":10},
  {"id":2,"name":"Mouse","price":25.50,"stock":50},
  {"id":3,"name":"Keyboard","price":75.00,"stock":30},
  {"id":4,"name":"Monitor","price":199.99,"stock":15},
  {"id":5,"name":"Headphones","price":49.99,"stock":20}
]
```

---

## 2. GET /api/products?q={query} (Filter by Name Case-Insensitive)

### Request
```bash
curl -i -X GET "http://localhost:8080/api/products?q=board"
```

### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json
Transfer-Encoding: chunked

[
  {"id":3,"name":"Keyboard","price":75.00,"stock":30}
]
```

---

## 3. GET /api/products/{id} (Find By ID - Success)

### Request
```bash
curl -i -X GET http://localhost:8080/api/products/1
```

### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json
Transfer-Encoding: chunked

{"id":1,"name":"Laptop","price":999.99,"stock":10}
```

---

## 4. GET /api/products/{id} (Not Found - 404)

### Request
```bash
curl -i -X GET http://localhost:8080/api/products/999
```

### Response
```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json
Transfer-Encoding: chunked

{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product 999 not found",
  "instance": "/api/products/999"
}
```

---

## 5. POST /api/products (Create Product - 201 Created)

### Request
```bash
curl -i -X POST http://localhost:8080/api/products \
     -H "Content-Type: application/json" \
     -d '{"name":"USB-C cable","price":7.90,"stock":25}'
```

### Response
```http
HTTP/1.1 201 Created
Location: /api/products/6
Content-Type: application/json
Transfer-Encoding: chunked

{"id":6,"name":"USB-C cable","price":7.90,"stock":25}
```

---

## 6. POST /api/products (Validation Failure - 400 Bad Request)

### Request
```bash
curl -i -X POST http://localhost:8080/api/products \
     -H "Content-Type: application/json" \
     -d '{"name":"","price":0,"stock":-1}'
```

### Response
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
Transfer-Encoding: chunked

{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/products",
  "errors": [
    {
      "field": "name",
      "message": "must not be blank"
    },
    {
      "field": "price",
      "message": "must be greater than or equal to 0.01"
    },
    {
      "field": "stock",
      "message": "must be greater than or equal to 0"
    }
  ]
}
```

---

## 7. POST /api/products (Duplicate Name - 409 Conflict)

### Request
```bash
curl -i -X POST http://localhost:8080/api/products \
     -H "Content-Type: application/json" \
     -d '{"name":"Laptop","price":1200.00,"stock":5}'
```

### Response
```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json
Transfer-Encoding: chunked

{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "A product with this name already exists",
  "instance": "/api/products"
}
```

---

## 8. PATCH /api/products/{id}/stock (Update Stock - 200 OK)

### Request
```bash
curl -i -X PATCH http://localhost:8080/api/products/6/stock \
     -H "Content-Type: application/json" \
     -d '{"delta":-5}'
```

### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json
Transfer-Encoding: chunked

{"id":6,"name":"USB-C cable","price":7.90,"stock":20}
```

---

## 9. PATCH /api/products/{id}/stock (Insufficient Stock - 409 Conflict)

### Request
```bash
curl -i -X PATCH http://localhost:8080/api/products/6/stock \
     -H "Content-Type: application/json" \
     -d '{"delta":-30}'
```

### Response
```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json
Transfer-Encoding: chunked

{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Product 6 has 20 units, cannot remove 30",
  "instance": "/api/products/6/stock"
}
```

---

## 10. DELETE /api/products/{id} (Delete - 204 No Content)

### Request
```bash
curl -i -X DELETE http://localhost:8080/api/products/6
```

### Response
```http
HTTP/1.1 204 No Content
```

---

## 11. DELETE /api/products/{id} (Delete Non-Existent - 404 Not Found)

### Request
```bash
curl -i -X DELETE http://localhost:8080/api/products/6
```

### Response
```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json
Transfer-Encoding: chunked

{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product 6 not found",
  "instance": "/api/products/6"
}
```
