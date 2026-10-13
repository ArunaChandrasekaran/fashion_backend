package com.fashion;

import com.fashion.entity.OrderStatus;
import com.fashion.entity.PaymentMethod;
import com.fashion.entity.ProductStatus;
import com.fashion.entity.ProductVariant;
import com.fashion.repository.ProductVariantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FashionApiTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ProductVariantRepository variantRepository;

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail("dup");
        register(email, "CustomerTest!23");
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Aanya Kapoor", email, "9876543210", "CustomerTest!23")))
                .andExpect(status().isConflict());
    }

    @Test
    void loginSucceedsAndFailsWithoutRevealingAccountState() throws Exception {
        String email = uniqueEmail("login");
        register(email, "CustomerTest!23");
        login(email, "CustomerTest!23")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").exists());
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void userCannotAccessAdminApis() throws Exception {
        String email = uniqueEmail("useradmin");
        register(email, "CustomerTest!23");
        MvcResult login = login(email, "CustomerTest!23").andReturn();
        mockMvc.perform(get("/api/admin/dashboard").session(session(login)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/products").with(csrf()).session(session(login))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDashboardReturnsKpis() throws Exception {
        MvcResult admin = login("admin@fashion.test", "AdminTest!23456").andReturn();
        mockMvc.perform(get("/api/admin/dashboard").session(session(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalOrders").isNumber())
                .andExpect(jsonPath("$.kpis.totalCustomers").isNumber())
                .andExpect(jsonPath("$.kpis.activeProducts").isNumber())
                .andExpect(jsonPath("$.recentOrders").isArray())
                .andExpect(jsonPath("$.sales").isArray())
                .andExpect(jsonPath("$.lowStock").isArray());
    }

    @Test
    void adminCanManageProductsAndUserCannotSeeInactive() throws Exception {
        MvcResult admin = login("admin@fashion.test", "AdminTest!23456").andReturn();
        JsonNode created = read(mockMvc.perform(post("/api/admin/products").with(csrf()).session(session(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Hidden Atelier Dress", "HIDDEN-SKU-" + UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isCreated())
                .andReturn());
        long id = created.get("id").asLong();
        mockMvc.perform(patch("/api/admin/products/" + id + "/status").with(csrf()).session(session(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerOwnsOrdersAndAddresses() throws Exception {
        String one = uniqueEmail("own1");
        String two = uniqueEmail("own2");
        register(one, "CustomerTest!23");
        register(two, "CustomerTest!23");
        MvcResult sessionOne = login(one, "CustomerTest!23").andReturn();
        MvcResult sessionTwo = login(two, "CustomerTest!23").andReturn();

        JsonNode address = read(mockMvc.perform(post("/api/addresses").with(csrf()).session(session(sessionOne))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson()))
                .andExpect(status().isCreated())
                .andReturn());
        mockMvc.perform(delete("/api/addresses/" + address.get("id").asLong()).with(csrf()).session(session(sessionTwo)))
                .andExpect(status().isNotFound());

        long variantId = firstInStockVariant();
        mockMvc.perform(post("/api/cart/items").with(csrf()).session(session(sessionOne))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variantId + ",\"quantity\":1}"))
                .andExpect(status().isOk());
        JsonNode order = read(mockMvc.perform(post("/api/orders/checkout").with(csrf()).session(session(sessionOne))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + address.get("id").asLong() + ",\"paymentMethod\":\"CASH_ON_DELIVERY\"}"))
                .andExpect(status().isCreated())
                .andReturn());
        mockMvc.perform(get("/api/orders/" + order.get("id").asLong()).session(session(sessionTwo)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cartRejectsQuantityAboveStock() throws Exception {
        String email = uniqueEmail("cart");
        register(email, "CustomerTest!23");
        MvcResult session = login(email, "CustomerTest!23").andReturn();
        long variantId = firstInStockVariant();
        mockMvc.perform(post("/api/cart/items").with(csrf()).session(session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variantId + ",\"quantity\":999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkoutCalculatesTotalsAndIsIdempotent() throws Exception {
        String email = uniqueEmail("chk");
        register(email, "CustomerTest!23");
        MvcResult session = login(email, "CustomerTest!23").andReturn();
        JsonNode address = read(mockMvc.perform(post("/api/addresses").with(csrf()).session(session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson()))
                .andExpect(status().isCreated()).andReturn());
        long variantId = firstInStockVariant();
        mockMvc.perform(post("/api/cart/items").with(csrf()).session(session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variantId + ",\"quantity\":1}"))
                .andExpect(status().isOk());
        String key = UUID.randomUUID().toString();
        String body = "{\"addressId\":" + address.get("id").asLong() + ",\"paymentMethod\":\"CASH_ON_DELIVERY\"}";
        JsonNode first = read(mockMvc.perform(post("/api/orders/checkout").with(csrf()).session(session(session))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.finalTotal").exists())
                .andExpect(jsonPath("$.payment.method").value("CASH_ON_DELIVERY"))
                .andReturn());
        JsonNode second = read(mockMvc.perform(post("/api/orders/checkout").with(csrf()).session(session(session))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());
        assertThat(second.get("orderNumber").asText()).isEqualTo(first.get("orderNumber").asText());
        assertThat(first.get("finalTotal").decimalValue())
                .isEqualByComparingTo(first.get("subtotal").decimalValue()
                        .subtract(first.get("discountAmount").decimalValue())
                        .add(first.get("shippingCharge").decimalValue()));
    }

    @Test
    void inventoryDoesNotOversellUnderConcurrentCheckout() throws Exception {
        ProductVariant found = variantRepository.findAll().stream()
                .filter(item -> item.getStockQuantity() > 0)
                .findFirst()
                .orElseThrow();
        found.setStockQuantity(1);
        ProductVariant variant = variantRepository.saveAndFlush(found);

        String emailA = uniqueEmail("raceA");
        String emailB = uniqueEmail("raceB");
        register(emailA, "CustomerTest!23");
        register(emailB, "CustomerTest!23");
        MvcResult sessionA = login(emailA, "CustomerTest!23").andReturn();
        MvcResult sessionB = login(emailB, "CustomerTest!23").andReturn();
        long addressA = read(mockMvc.perform(post("/api/addresses").with(csrf()).session(session(sessionA))
                .contentType(MediaType.APPLICATION_JSON).content(addressJson())).andReturn()).get("id").asLong();
        long addressB = read(mockMvc.perform(post("/api/addresses").with(csrf()).session(session(sessionB))
                .contentType(MediaType.APPLICATION_JSON).content(addressJson())).andReturn()).get("id").asLong();
        mockMvc.perform(post("/api/cart/items").with(csrf()).session(session(sessionA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + variant.getId() + ",\"quantity\":1}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/cart/items").with(csrf()).session(session(sessionB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + variant.getId() + ",\"quantity\":1}")).andExpect(status().isOk());

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger created = new AtomicInteger();
        Runnable checkoutA = () -> checkoutRace(sessionA, addressA, start, done, created);
        Runnable checkoutB = () -> checkoutRace(sessionB, addressB, start, done, created);
        Thread t1 = new Thread(checkoutA);
        Thread t2 = new Thread(checkoutB);
        t1.start();
        t2.start();
        start.countDown();
        done.await();
        assertThat(created.get()).isEqualTo(1);
        int remaining = variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity();
        assertThat(remaining).isZero();
    }

    @Test
    void cancellationRestoresStockAndBlocksIllegalTransitions() throws Exception {
        String email = uniqueEmail("cancel");
        register(email, "CustomerTest!23");
        MvcResult user = login(email, "CustomerTest!23").andReturn();
        MvcResult admin = login("admin@fashion.test", "AdminTest!23456").andReturn();
        JsonNode address = read(mockMvc.perform(post("/api/addresses").with(csrf()).session(session(user))
                .contentType(MediaType.APPLICATION_JSON).content(addressJson())).andReturn());
        long variantId = firstInStockVariant();
        int before = variantRepository.findById(variantId).orElseThrow().getStockQuantity();
        mockMvc.perform(post("/api/cart/items").with(csrf()).session(session(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + variantId + ",\"quantity\":1}")).andExpect(status().isOk());
        JsonNode order = read(mockMvc.perform(post("/api/orders/checkout").with(csrf()).session(session(user))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + address.get("id").asLong() + ",\"paymentMethod\":\"CASH_ON_DELIVERY\"}"))
                .andExpect(status().isCreated()).andReturn());
        mockMvc.perform(post("/api/orders/" + order.get("id").asLong() + "/cancel").with(csrf()).session(session(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(variantRepository.findById(variantId).orElseThrow().getStockQuantity()).isEqualTo(before);
        mockMvc.perform(patch("/api/admin/orders/" + order.get("id").asLong() + "/status").with(csrf()).session(session(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isBadRequest());
        assertThat(OrderStatus.CANCELLED.allowedTransitions()).isEmpty();
        assertThat(PaymentMethod.CASH_ON_DELIVERY).isNotNull();
        assertThat(ProductStatus.ACTIVE).isNotNull();
    }

    private void checkoutRace(MvcResult session, long addressId, CountDownLatch start, CountDownLatch done, AtomicInteger created) {
        try {
            start.await();
            int status = mockMvc.perform(post("/api/orders/checkout").with(csrf()).session(session(session))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"addressId\":" + addressId + ",\"paymentMethod\":\"CASH_ON_DELIVERY\"}"))
                    .andReturn().getResponse().getStatus();
            if (status == 201) {
                created.incrementAndGet();
            }
        } catch (Exception ignored) {
        } finally {
            done.countDown();
        }
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Test Customer", email, "9876543210", password)))
                .andExpect(status().isCreated());
    }

    private org.springframework.mock.web.MockHttpSession session(MvcResult result) {
        return (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession();
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long firstInStockVariant() {
        return variantRepository.findAll().stream()
                .filter(v -> v.isActive() && v.getStockQuantity() > 2)
                .map(ProductVariant::getId)
                .findFirst()
                .orElseThrow();
    }

    private String uniqueEmail(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8) + "@fashion.test";
    }

    private String registerJson(String name, String email, String phone, String password) {
        return """
                {"fullName":"%s","email":"%s","phone":"%s","password":"%s","confirmPassword":"%s"}
                """.formatted(name, email, phone, password, password);
    }

    private String addressJson() {
        return """
                {"fullName":"Aanya Kapoor","phone":"9876543210","line1":"14 Atelier Lane","line2":"Bandra",
                 "city":"Mumbai","state":"Maharashtra","postalCode":"400050","country":"India","defaultAddress":true}
                """;
    }

    private String productJson(String name, String sku) {
        return """
                {
                  "name":"%s",
                  "description":"A development-only catalogue dress.",
                  "categoryId":1,
                  "price":9999.00,
                  "discountPercent":0,
                  "sku":"%s",
                  "status":"ACTIVE",
                  "images":[{"url":"https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&w=800&q=80","altText":"%s","sortOrder":0,"primary":true}],
                  "variants":[{"size":"M","color":"Wine","sku":"%s-M-WINE","stockQuantity":4,"active":true}]
                }
                """.formatted(name, sku, name, sku);
    }
}
