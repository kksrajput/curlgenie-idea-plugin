package com.curlgenie;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;

import java.util.*;
import java.util.stream.Collectors;

public class CurlGenerator {

    public String generateCurl(String methodCode, String classesCode) {
        JavaParser parser = new JavaParser();
        MethodDeclaration method = parseMethod(parser, methodCode);
        CompilationUnit cu = parser.parse(classesCode).getResult()
                .orElseThrow(() -> new RuntimeException("Invalid class definitions"));

        Map<String, Integer> nameCount = new HashMap<>();
        Map<String, String> queryParams = new LinkedHashMap<>();
        Map<String, String> pathParams = new LinkedHashMap<>();
        Map<String, String> headers = new LinkedHashMap<>();
        String body = "";
        String contentType = "application/json";
        Set<String> visited = new HashSet<>();

        String url = extractUrl(method);
        String httpMethod = extractHttpMethod(method);

        boolean isXml = method.getAnnotations().stream()
                .filter(a -> a.getNameAsString().matches("PostMapping|PutMapping|RequestMapping"))
                .filter(AnnotationExpr::isNormalAnnotationExpr)
                .map(AnnotationExpr::asNormalAnnotationExpr)
                .flatMap(a -> a.getPairs().stream())
                .anyMatch(p -> p.getNameAsString().equals("consumes") && p.getValue().toString().contains("xml"));
        if (isXml) {
            contentType = "application/xml";
        }

        for (Parameter param : method.getParameters()) {
            String type = param.getType().asString();
            String rawName = param.getNameAsString();
            String name = rawName;
            nameCount.put(name, nameCount.getOrDefault(name, 0) + 1);
            if (nameCount.get(name) > 1) {
                name = name + nameCount.get(name);
            }

            if (param.isAnnotationPresent("PathVariable")) {
                pathParams.put(name, "val");
            } else if (param.isAnnotationPresent("RequestParam")) {
                queryParams.put(name, "val");
            } else if (param.isAnnotationPresent("RequestHeader")) {
                String headerName = param.getAnnotationByName("RequestHeader")
                        .flatMap(a -> {
                            if (a.isSingleMemberAnnotationExpr()) {
                                return Optional.of(((SingleMemberAnnotationExpr) a).getMemberValue().toString().replaceAll("^\"|\"$", ""));
                            } else if (a.isNormalAnnotationExpr()) {
                                return a.asNormalAnnotationExpr().getPairs().stream()
                                        .filter(p -> p.getNameAsString().equals("value") || p.getNameAsString().equals("name"))
                                        .map(p -> p.getValue().toString().replaceAll("^\"|\"$", ""))
                                        .findFirst();
                            }
                            return Optional.empty();
                        })
                        .orElse(name);
                headers.put(headerName, "val");
            } else if (param.isAnnotationPresent("ModelAttribute")) {
                List<FieldDeclaration> fields = extractFieldsFromClass(type, cu);
                for (FieldDeclaration f : fields) {
                    for (VariableDeclarator v : f.getVariables()) {
                        queryParams.put(v.getNameAsString(), "val");
                    }
                }
            } else {
                if (type.contains("Map")) {
                    body = "{\"key\":\"value\"}";
                } else {
                    body = isXml ? genXml(type, cu, visited) : genJson(type, cu, visited);
                }
            }
        }

        for (var entry : pathParams.entrySet()) {
            url = url.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        if (!queryParams.isEmpty()) {
            url += "?" + queryParams.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
        }

        StringBuilder curl = new StringBuilder("curl -X ")
                .append(httpMethod)
                .append(" \\\n\"http://localhost:8080")
                .append(url)
                .append("\"");

        for (var h : headers.entrySet()) {
            curl.append(" \\\n  -H \"").append(h.getKey()).append(": ").append(h.getValue()).append("\"");
        }

        if (!body.isEmpty()) {
            curl.append(" \\\n  -H \"Content-Type: ").append(contentType).append("\"");
            curl.append(" \\\n  -d '").append(body).append("'");
        }

        return curl.toString();
    }

    private MethodDeclaration parseMethod(JavaParser parser, String code) {
        String wrapper = "class Temp { " + code + " }";
        ParseResult<CompilationUnit> result = parser.parse(wrapper);
        CompilationUnit tmp = result.getResult().orElseThrow(() -> new RuntimeException("Invalid method definition"));
        return tmp.findFirst(MethodDeclaration.class).orElseThrow(() -> new RuntimeException("No method found"));
    }

    private String extractHttpMethod(MethodDeclaration m) {
        if (m.isAnnotationPresent("PostMapping")) return "POST";
        if (m.isAnnotationPresent("GetMapping")) return "GET";
        if (m.isAnnotationPresent("PutMapping")) return "PUT";
        if (m.isAnnotationPresent("DeleteMapping")) return "DELETE";
        return m.getAnnotationByName("RequestMapping")
                .filter(AnnotationExpr::isNormalAnnotationExpr)
                .map(AnnotationExpr::asNormalAnnotationExpr)
                .flatMap(a -> a.getPairs().stream()
                        .filter(p -> p.getNameAsString().equals("method"))
                        .map(p -> p.getValue().toString().replace("RequestMethod.", ""))
                        .findFirst())
                .orElse("GET");
    }

    private String extractUrl(MethodDeclaration m) {
        for (String ann : List.of("GetMapping", "PostMapping", "PutMapping", "DeleteMapping")) {
            if (m.isAnnotationPresent(ann)) {
                return m.getAnnotationByName(ann).get()
                        .toString().replaceAll(".*\\(\"([^\"]+)\".*", "$1");
            }
        }
        return m.getAnnotationByName("RequestMapping")
                .filter(AnnotationExpr::isNormalAnnotationExpr)
                .map(AnnotationExpr::asNormalAnnotationExpr)
                .flatMap(expr -> expr.getPairs().stream()
                        .filter(p -> p.getNameAsString().matches("value|path"))
                        .map(p -> p.getValue().toString().replaceAll("^\"|\"$", ""))
                        .findFirst())
                .orElse("/");
    }

    private String genJson(String cls, CompilationUnit cu, Set<String> visited) {
        if (visited.contains(cls)) return "\"...\"";
        visited.add(cls);
        Optional<ClassOrInterfaceDeclaration> opt = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(c -> c.getNameAsString().equals(cls))
                .findFirst();
        if (opt.isEmpty()) return "{}";
        ClassOrInterfaceDeclaration cd = opt.get();
        List<FieldDeclaration> fields = collectFields(cd, cu);

        StringBuilder sb = new StringBuilder("{");
        for (FieldDeclaration f : fields) {
            for (VariableDeclarator v : f.getVariables()) {
                String n = v.getNameAsString();
                String t = v.getType().asString();
                sb.append("\"").append(n).append("\":");
                if (isPrimitive(t)) {
                    sb.append(primitiveVal(t));
                } else {
                    sb.append(genJson(t, cu, visited));
                }
                sb.append(",");
            }
        }
        if (sb.charAt(sb.length() - 1) == ',') sb.setLength(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    private String genXml(String cls, CompilationUnit cu, Set<String> visited) {
        if (visited.contains(cls)) return "<!--...-->";
        visited.add(cls);
        Optional<ClassOrInterfaceDeclaration> opt = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(c -> c.getNameAsString().equals(cls))
                .findFirst();
        if (opt.isEmpty()) return "<" + cls + "/>";
        ClassOrInterfaceDeclaration cd = opt.get();
        List<FieldDeclaration> fields = collectFields(cd, cu);

        StringBuilder sb = new StringBuilder("<").append(cls).append(">");
        for (FieldDeclaration f : fields) {
            for (VariableDeclarator v : f.getVariables()) {
                String n = v.getNameAsString();
                String t = v.getType().asString();
                sb.append("<").append(n).append(">");
                if (isPrimitive(t)) {
                    sb.append(primitiveVal(t).replace("\"", ""));
                } else {
                    sb.append(genXml(t, cu, visited));
                }
                sb.append("</").append(n).append(">");
            }
        }
        sb.append("</").append(cls).append(">");
        return sb.toString();
    }

    private List<FieldDeclaration> extractFieldsFromClass(String type, CompilationUnit cu) {
        return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(c -> c.getNameAsString().equals(type))
                .flatMap(c -> collectFields(c, cu).stream())
                .collect(Collectors.toList());
    }

    private List<FieldDeclaration> collectFields(ClassOrInterfaceDeclaration cd, CompilationUnit cu) {
        List<FieldDeclaration> all = new ArrayList<>();

        if (cd.isInterface()) {
            all.addAll(synthesizeFieldsFromInterfaceMethods(cd));
        } else {
            all.addAll(cd.getFields());
        }

        cd.getExtendedTypes().forEach(e ->
                cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                        .filter(c -> c.getNameAsString().equals(e.getNameAsString()))
                        .findFirst().ifPresent(parent -> all.addAll(collectFields(parent, cu)))
        );

        cd.getImplementedTypes().forEach(e ->
                cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                        .filter(c -> c.getNameAsString().equals(e.getNameAsString()))
                        .findFirst().ifPresent(parent -> {
                            all.addAll(collectFields(parent, cu));
                            if (parent.isInterface()) {
                                all.addAll(synthesizeFieldsFromInterfaceMethods(parent));
                            }
                        })
        );

        return all;
    }

    private List<FieldDeclaration> synthesizeFieldsFromInterfaceMethods(ClassOrInterfaceDeclaration iface) {
        List<FieldDeclaration> fields = new ArrayList<>();
        for (MethodDeclaration m : iface.getMethods()) {
            if (m.getParameters().isEmpty() && m.getNameAsString().startsWith("get")) {
                String name = Character.toLowerCase(m.getNameAsString().charAt(3)) + m.getNameAsString().substring(4);
                String type = m.getType().asString();
                Type parsedType = new JavaParser().parseType(type).getResult()
                        .orElseThrow(() -> new RuntimeException("Invalid type: " + type));
                VariableDeclarator var = new VariableDeclarator(parsedType, name);
                FieldDeclaration synthetic = new FieldDeclaration();
                synthetic.addVariable(var);
                fields.add(synthetic);
            }
        }
        return fields;
    }

    private boolean isPrimitive(String t) {
        return t.matches("int|Integer|long|Long|float|Float|double|Double|boolean|Boolean|String");
    }

    private String primitiveVal(String t) {
        return t.equals("String") ? "\"example\"" : (t.contains("Boolean") ? "true" : "1");
    }
}
