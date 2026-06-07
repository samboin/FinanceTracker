package com.example.FinanceTracker.dto.response;

import com.example.FinanceTracker.entity.TransactionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {

	private Long id;
	private String name;
	private TransactionType type;
}
