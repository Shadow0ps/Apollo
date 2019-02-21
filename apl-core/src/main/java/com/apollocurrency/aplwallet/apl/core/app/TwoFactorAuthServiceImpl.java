/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthEntity;
import com.apollocurrency.aplwallet.apl.core.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {
    private static final Logger LOG = getLogger(TwoFactorAuthServiceImpl.class);
    private static final Base32 BASE_32 = new Base32();
    private static final String ISSUER_URL_TEMPLATE = "&issuer=Apollo-%s-%d";
    private static final int SECRET_LENGTH = 32;
    private static final int UPPER_BOUND_OF_RANDOM_SUFFIX_NUMBER = 1_000_000;
    private static final String DEFAULT_CHARSET = "UTF-8";

    private TwoFactorAuthRepository repository;
    private final Random random;
    private final String issuerSuffix;

    @Inject
    public TwoFactorAuthServiceImpl(TwoFactorAuthRepository repository,
                                    @Property(name = "apl.issuerSuffix2FA") String issuerSuffix) {
        this(repository,
                StringUtils.isBlank(issuerSuffix)
                        ? RuntimeEnvironment.getInstance().isDesktopApplicationEnabled()
                        ? "desktop" : "web" :
                        issuerSuffix,
                new Random());
    }

    public TwoFactorAuthServiceImpl(TwoFactorAuthRepository repository, String issuerSuffix, Random random) {
        if (issuerSuffix == null || issuerSuffix.trim().isEmpty()) {
            throw new IllegalArgumentException("issuerSuffix cannot be null or empty");
        }
        this.repository = repository;
        this.random = random;
        this.issuerSuffix = issuerSuffix.trim();
    }

    @Override
//    transaction management required
    public TwoFactorAuthDetails enable(long accountId) {
//        check existing and not confirmed 2fa
        TwoFactorAuthEntity entity = repository.get(accountId);
        if (entity != null) {
            if (!entity.isConfirmed()) {

            String existingBase32Secret = BASE_32.encodeToString(entity.getSecret());
            return new TwoFactorAuthDetails(getQrCodeUrl(Convert2.rsAccount(entity.getAccount()), existingBase32Secret),
                    existingBase32Secret, Status2FA.OK);
            }
            return new TwoFactorAuthDetails(null, null, Status2FA.ALREADY_ENABLED);
        }
        //length of Base32Secret should be multiple 8 (length % 8 == 0); e.g. 8, 16, 24, 32, etc.

        String base32Secret = TimeBasedOneTimePasswordUtil.generateBase32Secret(SECRET_LENGTH);
        byte[] base32Bytes = BASE_32.decode(base32Secret);
        boolean saved = repository.add(new TwoFactorAuthEntity(accountId, base32Bytes, false));
        if (!saved) {
            return new TwoFactorAuthDetails(null, null, Status2FA.INTERNAL_ERROR);
        }

        String qrCodeUrl = getQrCodeUrl(Convert2.rsAccount(accountId), base32Secret);
        return new TwoFactorAuthDetails(qrCodeUrl, base32Secret, Status2FA.OK);
    }

    private String getQrCodeUrl(String rsAccount, String base32Secret) {
        try {
            String baseUrl = TimeBasedOneTimePasswordUtil.qrImageUrl(URLEncoder.encode(rsAccount, DEFAULT_CHARSET), base32Secret);
            String issuerUrlPart = String.format(ISSUER_URL_TEMPLATE, issuerSuffix, random.nextInt(UPPER_BOUND_OF_RANDOM_SUFFIX_NUMBER));
            return baseUrl + URLEncoder.encode(issuerUrlPart, DEFAULT_CHARSET);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Status2FA disable(long accountId, int authCode) {
        TwoFactorAuthEntity entity = repository.get(accountId);
        Status2FA status2Fa = process2FAEntity(entity, true, authCode);
        if (status2Fa != Status2FA.OK) {
            return status2Fa;
        }
        return repository.delete(accountId) ? Status2FA.OK : Status2FA.INTERNAL_ERROR;
    }

    @Override
    public boolean isEnabled(long accountId) {
        TwoFactorAuthEntity entity = repository.get(accountId);
        return entity != null && entity.isConfirmed() && entity.getSecret() != null;
    }

    @Override
    public Status2FA tryAuth(long accountId, int authCode) {
        TwoFactorAuthEntity entity = repository.get(accountId);
        return process2FAEntity(entity, true, authCode);
    }

    private boolean authEntity(TwoFactorAuthEntity entity, int authCode) {

        boolean success = false;
        try {
            if (entity != null) {
                String base32Secret = BASE_32.encodeToString(entity.getSecret());
                //window millis should be 0, other parameters will not work properly
                success = TimeBasedOneTimePasswordUtil.validateCurrentNumber(
                        base32Secret, authCode, 0);
            }
        }
        catch (GeneralSecurityException e) {
            LOG.error("Unable to create temporal code", e);
        }
        return success;
    }

    @Override
    public Status2FA confirm(long accountId, int authCode) {
        TwoFactorAuthEntity entity = repository.get(accountId);
        Status2FA analyzedStatus = process2FAEntity(entity, false, authCode);
        if (analyzedStatus != Status2FA.OK) {
            return analyzedStatus;
        }
        entity.setConfirmed(true);
        boolean updated = repository.update(entity);
        if (!updated) {
            return Status2FA.INTERNAL_ERROR;
        }
        return Status2FA.OK;
    }

    private Status2FA process2FAEntity(TwoFactorAuthEntity entity, boolean shouldBeConfirmed, int authCode) {
        if (entity == null) {
            return Status2FA.NOT_ENABLED;
        }
        if (!shouldBeConfirmed && entity.isConfirmed()) {
            return Status2FA.ALREADY_CONFIRMED;
        }
        if (shouldBeConfirmed && !entity.isConfirmed()) {
            return Status2FA.NOT_CONFIRMED;
        }
        if (!authEntity(entity, authCode)) {
            return Status2FA.INCORRECT_CODE;
        }
        return Status2FA.OK;
    }
}
