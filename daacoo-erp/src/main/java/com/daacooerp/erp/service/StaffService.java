package com.daacooerp.erp.service;

import com.daacooerp.erp.entity.Company;
import com.daacooerp.erp.entity.Staff;
import com.daacooerp.erp.repository.CompanyRepository;
import com.daacooerp.erp.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StaffService {

    @Autowired
    private StaffRepository staffRepository;
    
    @Autowired
    private CompanyRepository companyRepository;

    public Page<Staff> getAllStaff(Pageable pageable) {
        return staffRepository.findAll(pageable);
    }

    public Page<Staff> getStaffByCompany(Long companyId, Pageable pageable) {
        return staffRepository.findByCompany_Id(companyId, pageable);
    }
    
    public List<Staff> getStaffByCompany(Long companyId) {
        return staffRepository.findByCompany_Id(companyId);
    }

    public Optional<Staff> getStaffById(Long id) {
        return staffRepository.findById(id);
    }

    public Staff createStaff(Staff staff) {
        // 检查公司是否存在
        if (staff.getCompany() != null && staff.getCompany().getId() != null) {
            Optional<Company> company = companyRepository.findById(staff.getCompany().getId());
            if (!company.isPresent()) {
                throw new RuntimeException("Company not found with id: " + staff.getCompany().getId());
            }
            staff.setCompany(company.get());
        }
        return staffRepository.save(staff);
    }

    public Staff updateStaff(Long id, Staff staffDetails) {
        Optional<Staff> optionalStaff = staffRepository.findById(id);
        
        if (optionalStaff.isPresent()) {
            Staff existingStaff = optionalStaff.get();
            existingStaff.setName(staffDetails.getName());
            existingStaff.setPosition(staffDetails.getPosition());
            existingStaff.setTel(staffDetails.getTel());
            existingStaff.setEmail(staffDetails.getEmail());
            existingStaff.setDepartment(staffDetails.getDepartment());
            existingStaff.setJoinDate(staffDetails.getJoinDate());
            existingStaff.setStatus(staffDetails.getStatus());
            
            // 如果更新了公司
            if (staffDetails.getCompany() != null && staffDetails.getCompany().getId() != null) {
                Optional<Company> company = companyRepository.findById(staffDetails.getCompany().getId());
                if (!company.isPresent()) {
                    throw new RuntimeException("Company not found with id: " + staffDetails.getCompany().getId());
                }
                existingStaff.setCompany(company.get());
            }
            
            return staffRepository.save(existingStaff);
        } else {
            throw new RuntimeException("Staff not found with id: " + id);
        }
    }

    public void deleteStaff(Long id) {
        staffRepository.deleteById(id);
    }
} 