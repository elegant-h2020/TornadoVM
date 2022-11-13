package uk.ac.manchester.tornado.runtime.acceleration.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static uk.ac.manchester.tornado.runtime.common.TornadoOptions.METHOD_PATH_FOR_SERVICE;
import static uk.ac.manchester.tornado.runtime.common.TornadoOptions.SERVICE_DESTINATION_DIR;

public class Boilerplate {
    private static final String SUFFIX = ".java";
    private static StringBuilder stringBuilder;
    private static String signatureOfMethod;
    private static String methodBody;
    private static int numberOfArguments;
    private static long numberOfArrayArguments;
    private static String[] arrayOfParameterNames;
    private static String[] arrayOfParameterSizes = { "1024", "1024", "1024" }; // FIXME

    private static String[] arrayOfParameterTypes;
    private static String[] arrayOfSizeVariables;

    private static String methodName;
    private static String className = "Test";

    private static void emitPackagePrologue(StringBuilder sb) {
        sb.append("package uk.ac.manchester.tornado.examples.virtual;\n" + "\n" //
                + "import uk.ac.manchester.tornado.api.TaskSchedule;\n" //
                + "import uk.ac.manchester.tornado.api.annotations.Parallel;\n" //
                + "import uk.ac.manchester.tornado.api.annotations.Reduce;\n" //
                + "import java.util.stream.IntStream;");
        sb.append("\n");
    }

    private static void emitClassBegin(StringBuilder sb) {
        sb.append("\n");
        sb.append("public class ");
        sb.append(className);
        sb.append(" {");
        sb.append("\n");
    }

    private static void emitSizesOfParameters(StringBuilder sb) {
        sb.append("\n");
        for (int i = 0; i < numberOfArrayArguments; i++) {
            arrayOfSizeVariables[i] = arrayOfParameterNames[i] + "Size";
            sb.append("private static int ");
            sb.append(arrayOfSizeVariables[i]);
            sb.append(" = ");
            sb.append(arrayOfParameterSizes[i]);
            sb.append(";\n");
        }
        sb.append("\n");
    }

    private static void emitMethod(StringBuilder sb, String method) {
        sb.append("\n");
        sb.append(method);
    }

    private static void emitMainMethod(StringBuilder sb) {
        sb.append("\n");
        sb.append("public static void main(String[] args) {");
        sb.append("\n");
    }

    private static void emitDeclarationOfParameters(StringBuilder sb) {
        sb.append("\n");
        for (int i = 0; i < numberOfArguments; i++) {
            sb.append("    ");
            sb.append(arrayOfParameterTypes[i]);
            sb.append(" ");
            sb.append(arrayOfParameterNames[i]);
            sb.append(" = new ");
            sb.append(arrayOfParameterTypes[i].replaceFirst("\\]", ""));
            sb.append(arrayOfSizeVariables[i]);
            sb.append("];\n");
        }
        sb.append("\n");
    }

    private static void emitInitializationOfParameters(StringBuilder sb) {
        sb.append("\n");
        for (int i = 0; i < numberOfArrayArguments; i++) {
            sb.append("    ");
            sb.append("IntStream.range(0, ");
            sb.append(arrayOfSizeVariables[i]);
            sb.append(").forEach(idx -> {\n");
            sb.append("        ");
            sb.append(arrayOfParameterNames[i]);
            sb.append("[idx] = (");
            sb.append(arrayOfParameterTypes[i].replaceAll("\\[|\\]", ""));
            sb.append(") idx;");
            sb.append("\n");
            sb.append("    ");
            sb.append("});\n");
        }
    }

    private static void emitTaskSchedule(StringBuilder sb) {
        sb.append("\n");
        //@formatter:off
        sb.append("    ");
        sb.append("new TaskSchedule(\"virtual\")");
        sb.append("\n");
        sb.append("        ");
        sb.append(".task(\"");
        sb.append(methodName);
        sb.append("\", ");
        sb.append(className);
        sb.append("::");
        sb.append(methodName);
        for (int i = 0; i < numberOfArguments; i++) {
            sb.append(", ");
            sb.append(arrayOfParameterNames[i]);
        }
        sb.append(")\n");
        sb.append("        ");
        sb.append(".execute();\n");
        //@formatter:on
        sb.append("\n");
    }

    private static void emitClosingBraceForMainMethod(StringBuilder sb) {
        sb.append("    }");
        sb.append("\n");
    }

