/*
 * Copyright (c) 2023, APT Group, School of Computer Science,
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

import java.util.Random;
import java.util.stream.IntStream;

import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.collections.math.TornadoMath;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

/**
 * Simple example of a trigonometric map operator with TornadoVM.
 */
public class ElegantMapExample {

    private static final int SIZE = 16;
    private static final float GRAVITATION = 9.81F;

    private static void computeTrigonometricMap(float[] lean, int[] speed, float[] output, final int size) {
        for (@Parallel int i = 0; i < size; i++) {
            float cotValue = TornadoMath.cos(TornadoMath.toRadians(lean[i])) / TornadoMath.sin(TornadoMath.toRadians(lean[i]));
            float speedValue = TornadoMath.pow(speed[i] / 3.6F, 2) / GRAVITATION;
            float value = cotValue * speedValue;
            output[i] = TornadoMath.abs(value);
        }
    }

    public static boolean validateResults(float[] outputTornado, float[] outputJava) {
        boolean check = true;
        for (int i = 0; i < outputTornado.length; i++) {
            if (outputTornado[i] != outputJava[i]) {
                check = false;
            }
        }
        return check;
    }

    public static void main(String[] args) {

        float[] lean = new float[SIZE * SIZE];
        int[] speed = new int[SIZE * SIZE];
        float[] outputTornado = new float[SIZE * SIZE];
        float[] outputJava = new float[SIZE * SIZE];

        Random r = new Random();
        IntStream.range(0, lean.length).forEach(i -> {
            lean[i] = 50 + r.nextInt(100);
        });

        TaskGraph taskGraph = new TaskGraph("s0") //
                .transferToDevice(DataTransferMode.EVERY_EXECUTION, lean, speed) //
                .task("t0", ElegantMapExample::computeTrigonometricMap, lean, speed, outputTornado, SIZE) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, outputTornado);

        ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();
        TornadoExecutionPlan executorPlan = new TornadoExecutionPlan(immutableTaskGraph);
        executorPlan.execute();

        computeTrigonometricMap(lean, speed, outputJava, SIZE);

        if (validateResults(outputTornado, outputJava)) {
            System.out.println("Validation true");
        } else {
            System.out.println("Validation false");
        }
    }
}
