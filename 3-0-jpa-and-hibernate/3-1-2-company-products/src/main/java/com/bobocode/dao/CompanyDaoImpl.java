package com.bobocode.dao;

import com.bobocode.exception.CompanyDaoException;
import com.bobocode.model.Company;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

public class CompanyDaoImpl implements CompanyDao {
    private EntityManagerFactory entityManagerFactory;

    public CompanyDaoImpl(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public Company findByIdFetchProducts(Long id) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            TypedQuery<Company> query = entityManager.createQuery("SELECT DISTINCT c FROM Company c JOIN FETCH c.products WHERE c.id = :id", Company.class);
            query.setParameter("id", id);
            Company company = query.getSingleResult();
            entityManager.getTransaction().commit();
            return company;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new CompanyDaoException("Error: ", e);
        } finally {
            entityManager.close();
        }
    }

}
