package uk.ac.manchester.tornado.drivers.common;

import jdk.vm.ci.code.CompilationRequest;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import org.graalvm.compiler.core.target.Backend;
import org.graalvm.compiler.lir.asm.CompilationResultBuilderFactory;
import org.graalvm.compiler.lir.phases.LIRSuites;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.phases.OptimisticOptimizations;
import org.graalvm.compiler.phases.PhaseSuite;
import org.graalvm.compiler.phases.tiers.HighTierContext;
import org.graalvm.compiler.phases.tiers.Suites;
import org.graalvm.compiler.phases.util.Providers;

public class TornadoVMCompiler<T extends CompilationRequest> {
    public TornadoVMCompiler() {

    }

    // public static void compileGraph(StructuredGraph graph, ResolvedJavaMethod
    // installedCodeOwner, Providers providers, Backend backend) {
    // System.out.println("--- TornadoVM - new compileGraph");
    // }

    public void compileGraph(StructuredGraph graph, ResolvedJavaMethod installedCodeOwner, Providers providers, Backend backend, PhaseSuite<HighTierContext> graphBuilderSuite,
            OptimisticOptimizations optimisticOpts, ProfilingInfo profilingInfo, Suites suites, LIRSuites lirSuites, T compilationResult, CompilationResultBuilderFactory factory,
            boolean verifySourcePositions) {
        System.out.println("--- TornadoVM - compileGraph signature");
    }

    public static void compileGraph2() {
        // public static <T extends CompilationResult> T compileGraph(StructuredGraph
        // graph, ResolvedJavaMethod installedCodeOwner, Providers providers, Backend
        // backend, PhaseSuite<HighTierContext> graphBuilderSuite,
        // OptimisticOptimizations optimisticOpts, ProfilingInfo profilingInfo, Suites
        // suites, LIRSuites lirSuites, T compilationResult,
        // CompilationResultBuilderFactory factory, boolean verifySourcePositions) {
        // Request<OCLCompilationResult> r = new Request<>(graph, installedCodeOwner,
        // null, null, providers, (OCLBackend) backend, graphBuilderSuite,
        // optimisticOpts,
        // profilingInfo, null, null, compilationResult, factory, true, true, 0);
        // // TODO Suites, backend, lirSuites, CompilationResult
        // CompilationResult result = r.execute();
        // return (T) result;
        System.out.println("--- TornadoVM - compileGraph2");
        // return null;
    }

}
