/*
 * Copyright (c) 2023, APT Group, Department of Computer Science,
 * The University of Manchester.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.manchester.tornado.examples.elegant;

import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.collections.math.TornadoMath;
import uk.ac.manchester.tornado.api.collections.types.Double2;
import uk.ac.manchester.tornado.api.collections.types.VectorDouble2;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

/**
 * <p>
 * How to run?
 * </p>
 * <code>
 *     tornado --printKernel --threadInfo --jvm="-Ds0.t0.device=0:0 -Dtornado.recover.bailout=False -Dtornado.cim.mode=True" -m tornado.examples/uk.ac.manchester.tornado.examples.elegant.NESJavaUdfDoubleExample 1024 tornado 100
 * </code>
 *
 */
public class NESJavaUdfDoubleExample {

    // This is the input type of the UDF.
    // The names of the fields `x` and `y` must correspond to the schema of the
    // input stream.
    static class CartesianCoordinate {
        double x;
        double y;
    }

    // This is the output type of the UDF.
    // The schema of the output stream is derived from the names of the fields
    // `angle` and `radius`.
    static class PolarCoordinate {
        double angle;
        double radius;
    }

    public static PolarCoordinate map(final CartesianCoordinate inputmap) {
        PolarCoordinate output = new PolarCoordinate();
        output.radius = Math.sqrt(inputmap.x * inputmap.x + inputmap.y * inputmap.y);
        output.angle = Math.atan2(inputmap.x, inputmap.y);
        return output;
    }

    public static Double2 tornadoMap(final Double2 value) {
        double radius = TornadoMath.sqrt(value.getX() * value.getX() + value.getY() * value.getY());
        double angle = TornadoMath.atan2(value.getX(), value.getY());
        Double2 output = new Double2(angle, radius);

        return output;
    }

    public static void map(VectorDouble2 value, VectorDouble2 output) {
        for (@Parallel int i = 0; i < output.getLength(); i++) {
            output.set(i, tornadoMap(value.get(i)));
        }
    }

    public static boolean validate(PolarCoordinate[] resultJava, VectorDouble2 resultTornado) {
        boolean valid = true;

        for (int i = 0; i < resultJava.length; i++) {
            if (Math.abs(resultJava[i].angle - resultTornado.get(i).getX()) > 0.1) {
                System.out.println("Java resultJava[" + i + "].angle: " + resultJava[i].angle + " vs Tornado result (" + i + ").getX(): " + resultTornado.get(i).getX() + "\n");
                valid = false;
                break;
            }
            if (Math.abs(resultJava[i].radius - resultTornado.get(i).getY()) > 0.1) {
                System.out.println("Java resultJava[" + i + "].radius: " + resultJava[i].radius + " vs Tornado result (" + i + ").getY(): " + resultTornado.get(i).getY() + "\n");
                valid = false;
                break;
            }
        }
        return valid;
    }

    public static void main(String[] args) {
        final int size = Integer.parseInt(args[0]);
        String executionType = args[1];
        int iterations = Integer.parseInt(args[2]);
        long end,start;

        VectorDouble2 input = new VectorDouble2(size);
        VectorDouble2 resultTornado = new VectorDouble2(size);
        CartesianCoordinate[] inputJava = new CartesianCoordinate[size];
        PolarCoordinate[] resultJava = new PolarCoordinate[size];

        for (int i = 0; i < size; i++) {
            float value = (float) Math.random();
            input.set(i, new Double2(value, value));
            inputJava[i] = new CartesianCoordinate();
            inputJava[i].x = value;
            inputJava[i].y = value;
        }

        TaskGraph graph = new TaskGraph("s0");
        TornadoExecutionPlan executionPlan = null;
        if (!executionType.equals("sequential")) {
            long startInit = System.nanoTime();
            graph.transferToDevice(DataTransferMode.EVERY_EXECUTION, input) //
                    .task("t0", NESJavaUdfDoubleExample::map, input, resultTornado) //
                    .transferToHost(DataTransferMode.EVERY_EXECUTION, resultTornado);

            ImmutableTaskGraph immutableTaskGraph = graph.snapshot();
            executionPlan = new TornadoExecutionPlan(immutableTaskGraph);
            long stopInit = System.nanoTime();
            System.out.println("Initialization time:  " + (stopInit - startInit) + " ns" + "\n");
        }

        System.out.println("Version running: " + executionType + " ! ");
        for (int i = 0; i < iterations; i++) {
            System.gc();
            switch (executionType) {
                case "tornado":
                    start = System.nanoTime();
                    executionPlan.execute();
                    end = System.nanoTime();
                    System.out.println("Total time of " + executionType + ":  " + (end - start) + " ns" + " \n");
                    break;
                case "sequential":
                    start = System.nanoTime();
                    map(input, resultTornado);
                    end = System.nanoTime();
                    System.out.println("Total time of " + executionType + ":  " + (end - start) + " ns" + " \n");
                    break;
                default:
                    System.out.println("Wrong execution type! Try \"tornado\" or \"sequential\".");
                    break;
            }
        }

        for (int i = 0; i < inputJava.length; i++) {
            resultJava[i] = map(inputJava[i]);
        }

        if (validate(resultJava, resultTornado)) {
            System.out.println("Result is correct");
        } else {
            System.out.println("Result is not correct");
        }
    }
}
