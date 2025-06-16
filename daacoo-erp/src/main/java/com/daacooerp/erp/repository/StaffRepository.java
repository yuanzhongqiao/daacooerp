package com.daacooerp.erp.repository;

import com.daacooerp.erp.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    
    Page<Staff> findByCompany_Id(Long companyId, Pageable pageable);
    
    List<Staff> findByCompany_Id(Long companyId);
} 