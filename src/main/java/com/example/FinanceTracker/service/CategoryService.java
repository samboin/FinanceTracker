package com.example.FinanceTracker.service;

import com.example.FinanceTracker.dto.request.CreateCategoryRequest;
import com.example.FinanceTracker.dto.request.UpdateCategoryRequest;
import com.example.FinanceTracker.dto.response.CategoryResponse;
import com.example.FinanceTracker.entity.Category;
import com.example.FinanceTracker.exception.BusinessRuleException;
import com.example.FinanceTracker.exception.DuplicateResourceException;
import com.example.FinanceTracker.exception.ResourceNotFoundException;
import com.example.FinanceTracker.mapper.CategoryMapper;
import com.example.FinanceTracker.repository.CategoryRepository;
import com.example.FinanceTracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final TransactionRepository transactionRepository;
	private final CategoryMapper categoryMapper;

	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		return categoryRepository.findAll().stream()
				.map(categoryMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse findById(Long id) {
		return categoryMapper.toResponse(getCategoryOrThrow(id));
	}

	@Transactional
	public CategoryResponse create(CreateCategoryRequest request) {
		if (categoryRepository.existsByName(request.getName())) {
			throw new DuplicateResourceException("Category already exists with name: " + request.getName());
		}

		Category category = new Category();
		category.setName(request.getName());
		category.setType(request.getType());

		return categoryMapper.toResponse(categoryRepository.save(category));
	}

	@Transactional
	public CategoryResponse update(Long id, UpdateCategoryRequest request) {
		Category category = getCategoryOrThrow(id);

		if (categoryRepository.existsByNameAndIdNot(request.getName(), id)) {
			throw new DuplicateResourceException("Category already exists with name: " + request.getName());
		}

		category.setName(request.getName());
		category.setType(request.getType());

		return categoryMapper.toResponse(category);
	}

	@Transactional
	public void delete(Long id) {
		Category category = getCategoryOrThrow(id);

		if (transactionRepository.existsByCategoryId(id)) {
			throw new BusinessRuleException(
					"Category '%s' cannot be deleted because it has linked transactions".formatted(category.getName())
			);
		}

		categoryRepository.delete(category);
	}

	private Category getCategoryOrThrow(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category", id));
	}
}
