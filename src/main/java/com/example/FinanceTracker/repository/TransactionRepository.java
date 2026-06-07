package com.example.FinanceTracker.repository;

import com.example.FinanceTracker.entity.Transaction;
import com.example.FinanceTracker.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	boolean existsByCategoryId(Long categoryId);

	@Query("""
			SELECT t FROM Transaction t
			WHERE (:type IS NULL OR t.type = :type)
			  AND (:from IS NULL OR t.transactionDate >= :from)
			  AND (:to IS NULL OR t.transactionDate <= :to)
			ORDER BY t.transactionDate DESC
			""")
	List<Transaction> findByFilters(
			@Param("type") TransactionType type,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to
	);
}
