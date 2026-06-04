package com.example.FinanceTracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

	@Column(nullable = false, unique = true)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransactionType type;
}
