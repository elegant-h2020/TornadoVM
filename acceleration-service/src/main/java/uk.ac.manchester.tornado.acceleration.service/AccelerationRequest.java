package uk.ac.manchester.tornado.acceleration.service;

import org.graalvm.compiler.core.GraalCompiler;
import uk.ac.manchester.tornado.drivers.opencl.virtual.VirtualDeviceDescriptor;
import uk.ac.manchester.tornado.runtime.common.TornadoInstalledCode;

public interface AccelerationRequest {
    void setRequest(GraalCompiler.Request request, VirtualDeviceDescriptor deviceDescriptor);

    TornadoInstalledCode getInstalledCode();

    void compile();
}
