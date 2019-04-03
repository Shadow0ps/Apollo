/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.enterprise.inject.spi.CDI;

import static org.junit.jupiter.api.Assertions.*;

@EnableWeld
class PhasingAppendixTest {

    private BlockchainImpl blockchain = Mockito.mock(BlockchainImpl.class);
    private EpochTime timeService = Mockito.mock(EpochTime.class);
    private Block block = Mockito.mock(Block.class);

    private int lastBlockHeight = 1000;
    private int currentTime = 11000;

    private PhasingAppendix phasingAppendix = new PhasingAppendix(-1, 360, new PhasingParams(Byte.valueOf("-1"), 0L,0L,0L, Byte.valueOf("0"), Convert.EMPTY_LONG), null, null, Byte.MIN_VALUE);


    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesHolder.class, BlockchainConfig.class, DbConfig.class,
            BlockDaoImpl.class, TransactionDaoImpl.class, TransactionProcessor.class,
            TransactionalDataSource.class, DatabaseManager.class)
            .addBeans(MockBean.of(blockchain, BlockchainImpl.class))
            .addBeans(MockBean.of(timeService, EpochTime.class))

            .build();

    @BeforeEach
    void setUp() {
        //bug with CDI
        try {CDI.current().select(BlockchainImpl.class).get();}catch (Exception e){}

        Mockito.doReturn(lastBlockHeight).when(blockchain).getHeight();
        Mockito.doReturn(lastBlockHeight).when(block).getHeight();
        Mockito.doReturn(currentTime).when(timeService).getEpochTime();
        Mockito.doReturn(block).when(blockchain).getLastBlock();
    }

    @Test
    void validateFinishHeightAndTimeWhenBothNotFilled() {
        assertThrows(AplException.NotCurrentlyValidException.class,()-> phasingAppendix.validateFinishHeightAndTime(-1, -1));
    }

    @Test
    void validateFinishHeightAndTimeWhenBothFilled() {
        assertThrows(AplException.NotCurrentlyValidException.class,()-> phasingAppendix.validateFinishHeightAndTime(500, 360));
    }

    @Test
    void validateFinishHeightAndTimeWhenTimeNull() {
        assertThrows(AplException.NotCurrentlyValidException.class,()-> phasingAppendix.validateFinishHeightAndTime(500, null));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightNull() {
        assertThrows(AplException.NotCurrentlyValidException.class,()-> phasingAppendix.validateFinishHeightAndTime(null, 300));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightNotFilledAndTimeMoreThenMax() {
        assertThrows(AplException.NotCurrentlyValidException.class,()-> phasingAppendix.validateFinishHeightAndTime(-1, currentTime + Constants.MAX_PHASING_TIME_DURATION_SEC));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightLessThenMin() {
        assertThrows(AplException.NotCurrentlyValidException.class,()-> phasingAppendix.validateFinishHeightAndTime(lastBlockHeight, -1));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightMoreThenMax() {
        assertThrows(AplException.NotCurrentlyValidException.class,()-> phasingAppendix.validateFinishHeightAndTime(lastBlockHeight + Constants.MAX_PHASING_DURATION, -1));
    }



}