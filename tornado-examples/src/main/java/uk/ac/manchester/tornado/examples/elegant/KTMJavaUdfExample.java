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
import uk.ac.manchester.tornado.api.TornadoExecutionResult;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.collections.math.TornadoMath;
import uk.ac.manchester.tornado.api.collections.types.Double3;
import uk.ac.manchester.tornado.api.collections.types.Double4;
import uk.ac.manchester.tornado.api.collections.types.Float3;
import uk.ac.manchester.tornado.api.collections.types.Float4;
import uk.ac.manchester.tornado.api.collections.types.VectorDouble3;
import uk.ac.manchester.tornado.api.collections.types.VectorDouble4;
import uk.ac.manchester.tornado.api.collections.types.VectorFloat3;
import uk.ac.manchester.tornado.api.collections.types.VectorFloat4;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

/**
 * <p>
 * How to run?
 * </p>
 * <code>
 *     tornado --printKernel --threadInfo --jvm="-Ds0.t0.device=0:0 -Dtornado.recover.bailout=False -Dtornado.cim.mode=True" -m tornado.examples/uk.ac.manchester.tornado.examples.elegant.KTMJavaUdfExample  1024 tornado 100
 * </code>
 *
 */
public class KTMJavaUdfExample {
    static class CanData {
        float Time;
        float ABS_Lean_Angle;
        float ABS_Pitch_Info;
        float ABS_Front_Wheel_Speed;
    }

    static class AggregationInput {
        float radius;
        float ABS_Lean_Angle;

        float ABS_Front_Wheel_Speed;
    }

    private static final float GRAVITATION = 9.81F;

    public static AggregationInput map(final CanData value) {
        AggregationInput output = new AggregationInput();
        float radians_lean = (float) Math.toRadians(value.ABS_Lean_Angle);
        float cotValue = (float) (Math.cos(radians_lean) / Math.sin(radians_lean));
        float speedValue = (float) Math.pow(value.ABS_Front_Wheel_Speed / 3.6F, 2);
        output.radius = (Math.abs(cotValue) * speedValue) / GRAVITATION;
        output.ABS_Lean_Angle = Math.abs(value.ABS_Lean_Angle);
        output.ABS_Front_Wheel_Speed = value.ABS_Front_Wheel_Speed;
        return output;
    }

    public static Float3 tornadoMap(final Float4 value) {
        Float3 output = new Float3();
        float radians_lean = TornadoMath.toRadians(value.getY());
        float cotValue = TornadoMath.cos(radians_lean) / TornadoMath.sin(radians_lean);
        float speedValue = TornadoMath.pow(value.getW() / 3.6F, 2);
        float radius = TornadoMath.abs(cotValue * speedValue) / GRAVITATION;
        output.setX(radius);
        output.setY(TornadoMath.abs(value.getY()));
        output.setZ(value.getW());
        return output;
    }

    public static void map(VectorFloat4 value, VectorFloat3 output) {
        for (@Parallel int i = 0; i < output.getLength(); i++) {
            output.set(i, tornadoMap(value.get(i)));
        }
    }

    public static boolean validate(AggregationInput[] resultJava, VectorFloat3 resultTornado) {
        boolean valid = true;

        for (int i = 0; i < resultJava.length; i++) {
            if (Math.abs(resultJava[i].radius - resultTornado.get(i).getX()) > 0.1) {
                System.out.println("Java resultJava[" + i + "].radius: " + resultJava[i].radius + " vs Tornado result (" + i + ").getX(): " + resultTornado.get(i).getX() + "\n");
                valid = false;
                break;
            }
            if (Math.abs(resultJava[i].ABS_Lean_Angle - resultTornado.get(i).getY()) > 0.1) {
                System.out.println("Java resultJava[" + i + "].ABS_Lean_Angle: " + resultJava[i].ABS_Lean_Angle + " vs Tornado result (" + i + ").getY(): " + resultTornado.get(i).getY() + "\n");
                valid = false;
                break;
            }
            if (Math.abs(resultJava[i].ABS_Front_Wheel_Speed - resultTornado.get(i).getZ()) > 0.1) {
                System.out.println(
                        "Java resultJava[" + i + "].ABS_Front_Wheel_Speed: " + resultJava[i].ABS_Front_Wheel_Speed + " vs Tornado result (" + i + ").getZ(): " + resultTornado.get(i).getZ() + "\n");
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

        VectorFloat4 input = new VectorFloat4(size);
        VectorFloat3 resultTornado = new VectorFloat3(size);
        CanData[] inputJava = new CanData[size];
        AggregationInput[] resultJava = new AggregationInput[size];

        for (int i = 0; i < size; i++) {
            float value = (float) Math.random();
            input.set(i, new Float4(value, value, value, value));
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
                    .task("t0", KTMJavaUdfExample::map, input, resultTornado) //
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
                    TornadoExecutionResult executionResult = executionPlan.execute();
                    end = System.nanoTime();
                    System.out.println("Total time of " + executionType + ":  " + (end - start) + " ns" + " \n");
                    System.out.println("The data transfers time is: " + executionResult.getProfilerResult().getDataTransfersTime());
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
