package com.github.fbascheper.alerts.util.tensorflow;

import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Output;
import org.tensorflow.Tensor;
import org.tensorflow.types.UInt8;

/**
 * In the fullness of time, equivalents of the methods of this class should be auto-generated from
 * the OpDefs linked into libtensorflow_jni.so.
 * That would match what is done in other languages like Python, C++ and Go.
 *
 * @author Erik-Berndt Scheper
 * @see <a href="https://github.com/tensorflow/tensorflow/blob/master/tensorflow/java/src/main/java/org/tensorflow/examples/LabelImage.java#L160" >TensorFlow Java example</a>
 * @since 13-09-2018
 */
final class TensorFlowGraphBuilder {
    private final Graph g;

    TensorFlowGraphBuilder(Graph g) {
        this.g = g;
    }

    Output<Float> div(Output<Float> x, Output<Float> y) {
        return binaryOp("Div", x, y);
    }

    <T> Output<T> sub(Output<T> x, Output<T> y) {
        return binaryOp("Sub", x, y);
    }

    <T> Output<Float> resizeBilinear(Output<T> images, Output<Integer> size) {
        return binaryOp3("ResizeBilinear", images, size);
    }

    <T> Output<T> expandDims(Output<T> input, Output<Integer> dim) {
        return binaryOp3("ExpandDims", input, dim);
    }

//    Output cast(Output value, DataType dtype) {
//        return g.opBuilder("Cast", "Cast").addInput(value).setAttr("DstT", dtype).build().output(0);
//    }
//

    <T, U> Output<U> cast(Output<T> value, Class<U> type) {
        DataType dtype = DataType.fromClass(type);
        return g.opBuilder("Cast", "Cast")
                .addInput(value)
                .setAttr("DstT", dtype)
                .build()
                .output(0);
    }

    Output<UInt8> decodeJpeg(Output<String> contents, long channels) {
        return g.opBuilder("DecodeJpeg", "DecodeJpeg")
                .addInput(contents)
                .setAttr("channels", channels)
                .build()
                .output(0);
    }

    Output<String> constant(String name, byte[] value) {
        return this.constant(name, value, String.class);
    }

    Output<Integer> constant(String name, int value) {
        return this.constant(name, value, Integer.class);
    }

    Output<Integer> constant(String name, int[] value) {
        return this.constant(name, value, Integer.class);
    }

    Output<Float> constant(String name, float value) {
        return this.constant(name, value, Float.class);
    }

    private <T> Output<T> constant(String name, Object value, Class<T> type) {
        try (Tensor<T> t = Tensor.create(value, type)) {
            return g.opBuilder("Const", name)
                    .setAttr("dtype", DataType.fromClass(type))
                    .setAttr("value", t)
                    .build()
                    .output(0);
        }
    }

    private <T> Output<T> binaryOp(String type, Output<T> in1, Output<T> in2) {
        return g.opBuilder(type, type).addInput(in1).addInput(in2).build().output(0);
    }

    private <T, U, V> Output<T> binaryOp3(String type, Output<U> in1, Output<V> in2) {
        return g.opBuilder(type, type).addInput(in1).addInput(in2).build().output(0);
    }
}
