package com.maisonedition.repository;

import com.maisonedition.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findAllByOrderByDateCreationDesc();
    long countByLuFalse();
}
