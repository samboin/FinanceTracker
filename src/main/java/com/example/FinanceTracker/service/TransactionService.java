package com.example.FinanceTracker.service;

import com.example.FinanceTracker.dto.request.CreateTransactionRequest;
import com.example.FinanceTracker.dto.response.TransactionResponse;
import com.example.FinanceTracker.entity.Category;
import com.example.FinanceTracker.entity.Transaction;
import com.example.FinanceTracker.repository.CategoryRepository;
import com.example.FinanceTracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

	private final TransactionRepository transactionRepository;
	private final CategoryRepository categoryRepository;

	@Transactional(readOnly = true)
	public List<TransactionResponse> findAll() {
		return transactionRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public TransactionResponse create(CreateTransactionRequest request) {
		Transaction transaction = new Transaction();
		transaction.setAmount(request.getAmount());
		transaction.setDescription(request.getDescription());
		transaction.setType(request.getType());
		transaction.setTransactionDate(request.getTransactionDate());

		if (request.getCategoryId() != null) {
			Category category = categoryRepository.findById(request.getCategoryId())
					.orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));
			transaction.setCategory(category);
		}

		return toResponse(transactionRepository.save(transaction));
	}

	private TransactionResponse toResponse(Transaction transaction) {
		return TransactionResponse.builder()
				.id(transaction.getId())
				.amount(transaction.getAmount())
				.description(transaction.getDescription())
				.type(transaction.getType())
				.transactionDate(transaction.getTransactionDate())
				.categoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
				.build();
	}
}
