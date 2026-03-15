package com.paulo.pricemonitor.marketplace.mercadolivre;

/**
 * Representa os identificadores resolvidos de uma URL do Mercado Livre.
 *
 * catalogId -> ID do catálogo (/p/MLB...)
 * itemId    -> ID do anúncio (wid=MLB... ou item_id=MLB...)
 */
public record ResolvedIds(
        String catalogId,
        String itemId
) {
}