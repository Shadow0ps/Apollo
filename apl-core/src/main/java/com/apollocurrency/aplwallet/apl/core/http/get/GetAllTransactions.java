/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.transaction.Payment;
import com.apollocurrency.aplwallet.apl.core.app.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.PRIVATE_TRANSACTIONS_ACCESS_DENIED;


public class GetAllTransactions extends AbstractAPIRequestHandler {

    private static class GetAllTransactionsHolder {
        private static final GetAllTransactions INSTANCE = new GetAllTransactions();
    }

    public static GetAllTransactions getInstance() {
        return GetAllTransactionsHolder.INSTANCE;
    }

    private GetAllTransactions() {
        super(new APITag[] {APITag.TRANSACTIONS}, "type", "subtype", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        byte type = ParameterParser.getByteOrNegative(req, "type", false);
        byte subtype = ParameterParser.getByteOrNegative(req, "subtype", false);
        if (TransactionType.findTransactionType(type, subtype) == Payment.PRIVATE) {
            return PRIVATE_TRANSACTIONS_ACCESS_DENIED;
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray transactions = new JSONArray();
        try (DbIterator<? extends Transaction> iterator = lookupBlockchain().getTransactions(type, subtype, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                transactions.add(JSONData.transaction(transaction, false, false));
            }
        }
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;

    }
}
