package com.example.FinanceTracker.controller;

import com.example.FinanceTracker.dto.request.CreateTransactionRequest;
import com.example.FinanceTracker.dto.response.TransactionResponse;
import com.example.FinanceTracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionService transactionService;

	@GetMapping
	public List<TransactionResponse> getAll() {
		return transactionService.findAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
		return transactionService.create(request);
	}
}