    private static void emitClosingBraceForClass(StringBuilder sb) {
        sb.append("}");
        sb.append("\n");
    }

    private static String extractMethodFromFileToString(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String extractSignature(String line) {
        return line.replaceFirst(" \\{|\\{", ";");
    }

    private static int getNumberOfArguments(String signature) {
        if (signature.chars().filter(ch -> ch == ',').count() == 0) {
            return 0;
        }
        return (int) signature.chars().filter(ch -> ch == ',').count() + 1;
    }

    private static long getNumberOfArrayArguments(String signature) {
        if (signature.chars().filter(ch -> ch == '[').count() == 0) {
            return 0;
        }
        return signature.chars().filter(ch -> ch == '[').count();
    }

    private static String getClassNameAppendedWithMethodName(String methodName) {
        return className += methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    private static String trimFirstSpaceFromString(String string) {
        if (string.startsWith(" ")) {
            string = string.replaceFirst("\\s+", "");
        }
        return string;
    }

    private static String trimParenthesisSuffixFromString(String string) {
        if (string.contains(")")) {
            return string.split("\\)")[0];
        }
        return string;
    }

    private static void retrieveParameterInfoFromSignature(String signatureName) {
        String[] strings = signatureName.split("\\(");
        String[] subStrings = strings[1].split("\\,|\\, ");

        for (int i = 0; i < subStrings.length; i++) {
            String curSubString = trimFirstSpaceFromString(subStrings[i]);
            curSubString = trimParenthesisSuffixFromString(curSubString);
            if (curSubString.contains("[]")) {
                arrayOfParameterTypes[i] = curSubString.split(" ")[0];
                arrayOfParameterNames[i] = curSubString.split(" ")[1];
            }
        }

    }

    private static String getMethodNameFromSignature(String signatureName) {
        String[] strings = signatureName.split("\\(");
        String[] subStrings = strings[0].split(" ");
        return subStrings[subStrings.length - 1];
    }

    private static String getSignatureOfMethodFile(String fileName) {
        FileReader fileReader;
        BufferedReader bufferedReader;
        try {
            fileReader = new FileReader(fileName);
            bufferedReader = new BufferedReader(fileReader);
            String line;
            line = bufferedReader.readLine();
            signatureOfMethod = extractSignature(line);
        } catch (IOException e) {
            System.err.println("Input fileName [" + fileName + "] failed to run." + e.getMessage());
        }
        return signatureOfMethod;
    }

    private static void writeClassToFile(String classContent, String fileName) {
        FileWriter fileWriter;
        BufferedWriter bufferedWriter;
        try {
            fileWriter = new FileWriter(fileName);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.append(classContent);
            bufferedWriter.close();
        } catch (IOException e) {
            System.err.println("Error in writing of the generated class to file [" + fileName + "]." + e.getMessage());
        }
    }

    private static String generateBoilerplateCode() {
        stringBuilder = new StringBuilder();
        emitPackagePrologue(stringBuilder);
        emitClassBegin(stringBuilder);
        emitSizesOfParameters(stringBuilder);
        emitMethod(stringBuilder, methodBody);
        emitMainMethod(stringBuilder);
        emitDeclarationOfParameters(stringBuilder);
        emitInitializationOfParameters(stringBuilder);
        emitTaskSchedule(stringBuilder);
        emitClosingBraceForMainMethod(stringBuilder);
        emitClosingBraceForClass(stringBuilder);
        return stringBuilder.toString();
    }

    private static void initializeConfigurations(String filePath) {
        methodBody = extractMethodFromFileToString(filePath);
        signatureOfMethod = getSignatureOfMethodFile(filePath);
        methodName = getMethodNameFromSignature(signatureOfMethod);
        numberOfArguments = getNumberOfArguments(signatureOfMethod);
        numberOfArrayArguments = getNumberOfArguments(signatureOfMethod);
        arrayOfParameterNames = new String[numberOfArguments];
        arrayOfParameterTypes = new String[numberOfArguments];

        retrieveParameterInfoFromSignature(signatureOfMethod);
        className = getClassNameAppendedWithMethodName(methodName);

        arrayOfSizeVariables = new String[(int) numberOfArrayArguments];
    }

    public static void main(String[] args) {

        initializeConfigurations(METHOD_PATH_FOR_SERVICE);

        String classContent = generateBoilerplateCode();

        writeClassToFile(classContent, SERVICE_DESTINATION_DIR + className + SUFFIX);
    }
}
