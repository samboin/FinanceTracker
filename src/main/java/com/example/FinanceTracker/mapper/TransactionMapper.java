package com.example.FinanceTracker.mapper;

import com.example.FinanceTracker.dto.response.TransactionResponse;
import com.example.FinanceTracker.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

	public TransactionResponse toResponse(Transaction transaction) {
		return TransactionResponse.builder()
				.id(transaction.getId())
				.amount(transaction.getAmount())
				.description(transaction.getDescription())
				.type(transaction.getType())
				.transactionDate(transaction.getTransactionDate())
				.categoryId(transaction.getCategory() != null ? transaction.getCategory().getId() : null)
				.categoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
				.build();
	}
}
