/*
 * Copyright (c) 2022, APT Group, Department of Computer Science,
 * School of Engineering, The University of Manchester. All rights reserved.
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
 */
package uk.ac.manchester.tornado.drivers.common.graal.compiler;

import jdk.vm.ci.meta.Local;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import uk.ac.manchester.tornado.api.exceptions.TornadoCompilationException;
import uk.ac.manchester.tornado.runtime.common.RuntimeUtilities;
import uk.ac.manchester.tornado.runtime.tasks.CompilableTask;

public class RuntimeReflectionUtils {

    /**
     * This method creates a list of arguments for a non-inlined method. The list is
     * created by matching the local variables of the method with the list of
     * arguments of the caller method. In the current test-cases, the caller method
     * is a {@link CompilableTask}. If the list of
     *
     * @param method
     * @param callerArgs
     * @return Object[]
     */
    public static Object[] resolveUnboxedArgsOfNonInlinedMethodFromCallerArgs(ResolvedJavaMethod method, Object[] callerArgs) throws TornadoCompilationException {
        final Local[] locals = method.getLocalVariableTable().getLocalsAt(0);
        final Object[] argsOfNonInlinedMethod = new Object[locals.length];

        for (int i = 0; i < argsOfNonInlinedMethod.length; i++) {
            for (Object callerArg : callerArgs) {
                if (locals[i].getType().toJavaName().equals(callerArg.getClass().getTypeName()) && !RuntimeUtilities.isBoxedPrimitiveClass(callerArg.getClass())) {
                    argsOfNonInlinedMethod[i] = callerArg;
                    break;
                }
            }
        }
        return argsOfNonInlinedMethod;
    }
}