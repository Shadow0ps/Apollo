/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Asset;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssetsByIssuer extends APIServlet.APIRequestHandler {

    private static class GetAssetsByIssuerHolder {
        private static final GetAssetsByIssuer INSTANCE = new GetAssetsByIssuer();
    }

    public static GetAssetsByIssuer getInstance() {
        return GetAssetsByIssuerHolder.INSTANCE;
    }

    private GetAssetsByIssuer() {
        super(new APITag[] {APITag.AE, APITag.ACCOUNTS}, "account", "account", "account", "firstIndex", "lastIndex", "includeCounts");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long[] accountIds = ParameterParser.getAccountIds(req, true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeCounts = "true".equalsIgnoreCase(req.getParameter("includeCounts"));

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        response.put("assets", accountsJSONArray);
        for (long accountId : accountIds) {
            JSONArray assetsJSONArray = new JSONArray();
            try (DbIterator<Asset> assets = Asset.getAssetsIssuedBy(accountId, firstIndex, lastIndex)) {
                while (assets.hasNext()) {
                    assetsJSONArray.add(JSONData.asset(assets.next(), includeCounts));
                }
            }
            accountsJSONArray.add(assetsJSONArray);
        }
        return response;
    }

}