package com.example.FinanceTracker.mapper;

import com.example.FinanceTracker.dto.response.CategoryResponse;
import com.example.FinanceTracker.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

	public CategoryResponse toResponse(Category category) {
		return CategoryResponse.builder()
				.id(category.getId())
				.name(category.getName())
				.type(category.getType())
				.build();
	}
}
