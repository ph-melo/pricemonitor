package com.paulo.pricemonitor.job;

import com.paulo.pricemonitor.service.EnterpriseMonitorService;
import com.paulo.pricemonitor.service.PriceMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceMonitorJob {

    private final PriceMonitorService priceMonitorService;
    private final EnterpriseMonitorService enterpriseMonitorService;

    // Job Free/Pro — a cada 30 minutos
    @Scheduled(fixedDelayString = "PT30M")
    public void run() {
        log.info("==> [JOB] Iniciando verificação de preços Free/Pro...");
        priceMonitorService.checkAllActiveProducts();
        log.info("==> [JOB] Verificação Free/Pro concluída.");
    }

    // Job Enterprise — a cada 1 hora
    @Scheduled(fixedDelayString = "PT1H")
    public void runEnterprise() {
        log.info("==> [JOB] Iniciando verificação Enterprise de MAP...");
        enterpriseMonitorService.checkAllEnterpriseProducts();
        log.info("==> [JOB] Verificação Enterprise concluída.");
    }
}
