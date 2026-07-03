package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.CartRequest;
import com.example.freshfarm3.dto.response.CartResponse;
import com.example.freshfarm3.entity.*;
import com.example.freshfarm3.repository.BuyerRepository;
import com.example.freshfarm3.repository.CartItemRepository;
import com.example.freshfarm3.repository.CartRepository;
import com.example.freshfarm3.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final BuyerRepository buyerRepository;

    // ── ADD TO CART ──────────────────────────────────────────────
    @Transactional
    public CartResponse addToCart(String buyerEmail, CartRequest req) {

        Buyer buyer = getBuyer(buyerEmail);
        Product product = getProduct(req.getProductId());

        if (!product.hasStock(req.getQuantity())) {
            throw new RuntimeException("Insufficient stock for " + product.getName());
        }

        Cart cart = getOrCreateCart(buyer);

        Optional<CartItem> existingItem =
                cartItemRepository.findByCartAndProduct(cart, product);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + req.getQuantity();

            if (!product.hasStock(newQty)) {
                throw new RuntimeException("Not enough stock available");
            }

            item.setQuantity(newQty);
            item.setSubtotal(product.getPrice()
                    .multiply(BigDecimal.valueOf(newQty)));

            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(req.getQuantity());
            newItem.setPrice(product.getPrice());
            newItem.setSubtotal(product.getPrice()
                    .multiply(BigDecimal.valueOf(req.getQuantity())));

            cartItemRepository.save(newItem);
        }

        return buildCartResponse(cart);
    }

    // ── UPDATE QUANTITY ─────────────────────────────────────────
    @Transactional
    public CartResponse updateQuantity(String buyerEmail, Long cartItemId, int newQty) {

        if (newQty < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        validateCartOwnership(item, buyerEmail);

        Product product = item.getProduct();

        if (!product.hasStock(newQty)) {
            throw new RuntimeException("Only " + product.getStockQuantity() + " available");
        }

        item.setQuantity(newQty);
        item.setSubtotal(product.getPrice()
                .multiply(BigDecimal.valueOf(newQty)));

        cartItemRepository.save(item);

        return buildCartResponse(item.getCart());
    }

    // ── REMOVE ITEM ─────────────────────────────────────────────
    @Transactional
    public CartResponse removeItem(String buyerEmail, Long cartItemId) {

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        validateCartOwnership(item, buyerEmail);

        Cart cart = item.getCart();
        cartItemRepository.delete(item);

        return buildCartResponse(cart);
    }

    // ── CLEAR CART ───────────────────────────────────────────────
    @Transactional
    public void clearCart(String buyerEmail) {

        Buyer buyer = getBuyer(buyerEmail);

        cartRepository.findByBuyer(buyer).ifPresent(cart -> {
            cartItemRepository.deleteAll(cart.getItems());
        });
    }

    // ── VIEW CART ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public CartResponse getCart(String buyerEmail) {

        Buyer buyer = getBuyer(buyerEmail);
        Cart cart = getOrCreateCart(buyer);

        return buildCartResponse(cart);
    }

    // ── INTERNAL CART ENTITY ─────────────────────────────────────
    @Transactional(readOnly = true)
    public Cart getCartEntity(String buyerEmail) {

        Buyer buyer = getBuyer(buyerEmail);

        return cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new RuntimeException("Cart is empty"));
    }

    // ── HELPERS ──────────────────────────────────────────────────
    private Cart getOrCreateCart(Buyer buyer) {

        return cartRepository.findByBuyer(buyer)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setBuyer(buyer);
                    return cartRepository.save(cart);
                });
    }

    private Buyer getBuyer(String email) {

        return buyerRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
    }

    private Product getProduct(Long productId) {

        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    private void validateCartOwnership(CartItem item, String buyerEmail) {

        String ownerEmail = item.getCart()
                .getBuyer()
                .getUser()
                .getEmail();

        if (!ownerEmail.equals(buyerEmail)) {
            throw new RuntimeException("Unauthorized access");
        }
    }

    // ── BUILD RESPONSE ───────────────────────────────────────────
    private CartResponse buildCartResponse(Cart cart) {

        List<CartItem> items = cartItemRepository.findByCart(cart);

        List<CartResponse.CartItemResponseDto> itemDtos = items.stream()
                .map(item -> {

                    Product p = item.getProduct();
<<<<<<< HEAD
                    return CartResponse.CartItemResponseDto.builder()
                            .cartItemId(item.getId())
                            .productId(p.getId())
                            .productName(p.getName())
                            .price(item.getPrice())
                            .quantity(item.getQuantity())
                            .subtotal(item.getSubtotal())
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal total = itemDtos.stream()
                .map(CartResponse.CartItemResponseDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemDtos)
                .totalAmount(total)
                .build();
    }
<<<<<<< HEAD
}
=======
=======
>>>>>>> 66bc340 (Update)

                    return CartResponse.CartItemResponseDto.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .quantity(item.getQuantity())
                            .pricePerUnit(item.getPrice())
                            .subtotal(item.getSubtotal())
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal total = itemDtos.stream()
                .map(CartResponse.CartItemResponseDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(itemDtos)
                .totalAmount(total)
                .build();
    }
<<<<<<< HEAD
        return null;
    }
>>>>>>> 9fd0a7bf4402f08b8025c3f13c34dfb66b359e96
=======
}
>>>>>>> 66bc340 (Update)
