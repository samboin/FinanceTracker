package com.example.FinanceTracker.repository;

import com.example.FinanceTracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsByNameAndUserId(String name, Long userId);

	boolean existsByNameAndUserIdAndIdNot(String name, Long userId, Long id);

	List<Category> findAllByUserIdOrderByNameAsc(Long userId);

	Optional<Category> findByIdAndUserId(Long id, Long userId);
}
