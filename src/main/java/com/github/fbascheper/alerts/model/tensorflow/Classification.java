package com.github.fbascheper.alerts.model.tensorflow;

import java.util.EnumSet;

/**
 * An enumeration of all classifications in the TensorFlow model.
 *
 * @author Erik-Berndt Scheper
 * @since 28-05-2019
 */
public enum Classification {

    BURGLAR_ALERT(0, "burglar-alert"),

    NO_BURGLAR_ALERT(1, "no-burglar-alert");

    private final int index;
    private final String description;

    Classification(int index, String description) {
        this.index = index;
        this.description = description;
    }

    /**
     * Determine the most likely {@link Classification} for the given probabilities derived
     * from the TensorFlow model.
     *
     * @param labelProbabilities the array of probabilities
     * @return the {@link Classification}
     */
    public static Classification forLabelProbabilities(float[] labelProbabilities) {
        int bestLabelIdx = maxIndex(labelProbabilities);

        return EnumSet.allOf(Classification.class).stream()
                .filter(imageClassification -> imageClassification.index == bestLabelIdx)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Index " + bestLabelIdx + " not found"));
    }

    /**
     * @return index of this class in the TensorFlow model
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return description of this image class
     */
    public String getDescription() {
        return description;
    }


    private static int maxIndex(float[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; ++i) {
            if (probabilities[i] > probabilities[best]) {
                best = i;
            }
        }
        return best;
    }
}
