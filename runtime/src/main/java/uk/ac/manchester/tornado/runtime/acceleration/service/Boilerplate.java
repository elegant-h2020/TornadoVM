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
    private static String classContent;
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

    private static String extractMethodFromFileToString(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void emitPackagePrologue() {
        classContent = "package uk.ac.manchester.tornado.examples.virtual;\n" + "\n" //
                + "import uk.ac.manchester.tornado.api.TaskSchedule;\n" //
                + "import uk.ac.manchester.tornado.api.annotations.Parallel;\n" //
                + "import uk.ac.manchester.tornado.api.annotations.Reduce;\n" //
                + "import java.util.stream.IntStream;";
        classContent += "\n";
    }

    private static void emitClassBegin() {
        classContent += "\n";
        classContent += "public class " + className + " {";
        classContent += "\n";
    }

    private static void emitSizesOfParameters() {
        classContent += "\n";
        for (int i = 0; i < numberOfArrayArguments; i++) {
            arrayOfSizeVariables[i] = arrayOfParameterNames[i] + "Size";
            classContent += "private static int " + arrayOfSizeVariables[i] + " = " + arrayOfParameterSizes[i] + ";\n";
        }
        classContent += "\n";
    }

    private static void emitMethod(String method) {
        classContent += "\n";
        classContent += method;
    }

    private static void emitMainMethod() {
        classContent += "\n";
        classContent += "public static void main(String[] args) {";
        classContent += "\n";
    }

    private static void emitDeclarationOfParameters() {
        classContent += "\n";
        for (int i = 0; i < numberOfArguments; i++) {
            classContent += "    " + arrayOfParameterTypes[i] + " " + arrayOfParameterNames[i] + " = new " + arrayOfParameterTypes[i].replaceFirst("\\]", "") + arrayOfSizeVariables[i] + "];\n";
        }
        classContent += "\n";
    }

    private static void emitInitializationOfParameters() {
        classContent += "\n";
        for (int i = 0; i < numberOfArrayArguments; i++) {
            classContent += "    " + "IntStream.range(0, " + arrayOfSizeVariables[i] + ").forEach(idx -> {\n";
            classContent += "        " + arrayOfParameterNames[i] + "[idx] = (";
            classContent += arrayOfParameterTypes[i].replaceAll("\\[|\\]", "") + ") idx;" + "\n";
            classContent += "    " + "});\n";
        }
    }

    private static void emitTaskSchedule() {
        classContent += "\n";
        //@formatter:off
        classContent += "    " + "new TaskSchedule(\"virtual\")" + "\n";
//        classContent += "    " + ".streamIn(" + input+")\n";
        classContent += "        " + ".task(\"" + methodName + "\", " + className +"::" + methodName;
        for (int i = 0; i < numberOfArguments; i++) {
            classContent += ", " + arrayOfParameterNames[i];
        }
        classContent += ")\n";
//        classContent += "        " + ".streamOut(" + result ")\n";
        classContent += "        " + ".execute();\n";
        //@formatter:on
        classContent += "\n";
    }

    private static void emitClosingBraceForMainMethod() {
        classContent += "    }";
        classContent += "\n";
    }

    private static void emitClosingBraceForClass() {
        classContent += "}";
        classContent += "\n";
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
            System.out.println("Input fileName [" + fileName + "] failed to run." + e.getMessage());
        }
        return signatureOfMethod;
    }

    private static String writeClassToFile(String fileName) {
        FileWriter fileWriter;
        BufferedWriter bufferedWriter;
        try {
            fileWriter = new FileWriter(fileName);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.append(classContent);
            bufferedWriter.close();
        } catch (IOException e) {
            System.out.println("Error in writing of the generated class to file [" + fileName + "]." + e.getMessage());
        }
        return signatureOfMethod;
    }

    private static void generateBoilerplaceCode() {
        emitPackagePrologue();
        emitClassBegin();
        emitSizesOfParameters();
        emitMethod(methodBody);
        emitMainMethod();
        emitDeclarationOfParameters();
        emitInitializationOfParameters();
        emitTaskSchedule();
        emitClosingBraceForMainMethod();
        emitClosingBraceForClass();
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

        String filePath = METHOD_PATH_FOR_SERVICE;

        initializeConfigurations(filePath);

        generateBoilerplaceCode();

        writeClassToFile(SERVICE_DESTINATION_DIR + className + SUFFIX);
    }
}
