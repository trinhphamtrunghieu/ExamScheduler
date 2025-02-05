package com.doan.repository;

import com.doan.dto.Sinh_Vien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Sinh_Vien_Repository extends JpaRepository<Sinh_Vien, String> {
}
