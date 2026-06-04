package com.example.FinanceTracker.repository;

import com.example.FinanceTracker.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
