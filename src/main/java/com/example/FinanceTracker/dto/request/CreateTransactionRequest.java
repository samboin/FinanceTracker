package com.example.FinanceTracker.dto.request;

import com.example.FinanceTracker.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateTransactionRequest {

	@NotNull
	@Positive
	private BigDecimal amount;

	@NotBlank
	private String description;

	@NotNull
	private TransactionType type;

	@NotNull
	private LocalDate transactionDate;

	private Long categoryId;
}
