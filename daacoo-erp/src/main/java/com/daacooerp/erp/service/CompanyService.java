package com.daacooerp.erp.service;

import com.daacooerp.erp.entity.Company;
import com.daacooerp.erp.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    public Page<Company> getCompanyList(Pageable pageable) {
        return companyRepository.findAll(pageable);
    }

    public Optional<Company> getCompanyById(Long id) {
        return companyRepository.findById(id);
    }

    public Company createCompany(Company company) {
        return companyRepository.save(company);
    }

    public Company updateCompany(Long id, Company companyDetails) {
        Optional<Company> optionalCompany = companyRepository.findById(id);
        
        if (optionalCompany.isPresent()) {
            Company existingCompany = optionalCompany.get();
            existingCompany.setName(companyDetails.getName());
            existingCompany.setAddress(companyDetails.getAddress());
            existingCompany.setContact(companyDetails.getContact());
            existingCompany.setEmail(companyDetails.getEmail());
            existingCompany.setType(companyDetails.getType());
            existingCompany.setContactPerson(companyDetails.getContactPerson());
            existingCompany.setRemark(companyDetails.getRemark());
            
            return companyRepository.save(existingCompany);
        } else {
            throw new RuntimeException("Company not found with id: " + id);
        }
    }

    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }
} 