package com.apollocurrency.aplwallet.apl.core.http.exchange.api;



import java.math.BigDecimal;
import com.apollocurrency.aplwallet.apl.core.http.exchange.api.NotFoundException;

import javax.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
@Singleton
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2019-03-07T08:10:07.244Z")
public interface DexGetOffersApiService {
      Response dexGetOffersGet(String account,String pair,String type,BigDecimal minAskPrice,BigDecimal maxBidPrice,SecurityContext securityContext)
      throws NotFoundException;
}
