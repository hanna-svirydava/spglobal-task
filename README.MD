# S&P Global Task README

Welcome to the S&P Global Task README! This document provides essential information about the current and
possible implementations, and instructions on how to test the application.
Please follow the instructions below to get started.

## Table of Contents

- [Introduction](#introduction)
- [Testing the Application](#testing-the-application)
- [Current Implementation](#current-implementation)
- [Possible Implementations](#possible-implementations)

## Introduction

The S&P Global Task is designed to provide a service for tracking the last price of financial instruments.
This README will guide you through the process of testing the application, and information
about the current and possible implementations.

## Testing the Application

Unit tests have been written to validate all existing functionality.

To test the application, follow these steps:

1. Open a terminal window.

2. Navigate to the root directory of the project.

3. Run the following command:

   ```sh
   ./gradlew test
   ```

   This command will execute the application's tests. The results will be displayed in the terminal, showing whether the
   tests passed or failed.

## Current Implementation

The current implementation consists of two main classes:

1. `PriceData` as an entity.

2. `DataService` as a service that allows to upload and obtain data.

    * It's assumed the `obtainPriceById` method is called pretty often, and it was necessary
      to provide receiving data by different threads without blocking, that's why inside service
      there was used `ReentrantReadWriteLock`.

    * `readLock` and `writeLock` objects are acquired only once to minimize method call overhead.

    * To work as Publisher with data, it was created `Transaction` class. It allows to control
      data uploading, committing and rolling back changes.

      Creating transaction (`createTransaction` method), uploading data (`upload` method),
      and rollback (`rollback` method) through transactions don't block read operation and other transactions.  
      Only `commit` method blocks the data to provide atomic operation.
      One more advantage: it's hard to call methods in incorrect order.

## Possible Implementations

1. Instead of `Transaction` class, we could implement four methods (`startTransaction`, `upload`,
   `commit` and `rollback`) in `DataService` class. Transaction data would be saved using `ThreadLocal`.  
   However, the disadvantage of this approach is impossibility to create multiple transactions in one thread.

2. As option, we also could implement above methods in `DataService` class
   with `createTransaction` method returning unique transaction id, and other methods should get
   this transaction id like a parameter. Data could be stored in `ConcurrentHashMap` with `transactionId` as key.  
   The disadvantage: the transactions could block each other if their ids in the same map bucket,
   the leak memory could happen in case if user will not call `commit` or `rollback` methods
   (we can solve this problem through adding automatic rollback transaction in a timeout).  
   The advantage: such approach could be used in REST API.
