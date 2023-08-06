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
import uk.ac.manchester.tornado.api.collections.types.Double4;
import uk.ac.manchester.tornado.api.collections.types.VectorDouble2;
import uk.ac.manchester.tornado.api.collections.types.VectorDouble4;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

/**
 * <p>
 * How to run?
 * </p>
 * <code>
 *     tornado --printKernel --threadInfo --jvm="-Ds0.t0.device=0:0 -Dtornado.recover.bailout=False -Dtornado.cim.mode=True" -m tornado.examples/uk.ac.manchester.tornado.examples.elegant.KTMJavaUdfDoubleExample 1024 tornado 100
 * </code>
 *
 */
public class KTMJavaUdfDoubleExample {
    static class CanData {
        double Time;
        double ABS_Lean_Angle;
        double ABS_Pitch_Info;
        double ABS_Front_Wheel_Speed;
    }

    static class AggregationInput {
        double aggregation_input;
        double ABS_LEAN_ANGLE;
    }

    private static final float GRAVITATION = 9.81F;

    public static AggregationInput map(final CanData value) {
        AggregationInput output = new AggregationInput();
        double radians_lean = Math.toRadians(value.ABS_Lean_Angle);
        double cotValue = Math.cos((radians_lean) / Math.sin(radians_lean));
        double speedValue = Math.pow(value.ABS_Front_Wheel_Speed / 3.6F, 2);
        output.aggregation_input = Math.abs(cotValue * speedValue) / GRAVITATION;
        return output;
    }

    public static Double2 tornadoMap(final Double4 value) {
        Double2 output = new Double2();
        double radians_lean = TornadoMath.toRadians(value.getY());
        double cotValue = TornadoMath.cos((radians_lean) / TornadoMath.sin(radians_lean));
        double speedValue = TornadoMath.pow(value.getW() / 3.6F, 2);
        double aggregation_input = TornadoMath.abs(cotValue * speedValue) / GRAVITATION;
        output.setX(aggregation_input);
        return output;
    }

    public static void map(VectorDouble4 value, VectorDouble2 output) {
        for (@Parallel int i = 0; i < output.getLength(); i++) {
            output.set(i, tornadoMap(value.get(i)));
        }
    }

    public static boolean validate(AggregationInput[] resultJava, VectorDouble2 resultTornado) {
        boolean valid = true;

        for (int i = 0; i < resultJava.length; i++) {
            if (Math.abs(resultJava[i].aggregation_input - resultTornado.get(i).getX()) > 0.1) {
                System.out.println("Java resultJava[" + i + "].aggregation_input: " + resultJava[i].aggregation_input + " vs Tornado result (" + i + ").getX(): " + resultTornado.get(i).getX() + "\n");
                valid = false;
                break;
            }
            if (Math.abs(resultJava[i].ABS_LEAN_ANGLE - resultTornado.get(i).getY()) > 0.1) {
                System.out.println("Java resultJava[" + i + "].ABS_LEAN_ANGLE: " + resultJava[i].ABS_LEAN_ANGLE + " vs Tornado result (" + i + ").getY(): " + resultTornado.get(i).getY() + "\n");
                valid = false;
                break;
            }
        }
        return valid;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: <size> <mode:tornado|sequential> <iterations>");
            System.exit(-1);
        }

        final int size = Integer.parseInt(args[0]);
        String executionType = args[1];
        int iterations = Integer.parseInt(args[2]);
        long end,start;

        VectorDouble4 input = new VectorDouble4(size);
        VectorDouble2 resultTornado = new VectorDouble2(size);
        CanData[] inputJava = new CanData[size];
        AggregationInput[] resultJava = new AggregationInput[size];

        for (int i = 0; i < size; i++) {
            float value = (float) Math.random();
            input.set(i, new Double4(value, value, value, value));
            inputJava[i] = new CanData();
            inputJava[i].Time = value;
            inputJava[i].ABS_Front_Wheel_Speed = value;
            inputJava[i].ABS_Lean_Angle = value;
            inputJava[i].ABS_Pitch_Info = value;
        }

        TaskGraph graph = new TaskGraph("s0");
        TornadoExecutionPlan executionPlan = null;
        if (!executionType.equals("sequential")) {
            long startInit = System.nanoTime();
            graph.transferToDevice(DataTransferMode.EVERY_EXECUTION, input) //
                    .task("t0", KTMJavaUdfDoubleExample::map, input, resultTornado) //
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
