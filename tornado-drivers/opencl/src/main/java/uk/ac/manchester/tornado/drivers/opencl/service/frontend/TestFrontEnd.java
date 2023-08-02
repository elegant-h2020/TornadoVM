/*
 * This file is part of Tornado: A heterogeneous programming framework:
 * https://github.com/beehive-lab/tornadovm
 *
 * Copyright (c) 2013-2022, APT Group, Department of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Authors: Juan Fumero, Michalis Papadimitriou
 *
 */
package uk.ac.manchester.tornado.drivers.opencl.service.frontend;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import org.graalvm.compiler.phases.util.Providers;
import uk.ac.manchester.tornado.api.collections.types.VectorDouble2;
import uk.ac.manchester.tornado.api.collections.types.VectorDouble3;
import uk.ac.manchester.tornado.api.collections.types.VectorDouble4;
import uk.ac.manchester.tornado.api.collections.types.VectorDouble8;
import uk.ac.manchester.tornado.api.collections.types.VectorFloat2;
import uk.ac.manchester.tornado.api.collections.types.VectorFloat3;
import uk.ac.manchester.tornado.api.collections.types.VectorFloat4;
import uk.ac.manchester.tornado.api.collections.types.VectorFloat8;
import uk.ac.manchester.tornado.api.collections.types.VectorInt2;
import uk.ac.manchester.tornado.api.collections.types.VectorInt3;
import uk.ac.manchester.tornado.api.collections.types.VectorInt4;
import uk.ac.manchester.tornado.api.collections.types.VectorInt8;
import uk.ac.manchester.tornado.drivers.common.CompilerUtil;
import uk.ac.manchester.tornado.drivers.opencl.OCLDriver;
import uk.ac.manchester.tornado.drivers.opencl.OpenCL;
import uk.ac.manchester.tornado.drivers.opencl.graal.OCLProviders;
import uk.ac.manchester.tornado.drivers.opencl.graal.backend.OCLBackend;
import uk.ac.manchester.tornado.drivers.opencl.graal.compiler.OCLCompilationResult;
import uk.ac.manchester.tornado.drivers.opencl.graal.compiler.OCLCompiler;
import uk.ac.manchester.tornado.drivers.opencl.virtual.VirtualOCLTornadoDevice;
import uk.ac.manchester.tornado.runtime.TornadoCoreRuntime;
import uk.ac.manchester.tornado.runtime.common.RuntimeUtilities;
import uk.ac.manchester.tornado.runtime.common.TornadoAcceleratorDevice;
import uk.ac.manchester.tornado.runtime.common.TornadoOptions;
import uk.ac.manchester.tornado.runtime.graal.compiler.TornadoSuitesProvider;
import uk.ac.manchester.tornado.runtime.profiler.EmptyProfiler;
import uk.ac.manchester.tornado.runtime.sketcher.Sketch;
import uk.ac.manchester.tornado.runtime.tasks.CompilableTask;
import uk.ac.manchester.tornado.runtime.tasks.meta.ScheduleMetaData;
import uk.ac.manchester.tornado.runtime.tasks.meta.TaskMetaData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Test the front end of the service.
 *
 */
public class TestFrontEnd {

    private static int numberOfArgsPassed;
    private static int numberOfArgsFromSignature;
    private static int argSizesIndex = 0;
    private static String[] argNames;

