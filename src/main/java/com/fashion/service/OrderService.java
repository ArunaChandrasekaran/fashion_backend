package com.fashion.service;

import com.fashion.dto.CartDtos.CartResponse;
import com.fashion.dto.CartDtos.CartTotals;
import com.fashion.dto.OrderDtos.AddressInput;
import com.fashion.dto.OrderDtos.CheckoutRequest;
import com.fashion.dto.OrderDtos.OrderDetailResponse;
import com.fashion.dto.OrderDtos.OrderSummaryResponse;
import com.fashion.dto.PageResponse;
import com.fashion.entity.Address;
import com.fashion.entity.AppUser;
import com.fashion.entity.Cart;
import com.fashion.entity.CartItem;
import com.fashion.entity.Order;
import com.fashion.entity.OrderItem;
import com.fashion.entity.OrderStatus;
import com.fashion.entity.OrderStatusHistory;
import com.fashion.entity.Payment;
import com.fashion.entity.PaymentMethod;
import com.fashion.entity.PaymentStatus;
import com.fashion.entity.Product;
import com.fashion.entity.ProductVariant;
import com.fashion.exception.ApiException;
import com.fashion.repository.AddressRepository;
import com.fashion.repository.CartRepository;
import com.fashion.repository.OrderRepository;
import com.fashion.repository.ProductVariantRepository;
import com.fashion.security.SecurityUtils;
import com.fashion.util.Money;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final SecurityUtils securityUtils;
    private final DtoMapper mapper;
    private final PricingService pricingService;

    public OrderService(OrderRepository orderRepository,
                        ProductVariantRepository variantRepository,
                        AddressRepository addressRepository,
                        CartRepository cartRepository,
                        CartService cartService,
                        SecurityUtils securityUtils,
                        DtoMapper mapper,
                        PricingService pricingService) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.securityUtils = securityUtils;
        this.mapper = mapper;
        this.pricingService = pricingService;
    }

    @Transactional
    public OrderDetailResponse checkout(CheckoutRequest request, String idempotencyKey) {
        AppUser user = securityUtils.requireUser();
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Order order = existing.get();
                if (!order.getUser().getId().equals(user.getId())) {
                    throw ApiException.forbidden("You cannot reuse this checkout request");
                }
                return mapper.toOrderDetail(order, order.getStatus().customerCanCancel());
            }
        }

        Cart cart = cartService.requireCart(false);
        CartResponse validated = cartService.revalidate(cart);
        if (validated.items().isEmpty()) {
            throw ApiException.validation("Your bag is empty");
        }
        if (!validated.notices().isEmpty()) {
            throw ApiException.validation("Your bag changed. Review items before placing the order.");
        }

        AddressInput shipping = resolveShipping(user.getId(), request);
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(nextOrderNumber());
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentMethod(request.paymentMethod() == null ? PaymentMethod.CASH_ON_DELIVERY : request.paymentMethod());
        if (order.getPaymentMethod() != PaymentMethod.CASH_ON_DELIVERY) {
            throw ApiException.validation("Only Cash on Delivery is available at this time");
        }
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShipFullName(shipping.fullName());
        order.setShipPhone(shipping.phone());
        order.setShipLine1(shipping.line1());
        order.setShipLine2(shipping.line2());
        order.setShipCity(shipping.city());
        order.setShipState(shipping.state());
        order.setShipPostalCode(shipping.postalCode());
        order.setShipCountry(StringUtils.hasText(shipping.country()) ? shipping.country() : "India");
        order.setIdempotencyKey(StringUtils.hasText(idempotencyKey) ? idempotencyKey : null);

        CartTotals running = null;
        for (CartItem cartItem : List.copyOf(cart.getItems())) {
            ProductVariant variant = variantRepository.findByIdForUpdate(cartItem.getVariant().getId())
                    .orElseThrow(() -> ApiException.validation("A selected item is no longer available"));
            if (!variant.isActive() || variant.getStockQuantity() < cartItem.getQuantity()) {
                throw ApiException.conflict("Insufficient stock for " + variant.getSku());
            }
            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            Product product = variant.getProduct();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);
            orderItem.setProductName(product.getName());
            orderItem.setSku(variant.getSku());
            orderItem.setSize(variant.getSize());
            orderItem.setColor(variant.getColor());
            orderItem.setImageUrl(mapper.primaryImage(product));
            orderItem.setUnitPrice(Money.scale(product.getPrice()));
            orderItem.setDiscountPercent(Money.scale(product.getDiscountPercent()));
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setLineTotal(Money.lineTotal(product.getPrice(), product.getDiscountPercent(), cartItem.getQuantity()));
            order.getItems().add(orderItem);
        }

        running = pricingService.totals(
                order.getItems().stream().map(i -> i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity()))).reduce(Money.ZERO, java.math.BigDecimal::add),
                order.getItems().stream().map(i -> Money.discountAmount(i.getUnitPrice(), i.getDiscountPercent(), i.getQuantity())).reduce(Money.ZERO, java.math.BigDecimal::add),
                false
        );
        order.setSubtotal(running.subtotal());
        order.setDiscountAmount(running.discountAmount());
        order.setShippingCharge(running.shippingCharge());
        order.setFinalTotal(running.finalTotal());

        appendHistory(order, null, OrderStatus.PLACED, user, "Order placed");
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(order.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(order.getFinalTotal());
        payment.setProvider("COD");
        order.setPayment(payment);

        try {
            order = orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException ex) {
            if (StringUtils.hasText(idempotencyKey)) {
                return orderRepository.findByIdempotencyKey(idempotencyKey)
                        .map(o -> mapper.toOrderDetail(o, o.getStatus().customerCanCancel()))
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }

        cart.getItems().clear();
        cart.touch();
        cartRepository.save(cart);
        return mapper.toOrderDetail(order, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> myOrders(int page, int size) {
        AppUser user = securityUtils.requireUser();
        return PageResponse.from(orderRepository.findByUserId(
                        user.getId(),
                        PageRequest.of(page, Math.min(size, 40), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(order -> mapper.toOrderSummary(order, order.getStatus().customerCanCancel())));
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse myOrder(Long id) {
        AppUser user = securityUtils.requireUser();
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        hydrate(order);
        return mapper.toOrderDetail(order, order.getStatus().customerCanCancel());
    }

    @Transactional
    public OrderDetailResponse cancelMine(Long id) {
        AppUser user = securityUtils.requireUser();
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getStatus().customerCanCancel()) {
            throw ApiException.validation("This order can no longer be cancelled");
        }
        applyStatus(order, OrderStatus.CANCELLED, user, "Cancelled by customer");
        return mapper.toOrderDetail(order, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> adminSearch(String q, OrderStatus status, java.time.Instant from, java.time.Instant to, int page, int size) {
        java.time.Instant start = from == null ? java.time.Instant.EPOCH : from;
        java.time.Instant end = to == null ? java.time.Instant.now().plusSeconds(86400) : to;
        return PageResponse.from(orderRepository.searchAdmin(
                        q == null ? "" : q.trim(),
                        status != null,
                        status == null ? OrderStatus.PLACED : status,
                        start,
                        end,
                        PageRequest.of(page, Math.min(size, 40), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(order -> mapper.toOrderSummary(order, order.getStatus().adminCanCancel())));
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse adminDetail(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        hydrate(order);
        return mapper.toOrderDetail(order, order.getStatus().adminCanCancel());
    }

    @Transactional
    public OrderDetailResponse adminUpdateStatus(Long id, OrderStatus next, String notes) {
        AppUser admin = securityUtils.requireUser();
        Order order = orderRepository.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        applyStatus(order, next, admin, notes);
        hydrate(order);
        return mapper.toOrderDetail(order, order.getStatus().adminCanCancel());
    }

    private void applyStatus(Order order, OrderStatus next, AppUser actor, String notes) {
        if (!order.getStatus().allowedTransitions().contains(next)) {
            throw ApiException.validation("Cannot change status from " + order.getStatus() + " to " + next);
        }
        OrderStatus previous = order.getStatus();
        order.setStatus(next);
        order.touch();
        if (next == OrderStatus.CANCELLED) {
            restoreStock(order);
            if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.PENDING) {
                order.getPayment().setStatus(PaymentStatus.REFUNDED);
            }
        }
        if (next == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY && order.getPayment() != null) {
            order.getPayment().setStatus(PaymentStatus.PAID);
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        appendHistory(order, previous, next, actor, notes);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getVariant() == null) {
                continue;
            }
            ProductVariant variant = variantRepository.findByIdForUpdate(item.getVariant().getId()).orElse(null);
            if (variant != null) {
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            }
        }
    }

    private void appendHistory(Order order, OrderStatus from, OrderStatus to, AppUser actor, String notes) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actor);
        history.setNotes(notes);
        order.getStatusHistory().add(history);
    }

    private AddressInput resolveShipping(Long userId, CheckoutRequest request) {
        if (request.addressId() != null) {
            Address address = addressRepository.findByIdAndUserId(request.addressId(), userId)
                    .orElseThrow(() -> ApiException.notFound("Shipping address not found"));
            return new AddressInput(address.getFullName(), address.getPhone(), address.getLine1(), address.getLine2(),
                    address.getCity(), address.getState(), address.getPostalCode(), address.getCountry());
        }
        if (request.address() == null) {
            throw ApiException.validation("Choose or enter a shipping address");
        }
        return request.address();
    }

    private String nextOrderNumber() {
        String candidate;
        do {
            candidate = "FASH-" + DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now()) + "-"
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
        } while (orderRepository.existsByOrderNumber(candidate));
        return candidate;
    }

    private void hydrate(Order order) {
        order.getItems().size();
        order.getStatusHistory().size();
        if (order.getPayment() != null) {
            order.getPayment().getStatus();
        }
        order.getUser().getEmail();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
