package com.spglobal.service;

import com.spglobal.entity.PriceData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public class DataServiceTest {

    private final DataService<BigDecimal> service = new DataService<>();

    @Test
    public void whenNoDataExistShouldReturnNoSuchElementException() {
        final NoSuchElementException exception =
                Assertions.assertThrowsExactly(NoSuchElementException.class,
                        () -> service.obtainPriceById(UUID.randomUUID().toString()));
        Assertions.assertEquals("No value present.", exception.getMessage());
    }

    @Test
    public void whenDataUploadedButNotCommittedShouldReturnNoSuchElementException() {
        final Instant i1 = Instant.parse("2019-01-13T18:35:19.00Z");

        service.createTransaction()
                .upload(List.of(new PriceData<>("id", i1, new BigDecimal(12))));

        final NoSuchElementException exception = Assertions.assertThrowsExactly(
                NoSuchElementException.class, () -> service.obtainPriceById("id"));
        Assertions.assertEquals("No value present.", exception.getMessage());
    }

    @Test
    public void whenDataUploadedAndCommittedShouldReturnPrice() {
        final Instant i1 = Instant.parse("2019-01-13T18:35:19.00Z");

        service.createTransaction()
                .upload(List.of(new PriceData<>("id", i1, new BigDecimal(12))))
                .commit();

        Assertions.assertEquals(new BigDecimal(12), service.obtainPriceById("id"));
    }

    @Test
    public void whenUploadedSomeBatchesAndCommittedShouldReturnPrice() {
        final Instant i1 = Instant.parse("2019-01-13T18:35:19.00Z");

        service.createTransaction()
                .upload(List.of(new PriceData<>("id", i1, new BigDecimal(12))))
                .upload(List.of(new PriceData<>("2", i1, new BigDecimal(13))))
                .commit();

        Assertions.assertEquals(new BigDecimal(12), service.obtainPriceById("id"));
        Assertions.assertEquals(new BigDecimal(13), service.obtainPriceById("2"));
    }

    @Test
    public void whenDataUploadedButRollbackedShouldReturnNoSuchElementException() {
        final Instant i1 = Instant.parse("2019-01-13T18:35:19.00Z");

        service.createTransaction()
                .upload(List.of(new PriceData<>("id", i1, new BigDecimal(12))));

        final NoSuchElementException exception = Assertions.assertThrowsExactly(
                NoSuchElementException.class, () -> service.obtainPriceById("id"));
        Assertions.assertEquals("No value present.", exception.getMessage());
    }

    @Test
    public void whenNewerDataGoesBeforeOldDataShouldReturnTheNewestData() {
        final Instant i1 = Instant.parse("2019-01-13T16:10:35.00Z");

        service.createTransaction()
                .upload(List.of(new PriceData<>("id", i1, new BigDecimal(13))))
                .commit();
        Assertions.assertEquals(new BigDecimal(13), service.obtainPriceById("id"));

        final Instant i2 = Instant.parse("2019-01-13T18:35:19.00Z");
        service.createTransaction()
                .upload(List.of(new PriceData<>("id", i2, new BigDecimal(12))))
                .commit();
        Assertions.assertEquals(new BigDecimal(12), service.obtainPriceById("id"));
    }

    @Test
    public void whenOldDataGoesBeforeNewerDataShouldReturnTheNewestData() {
        final Instant i1 = Instant.parse("2019-01-13T18:35:19.00Z");

        service.createTransaction()
                .upload(List.of(new PriceData<>("id", i1, new BigDecimal(12))))
                .commit();
        Assertions.assertEquals(new BigDecimal(12), service.obtainPriceById("id"));

        final Instant i2 = Instant.parse("2019-01-13T16:10:35.00Z");
        service.createTransaction()
                .upload(List.of(new PriceData<>("id", i2, new BigDecimal(13))))
                .commit();
        Assertions.assertEquals(new BigDecimal(12), service.obtainPriceById("id"));
    }

    @Test
    public void whenTwoTransactionUploadedDataAtTheSameTimeShouldReturnAllUploadedData() {
        final Instant i1 = Instant.parse("2019-01-13T18:35:19.00Z");
        DataService<BigDecimal>.Transaction transaction1 = service.createTransaction();
        transaction1.upload(List.of(new PriceData<>("id1", i1, new BigDecimal(12))));

        final Instant i2 = Instant.parse("2019-01-13T18:35:19.00Z");
        DataService<BigDecimal>.Transaction transaction2 = service.createTransaction();
        transaction2.upload(List.of(new PriceData<>("id2", i2, new BigDecimal(13))));

        transaction1.commit();
        transaction2.commit();

        Assertions.assertEquals(new BigDecimal(12), service.obtainPriceById("id1"));
        Assertions.assertEquals(new BigDecimal(13), service.obtainPriceById("id2"));
    }

    @Test
    public void whenTransactionClosedAndTriedToUploadShouldReturnIllegalStateException() {
        final Instant i1 = Instant.parse("2019-01-13T18:35:19.00Z");

        final DataService<BigDecimal>.Transaction transaction = service.createTransaction();
        transaction.upload(List.of(new PriceData<>("id", i1, new BigDecimal(12))));
        transaction.commit();

        final IllegalStateException exception =
                Assertions.assertThrowsExactly(IllegalStateException.class,
                        () -> transaction.upload(List.of()));
        Assertions.assertEquals("Illegal operation. Transaction is already closed.", exception.getMessage());
    }

    @Test
    public void whenTransactionClosedAndTriedToCommitShouldReturnIllegalStateException() {
        final Instant i1 = Instant.parse("2019-01-13T18:35:19.00Z");

        final DataService<BigDecimal>.Transaction transaction = service.createTransaction();
        transaction.upload(List.of(new PriceData<>("id", i1, new BigDecimal(12))));
        transaction.commit();

        final IllegalStateException exception =
                Assertions.assertThrowsExactly(IllegalStateException.class, transaction::commit);
        Assertions.assertEquals("Illegal operation. Transaction is already closed.", exception.getMessage());
    }

    @Test
    public void whenTransactionClosedAndTriedToRollbackShouldReturnIllegalStateException() {
        final Instant i1 = Instant.parse("2019-01-13T18:35:19.00Z");

        final DataService<BigDecimal>.Transaction transaction = service.createTransaction();
        transaction.upload(List.of(new PriceData<>("id", i1, new BigDecimal(12))));
        transaction.commit();

        final IllegalStateException exception =
                Assertions.assertThrowsExactly(IllegalStateException.class, transaction::rollback);
        Assertions.assertEquals("Illegal operation. Transaction is already closed.", exception.getMessage());
    }
}
