package com.github.fbascheper.alerts.model.tensorflow;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * kafka-tf-burglar-alerts-demo - Description.
 *
 * @author Erik-Berndt Scheper
 * @since 28-05-2019
 */
public class ClassificationTest {

    @Test
    public void forLabelProbabilitiesBurglar() {
        float[] props1 = new float[]{0.5f, 0.3f};
        assertThat(Classification.forLabelProbabilities(props1), is(Classification.BURGLAR_ALERT));

    }

    @Test
    public void forLabelProbabilitiesNotBurglar() {
        float[] props1 = new float[]{0.2f, 0.3f};
        assertThat(Classification.forLabelProbabilities(props1), is(Classification.NO_BURGLAR_ALERT));

    }

    @Test
    public void forLabelProbabilitiesEqual() {
        float[] props1 = new float[]{0.3f, 0.3f};
        assertThat(Classification.forLabelProbabilities(props1), is(Classification.BURGLAR_ALERT));

    }

    @Test(expected = IllegalArgumentException.class)
    public void forLabelProbabilitiesIllegal() {
        float[] props1 = new float[]{0.3f, 0.4f, 0.5f};
        assertThat(Classification.forLabelProbabilities(props1), is(Classification.BURGLAR_ALERT));

    }
}
