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

import java.io.File;
import java.lang.reflect.Method;

import org.graalvm.compiler.phases.util.Providers;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import uk.ac.manchester.tornado.api.annotations.Parallel;
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

/**
 * Test the front end of the service.
 *
 */
public class TestFrontEnd {

    public static final int SIZE = 128;

    public static void methodToCompile(int[] a, int[] b, double[] c, int alpha) {
        for (@Parallel int i = 0; i < SIZE; i++) {
            c[i] = 0.12 * a[i] * b[i] + alpha;
        }
    }

    private static Class[] getMethodTypesFromClass(Class<?> klass, String methodName) {
        try {
            Method[] methods = klass.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals(methodName)) {
                    System.out.println("method[" + i + "]: " + methods[i].getName());
                    Class[] types = methods[i].getParameterTypes();
                    return types;
                }
            }
        } catch (SecurityException | IllegalArgumentException e) {
            throw new RuntimeException("[ERROR] Load class failed.", e);
        }
        return null;
    }

    private static Object[] resolveParametersFromTypes(Class[] types) {
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            args[i] = lookupBoxedTypes(types[i]);
            System.out.println("args[" + i + "]: " + args[i]);
        }
        return args;
    }

    private static Object lookupBoxedTypes(Class type) {
        System.out.println("type.getName(): " + type.getName());
        System.out.println("type.getTypeName(): " + type.getTypeName());
        System.out.println("type.getSimpleName(): " + type.getSimpleName());
        System.out.println("type.getCanonicalName(): " + type.getCanonicalName());

        switch (type.getTypeName()) {
            case "int[]":
                return new int[128];
            case "long[]":
                return new long[128];
            case "float[]":
                return new float[128];
            case "double[]":
                return new double[128];
            case "int":
                return Integer.valueOf(0);
            case "long":
                return Long.valueOf(0);
            case "float":
                return Float.valueOf(0.0f);
            case "double":
                return Double.valueOf(0.0f);
            default:
                return null;
        }

    }

    public byte[] compileMethod(Class<?> klass, String methodName, TornadoAcceleratorDevice tornadoDevice) {

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
        Object[] parameters = resolveParametersFromTypes(types);

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

    public void test(String[] args) {

        StringBuilder deviceInfoBuffer = new StringBuilder().append("\n");
        final int numDrivers = TornadoCoreRuntime.getTornadoRuntime().getNumDrivers();
        deviceInfoBuffer.append("Number of Tornado drivers: " + numDrivers + "\n");

        System.out.println(deviceInfoBuffer.toString());

        VirtualOCLTornadoDevice tornadoDevice = OpenCL.getDefaultVirtualDevice();
        System.out.println(tornadoDevice.getDescription());

        if (args.length != 0) {
            File file = new File(TornadoOptions.INPUT_CLASSFILE_DIR);
            Class klass = null;
            try {
                klass = Class.forName(file.getName().split("\\.")[0]); //
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            byte[] sourceCode = compileMethod(klass, args[0], tornadoDevice);
            RuntimeUtilities.maybePrintSource(sourceCode);
        } else {
            System.out.println("Please pass the method name as parameter.");
        }
    }

    public static void main(String[] args) {
        new TestFrontEnd().test(args);
    }

}
