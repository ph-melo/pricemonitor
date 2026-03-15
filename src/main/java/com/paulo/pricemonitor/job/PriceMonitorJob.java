package com.paulo.pricemonitor.job;

import com.paulo.pricemonitor.service.PriceMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceMonitorJob {

    private final PriceMonitorService monitorService;

    // MVP: a cada 30 minutos
    @Scheduled(fixedDelayString = "PT30M")
    public void run() {
        monitorService.checkAllActiveProducts();
    }
}