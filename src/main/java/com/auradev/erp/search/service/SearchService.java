package com.auradev.erp.search.service;

import com.auradev.erp.billing.entity.Bill;
import com.auradev.erp.billing.entity.Customer;
import com.auradev.erp.billing.repository.BillRepository;
import com.auradev.erp.billing.repository.CustomerRepository;
import com.auradev.erp.catalog.entity.Category;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.repository.CategoryRepository;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.search.dto.GlobalSearchResponse;
import com.auradev.erp.search.dto.SearchHitDto;
import com.auradev.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ProductRepository productRepository;
    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(String q, int limit) {
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.isBlank()) {
            return new GlobalSearchResponse(List.of());
        }

        UUID tenantId = TenantContext.require();
        int perType = Math.max(3, Math.min(limit, 10));
        Pageable page = PageRequest.of(0, perType);
        List<SearchHitDto> hits = new ArrayList<>();

        for (Product product : productRepository.searchActive(trimmed, page).getContent()) {
            String category = product.getCategory() != null ? product.getCategory().getName() : "";
            String subtitle = category.isBlank() ? product.getSku() : product.getSku() + " · " + category;
            hits.add(new SearchHitDto(
                    "PRODUCT",
                    product.getId().toString(),
                    product.getName(),
                    subtitle,
                    product.getName()));
        }

        List<Bill> bills = billRepository.searchByBillNo(tenantId, trimmed, page).getContent();
        Map<UUID, Customer> customers = loadCustomers(bills);
        for (Bill bill : bills) {
            Customer customer = customers.get(bill.getCustomerId());
            hits.add(new SearchHitDto(
                    "BILL",
                    bill.getId().toString(),
                    bill.getBillNo(),
                    customer != null ? customer.getName() : "Customer",
                    bill.getBillNo()));
        }

        for (Category category : categoryRepository.searchActive(trimmed, page)) {
            hits.add(new SearchHitDto(
                    "CATEGORY",
                    category.getId().toString(),
                    category.getName(),
                    "Product category",
                    category.getName()));
        }

        return new GlobalSearchResponse(hits);
    }

    private Map<UUID, Customer> loadCustomers(List<Bill> bills) {
        List<UUID> ids = bills.stream().map(Bill::getCustomerId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return customerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c));
    }
}
