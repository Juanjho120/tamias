package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.PaymentToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaymentReadOnlyToolService {

    private final PaymentToolRepository repository;

    public PaymentReadOnlyToolService(PaymentToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer paymentSummary(String userQuestion) {
        return repository.paymentSummary(userQuestion);
    }

    public AiToolAnswer paymentSearch(String userQuestion) {
        return repository.paymentSearch(userQuestion);
    }

    public AiToolAnswer recentPayments() {
        return repository.recentPayments();
    }

    public AiToolAnswer paymentsByCategory(String userQuestion) {
        return repository.paymentsByCategory(userQuestion);
    }

    public AiToolAnswer paymentsByMethod(String userQuestion) {
        return repository.paymentsByMethod(userQuestion);
    }

    public AiToolAnswer paymentsByProperty(String userQuestion) {
        return repository.paymentsByProperty(userQuestion);
    }

    public AiToolAnswer paymentMonthlyTotals() {
        return repository.paymentMonthlyTotals();
    }

    public AiToolAnswer highestPayments() {
        return repository.highestPayments();
    }

    public AiToolAnswer paymentImagesSummary() {
        return repository.paymentImagesSummary();
    }

    public AiToolAnswer paymentsWithoutCategory() {
        return repository.paymentsWithoutCategory();
    }

    public AiToolAnswer paymentCategories() {
        return repository.paymentCategories();
    }
}
