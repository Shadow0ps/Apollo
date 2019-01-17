/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.util.UUID;

public class ServiceModeDirProvider extends AbstractDirProvider {
    private static final String INSTALLATION_DIR =  ServiceModeDirProvider.class.getClassLoader().getResource("").getPath();

    public ServiceModeDirProvider(String applicationName, UUID chainId) {
        super(INSTALLATION_DIR, applicationName, chainId);
    }

    public ServiceModeDirProvider(String applicationName, UUID chainId, PredefinedDirLocations dirLocations) {
        super(INSTALLATION_DIR, applicationName, chainId, dirLocations);
    }
}
