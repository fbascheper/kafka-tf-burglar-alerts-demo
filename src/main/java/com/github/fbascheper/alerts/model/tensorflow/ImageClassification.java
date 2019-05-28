package com.github.fbascheper.alerts.model.tensorflow;


import com.github.fbascheper.alerts.model.avro.SerializableImage;

/**
 * An image combined with its classification from the TensorFlow model.
 *
 * @author Erik-Berndt Scheper
 * @since 28-05-2019
 */
public class ImageClassification {

    private final SerializableImage image;
    private final Classification classification;
    private final float probability;

    public ImageClassification(SerializableImage image, Classification classification, float probability) {
        this.image = image;
        this.classification = classification;
        this.probability = probability;
    }

    public SerializableImage getImage() {
        return image;
    }

    public Classification getClassification() {
        return classification;
    }

    public float getProbability() {
        return probability;
    }

    @Override
    public String toString() {
        return String.format("BEST MATCH: for image %s was %s (%.2f%% likely)",
                this.image.getName(), this.classification.getDescription(), this.probability);

    }
}
