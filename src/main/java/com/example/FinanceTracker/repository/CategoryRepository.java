package com.example.FinanceTracker.repository;

import com.example.FinanceTracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
