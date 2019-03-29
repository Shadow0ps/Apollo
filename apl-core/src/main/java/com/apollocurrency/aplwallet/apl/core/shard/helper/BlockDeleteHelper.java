/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_TABLE_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;

/**
 * Helper class is used for deleting block/transaction data from main database previously copied to shard db.
 *
 * @author yuriy.larin
 */
public class BlockDeleteHelper extends AbstractRelinkUpdateHelper {
    private static final Logger log = getLogger(BlockDeleteHelper.class);

    public BlockDeleteHelper() {
    }

    @Override
    public long processOperation(Connection sourceConnect, Connection targetConnect,
                                 TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);
        checkMandatoryParameters(sourceConnect, operationParams);

        long startSelect = System.currentTimeMillis();

        if (BLOCK_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging =
                    "select DB_ID from BLOCK where DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound = "SELECT IFNULL(DB_ID, 0) as DB_ID from BLOCK where HEIGHT = ?";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from BLOCK";
            log.trace(sqlSelectBottomBound);
        } else {
            throw new IllegalAccessException("Unsupported table. 'Block' is expected. Pls use another Helper class");
        }
        // select upper, bottom DB_ID
        selectLowerAndUpperBoundValues(sourceConnect, operationParams);

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.lowerBoundColumnValue = lowerBoundIdValue;
        paginateResultWrapper.upperBoundColumnValue = upperBoundIdValue;

        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                ps.setLong(1, paginateResultWrapper.lowerBoundColumnValue);
                ps.setLong(2, paginateResultWrapper.upperBoundColumnValue);
                ps.setLong(3, operationParams.batchCommitSize);
            } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, operationParams));
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        } finally {
            if (this.preparedInsertStatement != null && !this.preparedInsertStatement.isClosed()) {
                this.preparedInsertStatement.close();
            }
        }
        log.debug("Deleted '{}' = [{}] within {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);

        log.debug("Total (with CONSTRAINTS) '{}' = [{}] in {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);
        return totalRowCount;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                    Connection targetConnect, TableOperationParams operationParams)
            throws SQLException {
        int rows = 0;
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                 // handle rows here
                if (rsmd == null) {
                    rsmd = rs.getMetaData();
                    if (BLOCK_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
//                        sqlInsertString.append("delete from BLOCK WHERE DB_ID > ? AND DB_ID < ? LIMIT ?");
                        sqlInsertString.append("delete from BLOCK WHERE DB_ID < ? LIMIT ?");
                    }
                    // precompile sql
                    if (preparedInsertStatement == null) {
                        preparedInsertStatement = targetConnect.prepareStatement(sqlInsertString.toString());
                        log.trace("Precompiled delete = {}", sqlInsertString);
                    }
                }
//                paginateResultWrapper.lowerBoundColumnValue = rs.getLong(BASE_COLUMN_NAME); // assign latest value for usage outside method

                try {
//                    preparedInsertStatement.setObject(1, rs.getObject(1));
                    preparedInsertStatement.setObject(1, paginateResultWrapper.upperBoundColumnValue);
                    preparedInsertStatement.setObject(2, operationParams.batchCommitSize);
                    insertedCount += preparedInsertStatement.executeUpdate();
                    log.debug("Deleting '{}' into {} : column {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
                } catch (Exception e) {
                    log.error("Failed Deleting '{}' into {}, {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
                    log.error("Failed Deleting " + currentTableName, e);
                    targetConnect.rollback();
                    throw e;
                }
                rows++;
            }
            totalRowCount += rows;
        }
        log.trace("Total Records '{}': selected = {}, deleted = {}, rows = {}, {}={}", currentTableName,
                totalRowCount, insertedCount, rows, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);

        if (rows == 1) {
            // in case we have only 1 RECORD selected, move lower bound
            // move lower bound
            paginateResultWrapper.lowerBoundColumnValue += operationParams.batchCommitSize;
        }
        targetConnect.commit(); // commit latest records if any
        return rows != 0;
    }

}