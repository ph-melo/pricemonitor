package com.paulo.pricemonitor.service;

import com.paulo.pricemonitor.dto.CreateEnterpriseProductRequest;
import com.paulo.pricemonitor.dto.EnterpriseProductResponse;
import com.paulo.pricemonitor.dto.PriceViolationResponse;
import com.paulo.pricemonitor.entity.EnterpriseProduct;
import com.paulo.pricemonitor.entity.User;
import com.paulo.pricemonitor.entity.UserPlan;
import com.paulo.pricemonitor.repository.EnterpriseProductRepository;
import com.paulo.pricemonitor.repository.PriceViolationRepository;
import com.paulo.pricemonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnterpriseProductService {

    private final EnterpriseProductRepository enterpriseProductRepository;
    private final PriceViolationRepository priceViolationRepository;
    private final UserRepository userRepository;

    @Transactional
    public EnterpriseProductResponse create(CreateEnterpriseProductRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (!user.getPlan().isEnterprise()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este recurso é exclusivo do Plano Enterprise.");
        }

        if (enterpriseProductRepository.existsByUserIdAndEan(user.getId(), request.ean())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este EAN já está sendo monitorado.");
        }

        if (request.mapPrice() == null || request.mapPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Preço MAP inválido.");
        }

        EnterpriseProduct product = EnterpriseProduct.builder()
                .ean(request.ean().trim())
                .productName(request.productName().trim())
                .mapPrice(request.mapPrice())
                .tolerancePercent(request.tolerancePercent() != null
                        ? request.tolerancePercent() : BigDecimal.ZERO)
                .user(user)
                .build();

        return toResponse(enterpriseProductRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<EnterpriseProductResponse> listByUser(Long userId) {
        return enterpriseProductRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long productId) {
        enterpriseProductRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));

        priceViolationRepository.deleteByEnterpriseProductId(productId);
        enterpriseProductRepository.deleteById(productId);
    }

    @Transactional(readOnly = true)
    public List<PriceViolationResponse> getViolations(Long userId) {
        return priceViolationRepository.findByUserId(userId)
                .stream()
                .map(this::toViolationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PriceViolationResponse> getUnseenViolations(Long userId) {
        return priceViolationRepository.findUnseenByUserId(userId)
                .stream()
                .map(this::toViolationResponse)
                .toList();
    }

    @Transactional
    public void markAllViolationsAsSeen(Long userId) {
        priceViolationRepository.markAllAsSeenByUserId(userId);
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private EnterpriseProductResponse toResponse(EnterpriseProduct p) {
        return new EnterpriseProductResponse(
                p.getId(),
                p.getEan(),
                p.getProductName(),
                p.getMapPrice(),
                p.getTolerancePercent(),
                p.isActive(),
                p.getCreatedAt(),
                p.getLastCheckAt()
        );
    }

    private PriceViolationResponse toViolationResponse(
            com.paulo.pricemonitor.entity.PriceViolation v) {
        return new PriceViolationResponse(
                v.getId(),
                v.getEnterpriseProduct().getId(),
                v.getEnterpriseProduct().getProductName(),
                v.getEnterpriseProduct().getEan(),
                v.getMlItemId(),
                v.getListingUrl(),
                v.getSellerName(),
                v.getListingTitle(),
                v.getListedPrice(),
                v.getMapPrice(),
                v.getPercentBelow(),
                v.isSeen(),
                v.getDetectedAt()
        );
    }
}