    private static Class[] getMethodTypesFromClass(Class<?> klass, String methodName) {
        try {
            Method[] methods = klass.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals(methodName)) {
                    Class[] types = methods[i].getParameterTypes();
                    numberOfArgsFromSignature = types.length;
                    return types;
                }
            }
        } catch (SecurityException | IllegalArgumentException e) {
            throw new RuntimeException("[ERROR] Load class failed.", e);
        }
        throw new RuntimeException("No method found in the class file.");
    }

    private void checkParameterSizes() {
        if (numberOfArgsPassed != numberOfArgsFromSignature) {
            throw new RuntimeException(
                    "The number of parameters passed in JSON (" + numberOfArgsPassed + ") are not the same as the number of boxed types in the method (" + numberOfArgsFromSignature + ").");
        }
    }

    private static Object[] resolveParametersFromTypes(Class[] types, int[] parameterSizes) {
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            args[i] = lookupBoxedTypes(types[i], parameterSizes);
        }
        return args;
    }

    private static Object lookupBoxedTypes(Class type, int[] parameterSizes) {
        switch (type.getTypeName()) {
            case "int[]":
                return new int[parameterSizes[argSizesIndex++]];
            case "long[]":
                return new long[parameterSizes[argSizesIndex++]];
            case "float[]":
                return new float[parameterSizes[argSizesIndex++]];
            case "double[]":
                return new double[parameterSizes[argSizesIndex++]];
            case "int":
                return Integer.valueOf(0);
            case "long":
                return Long.valueOf(0);
            case "float":
                return Float.valueOf(0.0f);
            case "double":
                return Double.valueOf(0.0f);
            case "uk.ac.manchester.tornado.api.collections.types.VectorInt2":
                return new VectorInt2(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorInt3":
                return new VectorInt3(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorInt4":
                return new VectorInt4(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorInt8":
                return new VectorInt8(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorFloat2":
                return new VectorFloat2(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorFloat3":
                return new VectorFloat3(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorFloat4":
                return new VectorFloat4(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorFloat8":
                return new VectorFloat8(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorDouble2":
                return new VectorDouble2(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorDouble3":
                return new VectorDouble3(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorDouble4":
                return new VectorDouble4(parameterSizes[argSizesIndex++]);
            case "uk.ac.manchester.tornado.api.collections.types.VectorDouble8":
                return new VectorDouble8(parameterSizes[argSizesIndex++]);
            default:
                System.err.println("[TornadoVM] - type(" + type.getTypeName() + ") is not recognized by the frontend.");
                return null;
        }

    }

    public byte[] compileMethod(Class<?> klass, String methodName, TornadoAcceleratorDevice tornadoDevice, int[] parameterSizes) {

        // Get the method object to be compiled
        Method methodToCompile = CompilerUtil.getMethodForName(klass, methodName);

        // Get Tornado Runtime
        TornadoCoreRuntime tornadoRuntime = TornadoCoreRuntime.getTornadoRuntime();

        // Get the Graal Resolved Java Method
        ResolvedJavaMethod resolvedJavaMethod = tornadoRuntime.resolveMethod(methodToCompile);

        // Get the backend from TornadoVM
        OCLBackend openCLBackend = tornadoRuntime.getDriver(OCLDriver.class).getDefaultBackend();

        // Create a new task for TornadoVM
        ScheduleMetaData scheduleMetaData = new ScheduleMetaData("s0");
        // Create a compilable task
        Class[] types = getMethodTypesFromClass(klass, methodName);
        checkParameterSizes();
        Object[] parameters = resolveParametersFromTypes(types, parameterSizes);

        CompilableTask compilableTask = new CompilableTask(scheduleMetaData, "t0", methodToCompile, parameters);
        TaskMetaData taskMeta = compilableTask.meta();
        taskMeta.setDevice(tornadoDevice);

        // 1. Build Common Compiler Phase (Sketcher)
        // Utility to build a sketcher and insert into the HashMap for fast LookUps
        Providers providers = openCLBackend.getProviders();
        TornadoSuitesProvider suites = openCLBackend.getTornadoSuites();
        Sketch sketch = CompilerUtil.buildSketchForJavaMethod(resolvedJavaMethod, taskMeta, providers, suites);

        OCLCompilationResult compilationResult = OCLCompiler.compileSketchForDevice(sketch, compilableTask, (OCLProviders) providers, openCLBackend, new EmptyProfiler());

        return compilationResult.getTargetCode();
    }

    private Class readClassFromFile(File classFile) {
        if (!classFile.exists()) {
            throw new RuntimeException(classFile + " does not exist.");
        }
        Class klass = null;
        try {
            klass = Class.forName(classFile.getName().split("\\.")[0]); //
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return klass;
    }

    private String trimComma(String string) {
        return string.replaceFirst("\\,", "");
    }

    private int[] readArgSizesFromFile(File parameterSizeFile) {
        if (!parameterSizeFile.exists()) {
            throw new RuntimeException(parameterSizeFile + " does not exist.");
        }
        FileReader fileReader;
        BufferedReader bufferedReader;
        ArrayList<String> parsedArgNames = new ArrayList<>();
        ArrayList<Integer> parsedArgSizes = new ArrayList<>();

        try {
            fileReader = new FileReader(parameterSizeFile);
            bufferedReader = new BufferedReader(fileReader);
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, " :");
                while (tokenizer.hasMoreElements()) {
                    int numberOfTokensInLine = tokenizer.countTokens();
                    String token = tokenizer.nextToken();
                    if (token.contains("{") || token.contains("}")) {
                        break;
                    }
                    if (numberOfTokensInLine == 2) {
                        parsedArgNames.add(token);
                        parsedArgSizes.add(Integer.parseInt(trimComma(tokenizer.nextToken())));
                    }
                    numberOfArgsPassed++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Wrong parameter size file or invalid settings.");
        }

        if (numberOfArgsPassed > 0) {
            argNames = new String[numberOfArgsPassed];
            int[] argSizes = new int[numberOfArgsPassed];

            for (int i = 0; i < argSizes.length; i++) {
                argNames[i] = parsedArgNames.get(i);
                argSizes[i] = parsedArgSizes.get(i);
            }
            return argSizes;
        } else {
            throw new RuntimeException("No parameter size was loaded from file.");
        }
    }

    public void test(String[] args) {

        StringBuilder deviceInfoBuffer = new StringBuilder().append("\n");
        final int numDrivers = TornadoCoreRuntime.getTornadoRuntime().getNumDrivers();
        deviceInfoBuffer.append("Number of Tornado drivers: " + numDrivers + "\n");

        System.out.println(deviceInfoBuffer.toString());

        VirtualOCLTornadoDevice tornadoDevice = OpenCL.getDefaultVirtualDevice();
        System.out.println(tornadoDevice.getDescription());

        if (args.length != 0) {
            Class klass = readClassFromFile(new File(TornadoOptions.INPUT_CLASSFILE_DIR));
            int[] parameterSizes = readArgSizesFromFile(new File(TornadoOptions.PARAMETER_SIZE_DIR));

            byte[] sourceCode = compileMethod(klass, args[0], tornadoDevice, parameterSizes);
            RuntimeUtilities.maybePrintSource(sourceCode);
        } else {
            System.out.println("Please pass the method name as parameter.");
        }
    }

    public static void main(String[] args) {
        new TestFrontEnd().test(args);
    }

}
