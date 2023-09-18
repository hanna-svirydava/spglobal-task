package com.spglobal.service;

import com.spglobal.entity.PriceData;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe class.
 */
public class DataService<T> {
    private final Map<String, PriceData<T>> data = new HashMap<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

    /**
     * Obtains the last price record by ID.
     * Time complexity O(1)
     *
     * @param id is an ID.
     * @return the price data itself, which is a flexible data structure.
     * @throws NoSuchElementException if a record by ID doesn't exist.
     */
    public T obtainPriceById(String id) {
        readLock.lock();
        try {
            final PriceData<T> priceData = data.get(id);
            if (priceData == null) {
                throw new NoSuchElementException("No value present.");
            }
            return priceData.getPayload();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Creates a new transaction to work with data.<br>
     * Work after creation: it's possible to call:<br>
     * - upload() method to add data,<br>
     * - commit() to commit changes,<br>
     * - rollback() to rollback changes.<br>
     * Time complexity O(1)
     *
     * @return a transaction object through which the client can load data
     * and apply or cancel it.
     */
    public Transaction createTransaction() {
        return new Transaction();
    }

    /**
     * Thread-unsafe class.
     */
    public class Transaction {
        private List<PriceData<T>> dataListToPublish = new ArrayList<>();
        private boolean endOfTransactionFlag = false;

        /**
         * Uploads data to temporary list.
         * Time complexity: O(n), n - dataList.size().
         *
         * @param dataList is a list of PriceData objects that necessary to upload.
         * @throws IllegalStateException in case if Transaction was completed before method called.
         */
        public Transaction upload(List<PriceData<T>> dataList) {
            verifyIsTransactionOpen();
            dataListToPublish.addAll(dataList);

            return this;
        }

        /**
         * Applies transaction changes to save data.
         * Time complexity: O(n), n - dataListToPublish.size().
         *
         * @throws IllegalStateException in case if Transaction was completed before method called.
         */
        public void commit() {
            verifyIsTransactionOpen();
            writeLock.lock();
            try {
                for (final PriceData<T> priceData : dataListToPublish) {
                    final PriceData<T> oldPriceData = data.get(priceData.getId());
                    if (oldPriceData == null
                            || oldPriceData.getAsOf().isBefore(priceData.getAsOf())) {
                        data.put(priceData.getId(), priceData);
                    }
                }
            } finally {
                writeLock.unlock();
            }
            dataListToPublish = null;
            endOfTransactionFlag = true;
        }

        /**
         * Rollbacks transaction changes.
         * Not save data and clear temporary data.
         * Time complexity: O(1).
         *
         * @throws IllegalStateException in case if Transaction was completed before method called.
         */
        public void rollback() {
            verifyIsTransactionOpen();
            dataListToPublish = null;
            endOfTransactionFlag = true;
        }

        private void verifyIsTransactionOpen() {
            if (endOfTransactionFlag) {
                throw new IllegalStateException("Illegal operation. Transaction is already closed.");
            }
        }
    }
}
