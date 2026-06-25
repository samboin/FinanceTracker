package com.example.FinanceTracker.service;

import com.example.FinanceTracker.dto.request.CreateCategoryRequest;
import com.example.FinanceTracker.dto.request.UpdateCategoryRequest;
import com.example.FinanceTracker.dto.response.CategoryResponse;
import com.example.FinanceTracker.entity.Category;
import com.example.FinanceTracker.exception.BusinessRuleException;
import com.example.FinanceTracker.exception.DuplicateResourceException;
import com.example.FinanceTracker.exception.ResourceNotFoundException;
import com.example.FinanceTracker.mapper.CategoryMapper;
import com.example.FinanceTracker.security.CurrentUserService;
import com.example.FinanceTracker.repository.CategoryRepository;
import com.example.FinanceTracker.repository.TransactionRepository;
import com.example.FinanceTracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final TransactionRepository transactionRepository;
	private final UserRepository userRepository;
	private final CategoryMapper categoryMapper;
	private final CurrentUserService currentUserService;

	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		Long userId = currentUserService.getCurrentUserId();
		return categoryRepository.findAllByUserIdOrderByNameAsc(userId).stream()
				.map(categoryMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse findById(Long id) {
		return categoryMapper.toResponse(getCategoryOrThrow(id, currentUserService.getCurrentUserId()));
	}

	@Transactional
	public CategoryResponse create(CreateCategoryRequest request) {
		Long userId = currentUserService.getCurrentUserId();
		if (categoryRepository.existsByNameAndUserId(request.getName(), userId)) {
			throw new DuplicateResourceException("Category already exists with name: " + request.getName());
		}

		Category category = new Category();
		category.setName(request.getName());
		category.setType(request.getType());
		category.setUser(userRepository.getReferenceById(userId));

		return categoryMapper.toResponse(categoryRepository.save(category));
	}

	@Transactional
	public CategoryResponse update(Long id, UpdateCategoryRequest request) {
		Long userId = currentUserService.getCurrentUserId();
		Category category = getCategoryOrThrow(id, userId);

		if (categoryRepository.existsByNameAndUserIdAndIdNot(request.getName(), userId, id)) {
			throw new DuplicateResourceException("Category already exists with name: " + request.getName());
		}

		category.setName(request.getName());
		category.setType(request.getType());

		return categoryMapper.toResponse(category);
	}

	@Transactional
	public void delete(Long id) {
		Long userId = currentUserService.getCurrentUserId();
		Category category = getCategoryOrThrow(id, userId);

		if (transactionRepository.existsByCategoryIdAndUserId(id, userId)) {
			throw new BusinessRuleException(
					"Category '%s' cannot be deleted because it has linked transactions".formatted(category.getName())
			);
		}

		categoryRepository.delete(category);
	}

	private Category getCategoryOrThrow(Long id, Long userId) {
		return categoryRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", id));
	}
}
