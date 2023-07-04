/*
 * Copyright (c) 2022-2023, APT Group, Department of Computer Science,
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
package uk.ac.manchester.tornado.examples;

import java.util.Arrays;

import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

/**
 * <code>
 *     tornado --printKernel --jvm="-Dtornado.cim.mode=True" -m tornado.examples/uk.ac.manchester.tornado.examples.VectorAddIntCim --params 1024
 * </code>
 */
public class VectorAddIntCim {

    private static void vectorAdd(int[] a, int[] b, int[] c, int size) {
        for (@Parallel int i = 0; i < size; i++) {
            c[i] = a[i] + b[i];
        }
    }

    public static void main(String[] args) {
        int size = Integer.parseInt(args[0]);
        boolean isResultValid = true;

        int[] a = new int[size];
        int[] b = new int[size];
        int[] c = new int[size];
        int[] result = new int[size];

        Arrays.fill(a, 10);
        Arrays.fill(b, 20);

        TaskGraph taskGraph = new TaskGraph("s0") //
                .transferToDevice(DataTransferMode.EVERY_EXECUTION, a, b) //
                .task("t0", VectorAddIntCim::vectorAdd, a, b, c, size) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, c);

        ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();
        TornadoExecutionPlan executor = new TornadoExecutionPlan(immutableTaskGraph);

        boolean wrongResult;
        for (int idx = 0; idx < 10; idx++) {
            // Parallel
            executor.execute();
            // Sequential
            vectorAdd(a, b, result, size);

            // Check Result
            wrongResult = false;
            for (int j : c) {
                if (j != 30) {
                    wrongResult = true;
                    break;
                }
            }
            if (wrongResult) {
                isResultValid = false;

            }
        }
        System.out.println((isResultValid) ? "SUCCESS" : "FAILURE");
    }
}
