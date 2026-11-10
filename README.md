# E-commerce Backend (Spring Boot)

REST API for a small online store: **JWT auth, products, cart, orders**.

**Tech:** Java 17, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA/Hibernate, Bean Validation, H2 (default) / MySQL, Maven.

## Features
- Register / login with BCrypt password hashing + JWT (stateless)
- Roles: `CUSTOMER`, `ADMIN`
- Products: public browse + search + pagination; admin create / update / soft-delete
- Cart: add, update quantity, remove, clear (stock validated)
- Checkout: cart -> order in one transaction, with **pessimistic row locking** so stock can't be oversold
- Orders: list, view, cancel (stock restored), admin can mark SHIPPED / DELIVERED
- Order items store a price/name snapshot, so old orders stay correct if a product changes
- Global exception handling with clean JSON errors

## Frontend (included)
A simple single-page shop UI (HTML/CSS/vanilla JS) is served by Spring Boot itself from
`src/main/resources/static/index.html`. After starting the app, open **http://localhost:8080** to:
browse/search products, register/login, add to cart, checkout, view/cancel orders.
Log in as the admin (`admin@shop.com` / `Admin@123`) to get an **Admin** page (add products,
update order status) and a Delete button on product cards.

## Run
```bash
mvn spring-boot:run
```
App starts on http://localhost:8080 with in-memory H2 (data resets on restart).
H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:ecommerce`, user `sa`, empty password).

Default admin (auto-created): `admin@shop.com` / `Admin@123` (change this before any real deployment).

### Use MySQL instead
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
Edit `src/main/resources/application-mysql.properties` with your DB user/password.
Set a strong secret: `export JWT_SECRET=<random string, 32+ chars>`

## API

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | - | Create account, returns JWT |
| POST | `/api/auth/login` | - | Login, returns JWT |
| GET | `/api/products?search=&page=0&size=10` | - | List / search products |
| GET | `/api/products/{id}` | - | Product details |
| POST | `/api/products` | ADMIN | Create product |
| PUT | `/api/products/{id}` | ADMIN | Update product |
| DELETE | `/api/products/{id}` | ADMIN | Soft-delete product |
| GET | `/api/cart` | USER | View cart |
| POST | `/api/cart/items` | USER | Add item `{productId, quantity}` |
| PUT | `/api/cart/items/{productId}` | USER | Set quantity `{quantity}` |
| DELETE | `/api/cart/items/{productId}` | USER | Remove item |
| DELETE | `/api/cart` | USER | Clear cart |
| POST | `/api/orders` | USER | Checkout (cart -> order) |
| GET | `/api/orders` | USER | My orders |
| GET | `/api/orders/{id}` | USER | One of my orders |
| PUT | `/api/orders/{id}/cancel` | USER | Cancel a PLACED order |
| PUT | `/api/orders/{id}/status` | ADMIN | `{ "status": "SHIPPED" }` or `DELIVERED` |

Send the token as: `Authorization: Bearer <token>`

## Quick test (curl)
```bash
# 1. register
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Abhay","email":"abhay@test.com","password":"secret123"}'

# 2. login and keep the token
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"abhay@test.com","password":"secret123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 3. browse products
curl "localhost:8080/api/products?search=mouse"

# 4. add to cart, view cart
curl -X POST localhost:8080/api/cart/items -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"productId":1,"quantity":2}'
curl localhost:8080/api/cart -H "Authorization: Bearer $TOKEN"

# 5. checkout
curl -X POST localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"

# 6. my orders
curl localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"
```

## Project structure
```
com.ecommerce
 ├── config       SecurityConfig, DataSeeder
 (resources/static/index.html = frontend)
 ├── controller   REST endpoints
 ├── service      business logic (transactions, stock rules)
 ├── repository   Spring Data JPA
 ├── entity       User, Product, Cart, CartItem, Order, OrderItem
 ├── dto          request/response records
 ├── security     JwtService, JwtAuthFilter
 └── exception    ApiException + global handler
```

## Ideas to extend
Swagger/OpenAPI, unit + integration tests, Docker, payment integration, refresh tokens, product images, reviews.
