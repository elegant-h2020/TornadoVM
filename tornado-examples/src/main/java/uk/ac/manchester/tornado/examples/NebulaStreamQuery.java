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
package uk.ac.manchester.tornado.examples;

import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.collections.types.Int2;
import uk.ac.manchester.tornado.api.collections.types.Int4;
import uk.ac.manchester.tornado.api.collections.types.VectorInt2;
import uk.ac.manchester.tornado.api.collections.types.VectorInt4;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

/**
 * <p>
 * How to run?
 * </p>
 * <code>
 *     tornado --printKernel --threadInfo --jvm "-Ds0.t0.device=0:1 -Dtornado.recover.bailout=False -Dtornado.cim.mode=True" -m tornado.examples/uk.ac.manchester.tornado.examples.NebulaStreamQuery
 * </code>
 *
 */
public class NebulaStreamQuery {

    public static void computeNesMap(VectorInt2 inputTuples, VectorInt4 resultTuples, int numberOfTuples) {
        for (@Parallel int recordIndex = 0; recordIndex < numberOfTuples; ++recordIndex) {
            int default_logical$new1 = (inputTuples.get(recordIndex).getX() * 2);
            int default_logical$new2 = (inputTuples.get(recordIndex).getX() + 2);
            resultTuples.set(recordIndex, new Int4(inputTuples.get(recordIndex).getX(), inputTuples.get(recordIndex).getY(), default_logical$new1, default_logical$new2));
        }
    }

    public static void main(String[] args) {

        int size = 16;
        boolean valid = true;

        VectorInt2 input = new VectorInt2(size);
        VectorInt4 result = new VectorInt4(size);

        int numberOfTuples = 16;

        for (int i = 0; i < input.getLength(); i++) {
            input.set(i, new Int2(i, i));
        }

        TaskGraph taskGraph = new TaskGraph("s0") //
                .transferToDevice(DataTransferMode.FIRST_EXECUTION, input) //
                .task("t0", NebulaStreamQuery::computeNesMap, input, result, numberOfTuples) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, result);

        ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();
        TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(immutableTaskGraph);
        executionPlan.execute();

        for (int i = 0; i < size; i++) {
            if ((result.get(i).getZ() - input.get(i).getX() * 2) > 0.01f) {
                System.out.println("Result [" + i + "].default_logical$new1: " + result.get(i).getZ() + ", it should be: " + (input.get(i).getX() * 2));
                valid = false;
                break;
            }

            if ((result.get(i).getW() - (input.get(i).getX() + 2)) > 0.01f) {
                System.out.println("Result [" + i + "].default_logical$new2: " + result.get(i).getW() + ", it should be: " + (input.get(i).getX() + 2));
                valid = false;
                break;
            }
        }

        if (valid) {
            System.out.println("Result is correct");
        } else {
            System.out.println("Result is not correct");
        }
    }
}
