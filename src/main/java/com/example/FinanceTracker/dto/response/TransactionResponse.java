package com.example.FinanceTracker.dto.response;

import com.example.FinanceTracker.entity.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class TransactionResponse {

	private Long id;
	private BigDecimal amount;
	private String description;
	private TransactionType type;
	private LocalDate transactionDate;
	private Long categoryId;
	private String categoryName;
}
