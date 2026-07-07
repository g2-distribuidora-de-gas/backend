package com.sistemagas.pedidos.support;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * TransactionManager que no hace commit/rollback real, util para tests unitarios
 * donde queremos ejercitar la logica envuelta en TransactionTemplate sin
 * necesitar una DB.
 */
public class NoopTransactionManager extends AbstractPlatformTransactionManager {

    @Override
    protected Object doGetTransaction() throws TransactionException {
        return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        // no-op
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        // no-op
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        // no-op
    }
}