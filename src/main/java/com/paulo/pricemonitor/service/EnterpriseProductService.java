package com.paulo.pricemonitor.service;

import com.paulo.pricemonitor.dto.CreateEnterpriseProductRequest;
import com.paulo.pricemonitor.dto.EnterpriseProductResponse;
import com.paulo.pricemonitor.dto.MlListingResponse;
import com.paulo.pricemonitor.entity.EnterpriseProduct;
import com.paulo.pricemonitor.entity.MlListing;
import com.paulo.pricemonitor.entity.User;
import com.paulo.pricemonitor.repository.EnterpriseProductRepository;
import com.paulo.pricemonitor.repository.MlListingRepository;
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
    private final MlListingRepository mlListingRepository;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preço MAP inválido.");
        }

        EnterpriseProduct product = EnterpriseProduct.builder()
                .ean(request.ean().trim())
                .productName(request.productName().trim())
                .marca(request.marca() != null ? request.marca().trim() : null)
                .mapPrice(request.mapPrice())
                .tolerancePercent(request.tolerancePercent() != null
                        ? request.tolerancePercent() : BigDecimal.ZERO)
                .user(user)
                .build();

        EnterpriseProduct saved = enterpriseProductRepository.save(product);
        return toResponse(saved);
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
        mlListingRepository.deleteByEnterpriseProductId(productId);
        enterpriseProductRepository.deleteById(productId);
    }

    // Todos os anúncios de um produto
    @Transactional(readOnly = true)
    public List<MlListingResponse> getListingsByProduct(Long userId, Long productId) {
        enterpriseProductRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
        return mlListingRepository.findByProductId(productId)
                .stream().map(this::toListingResponse).toList();
    }

    // Todos os anúncios do usuário
    @Transactional(readOnly = true)
    public List<MlListingResponse> getAllListings(Long userId) {
        return mlListingRepository.findByUserId(userId)
                .stream().map(this::toListingResponse).toList();
    }

    // Só violações
    @Transactional(readOnly = true)
    public List<MlListingResponse> getViolations(Long userId) {
        return mlListingRepository.findViolationsByUserId(userId)
                .stream().map(this::toListingResponse).toList();
    }

    @Transactional
    public void markAllAsSeen(Long userId) {
        mlListingRepository.markAllAsSeenByUserId(userId);
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private EnterpriseProductResponse toResponse(EnterpriseProduct p) {
        long total = mlListingRepository.countByProductId(p.getId());
        long violations = mlListingRepository.countViolationsByProductId(p.getId());
        return new EnterpriseProductResponse(
                p.getId(), p.getEan(), p.getProductName(), p.getMarca(),
                p.getMapPrice(), p.getTolerancePercent(), p.isActive(),
                p.getCreatedAt(), p.getLastCheckAt(), total, violations
        );
    }

    private MlListingResponse toListingResponse(MlListing l) {
        return new MlListingResponse(
                l.getId(),
                l.getEnterpriseProduct().getId(),
                l.getEnterpriseProduct().getProductName(),
                l.getEnterpriseProduct().getEan(),
                l.getEnterpriseProduct().getMarca(),
                l.getMlItemId(),
                l.getListingUrl(),
                l.getSellerName(),
                l.getSellerId(),
                l.getListingTitle(),
                l.getListedPrice(),
                l.getMapPrice(),
                l.getPercentBelow(),
                l.isViolation(),
                l.isSeen(),
                l.getDetectedAt()
        );
    }
}
