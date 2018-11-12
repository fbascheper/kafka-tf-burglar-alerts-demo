package com.github.fbascheper.alerts.util.tensorflow;

import com.github.fbascheper.alerts.model.avro.SerializableImage;
import org.slf4j.Logger;
import org.tensorflow.Graph;
import org.tensorflow.Output;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.util.Arrays;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Matcher using TensorFlow.
 *
 * @author Erik-Berndt Scheper
 * @since 02-11-2018
 */
public final class TensorFlowMatcher {

    private static final Logger LOGGER = getLogger(TensorFlowMatcher.class);

    private TensorFlowMatcher() {
        // prevent instantiation
    }

    /**
     * Match a given image and return a string that can be used as caption of an alert message.
     *
     * @param tfLabels   list of labels for all potential image classifications
     * @param tfGraphDef the graph definition used by TensorFlow
     * @param image      image to matched using TensorFlow
     * @return string that can be used as caption of an alert message
     */
    public static String matchImage(List<String> tfLabels, byte[] tfGraphDef, SerializableImage image) {

        byte[] imageBytes = image.getImageData().array();

        try (Tensor tensor = constructAndExecuteGraphToNormalizeImage(imageBytes)) {
            float[] labelProbabilities = executeInceptionGraph(tfGraphDef, tensor);
            int bestLabelIdx = maxIndex(labelProbabilities);

            String imageClassification = tfLabels.get(bestLabelIdx);
            float imageProbability = labelProbabilities[bestLabelIdx] * 100f;
            return String.format("BEST MATCH: for image %s was %s (%.2f%% likely)",
                    image.getName(), imageClassification, imageProbability);
        }

    }

    // ########################################################################################
    // Private helper class for construction and execution of the pre-built
    // TensorFlow model
    // ########################################################################################

    private static Tensor constructAndExecuteGraphToNormalizeImage(byte[] imageBytes) {
        // Graph construction: using the OperationBuilder class to construct a graph to
        // toSmartLockKeyValueList, resize and normalize a JPEG image.

        try (Graph g = new Graph()) {
            TensorFlowGraphBuilder b = new TensorFlowGraphBuilder(g);
            // Some constants specific to the pre-trained model at:
            // https://storage.googleapis.com/download.tensorflow.org/models/inception5h.zip
            //
            // - The model was trained with images scaled to 224x224 pixels.
            // - The colors, represented as R, G, B in 1-byte each were
            // converted to
            // float using (value - Mean)/Scale.
            final int H = 224;
            final int W = 224;
            final float mean = 117f;
            final float scale = 1f;

            // Since the graph is being constructed once per execution here, we
            // can use a constant for the
            // input image. If the graph were to be re-used for multiple input
            // images, a placeholder would
            // have been more appropriate.
            final Output<String> input = b.constant("input", imageBytes);
            final Output<Float> output = b
                    .div(b.sub(
                            b.resizeBilinear(b.expandDims(b.cast(b.decodeJpeg(input, 3), Float.class),
                                    b.constant("make_batch", 0)), b.constant("size", new int[]{H, W})),
                            b.constant("mean", mean)), b.constant("scale", scale));
            try (Session s = new Session(g)) {
                return s.runner().fetch(output.op().name()).run().get(0);
            }
        }
    }

    private static float[] executeInceptionGraph(byte[] graphDef, Tensor image) {

        try (Graph g = new Graph()) {

            // Model loading: Using Graph.importGraphDef() to load a pre-trained Inception
            // model.
            g.importGraphDef(graphDef);

            // Graph execution: Using a Session to execute the graphs and find the best
            // label for an image.
            try (Session s = new Session(g);
                 @SuppressWarnings({"unchecked", "rawtypes"})
                 Tensor<Float> result = (Tensor<Float>) s.runner().feed("input", image).fetch("output").run().get(0)) {
                final long[] rshape = result.shape();
                if (result.numDimensions() != 2 || rshape[0] != 1) {
                    throw new RuntimeException(String.format(
                            "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                            Arrays.toString(rshape)));
                }
                int nlabels = (int) rshape[1];

                float[][] destinationArray = new float[1][nlabels];
                result.copyTo(destinationArray);
                return destinationArray[0];
            }
        }
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
