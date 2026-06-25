package com.example.FinanceTracker.service;

import com.example.FinanceTracker.dto.request.CreateTransactionRequest;
import com.example.FinanceTracker.dto.request.UpdateTransactionRequest;
import com.example.FinanceTracker.dto.response.TransactionResponse;
import com.example.FinanceTracker.entity.Category;
import com.example.FinanceTracker.entity.Transaction;
import com.example.FinanceTracker.entity.TransactionType;
import com.example.FinanceTracker.exception.ResourceNotFoundException;
import com.example.FinanceTracker.mapper.TransactionMapper;
import com.example.FinanceTracker.repository.CategoryRepository;
import com.example.FinanceTracker.repository.TransactionRepository;
import com.example.FinanceTracker.repository.UserRepository;
import com.example.FinanceTracker.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

	private final TransactionRepository transactionRepository;
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final TransactionMapper transactionMapper;
	private final CurrentUserService currentUserService;

	@Transactional(readOnly = true)
	public List<TransactionResponse> findAll(TransactionType type, LocalDate from, LocalDate to) {
		Long userId = currentUserService.getCurrentUserId();
		return transactionRepository.findByFilters(userId, type, from, to).stream()
				.map(transactionMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public TransactionResponse findById(Long id) {
		return transactionMapper.toResponse(getTransactionOrThrow(id, currentUserService.getCurrentUserId()));
	}

	@Transactional
	public TransactionResponse create(CreateTransactionRequest request) {
		Transaction transaction = new Transaction();
		transaction.setUser(userRepository.getReferenceById(currentUserService.getCurrentUserId()));
		applyRequest(transaction, request.getAmount(), request.getDescription(),
				request.getType(), request.getTransactionDate(), request.getCategoryId(), currentUserService.getCurrentUserId());

		return transactionMapper.toResponse(transactionRepository.save(transaction));
	}

	@Transactional
	public TransactionResponse update(Long id, UpdateTransactionRequest request) {
		Long userId = currentUserService.getCurrentUserId();
		Transaction transaction = getTransactionOrThrow(id, userId);
		applyRequest(transaction, request.getAmount(), request.getDescription(),
				request.getType(), request.getTransactionDate(), request.getCategoryId(), userId);

		return transactionMapper.toResponse(transaction);
	}

	@Transactional
	public void delete(Long id) {
		Transaction transaction = getTransactionOrThrow(id, currentUserService.getCurrentUserId());
		transactionRepository.delete(transaction);
	}

	private void applyRequest(Transaction transaction, java.math.BigDecimal amount, String description,
			TransactionType type, LocalDate transactionDate, Long categoryId, Long userId) {
		transaction.setAmount(amount);
		transaction.setDescription(description);
		transaction.setType(type);
		transaction.setTransactionDate(transactionDate);
		transaction.setCategory(resolveCategory(categoryId, userId));
	}

	private Category resolveCategory(Long categoryId, Long userId) {
		if (categoryId == null) {
			return null;
		}
		return categoryRepository.findByIdAndUserId(categoryId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
	}

	private Transaction getTransactionOrThrow(Long id, Long userId) {
		return transactionRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
	}
}
