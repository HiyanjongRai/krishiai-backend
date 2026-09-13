package com.krishiai.expert.entity;

/**
 * Classifies how significant a crop or domain is to the expert's practice.
 *
 * <p>Business rule: an expert may have at most <strong>3 PRIMARY</strong> crops.
 * There is no limit on SECONDARY crops or broad AREA expertises.
 */
public enum CropExpertiseType {

    /**
     * Core crop — the expert has deep primary specialization.
     * Maximum 3 per expert (enforced in the service layer).
     */
    PRIMARY,

    /**
     * Supporting crop — the expert has working knowledge but it is not their main focus.
     */
    SECONDARY,

    /**
     * Broader agricultural domain (e.g. Pest Management, Soil Management, Irrigation).
     */
    AREA
}
