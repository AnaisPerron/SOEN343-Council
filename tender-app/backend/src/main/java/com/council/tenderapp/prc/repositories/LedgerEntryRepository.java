package com.council.tenderapp.prc.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.prc.entities.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long>
{
    
}
