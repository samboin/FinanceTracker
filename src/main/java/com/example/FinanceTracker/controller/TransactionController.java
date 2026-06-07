package com.example.FinanceTracker.controller;

import com.example.FinanceTracker.dto.request.CreateTransactionRequest;
import com.example.FinanceTracker.dto.request.UpdateTransactionRequest;
import com.example.FinanceTracker.dto.response.TransactionResponse;
import com.example.FinanceTracker.entity.TransactionType;
import com.example.FinanceTracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionService transactionService;

	@GetMapping
	public List<TransactionResponse> getAll(
			@RequestParam(required = false) TransactionType type,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return transactionService.findAll(type, from, to);
	}

	@GetMapping("/{id}")
	public TransactionResponse getById(@PathVariable Long id) {
		return transactionService.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
		return transactionService.create(request);
	}

	@PutMapping("/{id}")
	public TransactionResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTransactionRequest request) {
		return transactionService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		transactionService.delete(id);
	}
}
