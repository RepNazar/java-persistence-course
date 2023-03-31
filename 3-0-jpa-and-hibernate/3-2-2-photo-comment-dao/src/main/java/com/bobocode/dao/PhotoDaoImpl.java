package com.bobocode.dao;

import com.bobocode.model.Photo;
import com.bobocode.model.PhotoComment;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Please note that you should not use auto-commit mode for your implementation.
 */
public class PhotoDaoImpl implements PhotoDao {
    private EntityManagerFactory entityManagerFactory;

    public PhotoDaoImpl(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void save(Photo photo) {
        doWithTX(entityManager -> entityManager.persist(photo));
    }

    @Override
    public Photo findById(long id) {
        return doWithTXReturning(entityManager -> entityManager.find(Photo.class, id));
    }

    @Override
    public List<Photo> findAll() {
        return doWithTXReturning(entityManager -> {
            TypedQuery<Photo> findAllQuery = entityManager.createQuery("SELECT p FROM Photo p", Photo.class);
            return findAllQuery.getResultList();
        });
    }

    @Override
    public void remove(Photo photo) {
        doWithTX(entityManager -> entityManager.remove(entityManager.merge(photo)));
    }

    @Override
    public void addComment(long photoId, String comment) {
        doWithTX(entityManager -> {
            Photo photo = entityManager.getReference(Photo.class, photoId);
            PhotoComment newComment = new PhotoComment();
            newComment.setPhoto(photo);
            newComment.setText(comment);
            newComment.setCreatedOn(LocalDateTime.now());
            entityManager.persist(newComment);
        });
    }

    private <T> T doWithTXReturning(Function<EntityManager, T> function) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            T res = function.apply(entityManager);
            entityManager.getTransaction().commit();
            return res;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new RuntimeException(e);
        }
    }

    private void doWithTX(Consumer<EntityManager> consumer) {
        doWithTXReturning(entityManager -> {
            consumer.accept(entityManager);
            return null;
        });
    }
}
