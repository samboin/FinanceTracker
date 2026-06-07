package com.example.FinanceTracker.dto.request;

import com.example.FinanceTracker.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryRequest {

	@NotBlank
	private String name;

	@NotNull
	private TransactionType type;
}
