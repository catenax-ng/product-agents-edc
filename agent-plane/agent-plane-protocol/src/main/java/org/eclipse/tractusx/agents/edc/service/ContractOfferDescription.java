//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.service;

import org.eclipse.edc.policy.model.Policy;

public class ContractOfferDescription {
    private final String offerId;
    private final String assetId;
    private final Policy policy;

    public ContractOfferDescription(String offerId,
                                    String assetId,
                                    Policy policy) {
        this.offerId = offerId;
        this.assetId = assetId;
        this.policy = policy;
    }

    public String getOfferId() {
        return this.offerId;
    }

    public String getAssetId() {
        return this.assetId;
    }

    public Policy getPolicy() {
        return this.policy;
    }
}