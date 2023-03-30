package com.bobocode.dao;

import com.bobocode.exception.AccountDaoException;
import com.bobocode.model.Account;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

public class AccountDaoImpl implements AccountDao {
    private EntityManagerFactory emf;

    public AccountDaoImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public void save(Account account) {
        Objects.requireNonNull(account);
        doWithTX(entityManager -> {
            entityManager.persist(account);
            return null;
        });
    }

    @Override
    public Account findById(Long id) {
        return doWithTX(entityManager -> {
            return entityManager.find(Account.class, id);
        });
    }

    @Override
    public Account findByEmail(String email) {
        return doWithTX(entityManager -> {
            Query query = entityManager.createQuery("SELECT a FROM Account a WHERE a.email = :email");
            // seen
            query.setParameter("email", email);
            List<Account> resultList = (List<Account>) query.getResultList();
            if (resultList.isEmpty()) {
                throw new AccountDaoException("", new NoSuchElementException());
            }
            return resultList.get(0);
        });
    }

    @Override
    public List<Account> findAll() {
        return doWithTX(entityManager -> {
            Query query = entityManager.createQuery("SELECT a FROM Account a WHERE a.email = email");
            return (List<Account>) query.getResultList();
        });
    }

    // seen
    @Override
    public void update(Account account) {
        doWithTX(entityManager -> {
            entityManager.merge(account);
            return null;
        });
    }

    // seen
    @Override
    public void remove(Account account) {
        doWithTX(entityManager -> {
            Account mergedAccount = entityManager.merge(account);
            entityManager.remove(mergedAccount);
            return null;
        });
    }

    private <T> T doWithTX(Function<EntityManager, T> function) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            T val = function.apply(em);
            em.getTransaction().commit();
            return val;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new AccountDaoException("Transaction is rolled back, error: ", e);
        } finally {
            em.close();
        }
    }
}

