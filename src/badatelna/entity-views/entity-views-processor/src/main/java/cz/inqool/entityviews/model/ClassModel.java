package cz.inqool.entityviews.model;

import cz.inqool.entityviews.AbstractView;
import cz.inqool.entityviews.View;
import cz.inqool.entityviews.ViewContext;
import cz.inqool.entityviews.function.Printable;
import cz.inqool.entityviews.function.Viewable;
import cz.inqool.entityviews.model.type.TypeModel;
import cz.inqool.entityviews.model.type.RealTypeModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Embeddable;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.inqool.entityviews.context.ContextHolder.*;
import static cz.inqool.entityviews.context.ContextHolder.inView;
import static cz.inqool.entityviews.function.Printable.*;
import static java.util.Collections.emptyMap;

@AllArgsConstructor
@Getter
@Setter
public class ClassModel implements Viewable {

    private RealTypeModel type;
    private TypeModel superClass;
    private ImplementModel[] implementModels;
    private boolean isAbstract;
    private String[] views;
    private boolean generateRef;
    private Map<String, ViewContext> viewMappings;

    private AnnotationModel[] annotations;
    private FieldModel[] fields;
    private MethodModel[] methods;

    private static final RealTypeModel embeddableType = new RealTypeModel(Embeddable.class.getPackageName(), Embeddable.class.getSimpleName(), new TypeModel[]{});
    private static final AnnotationModel embeddableAnnotation = new AnnotationModel(embeddableType, emptyMap(), null);

    public void printClass() {
        printAnnotations();

        println(() -> {
            print("public ");
            if (isAbstract) {
                print("abstract ");
            }

            print("class ", type.getDefinition());

            if (superClass != null) {
                printSuperClass();
            }

            printViewInterface();
            printInterfaces();


            print(" {");
        });

        intended(() -> {
            printFields();
            printMethods();

            printToEntityAbstractMethod();
            if (!isAbstract) {
                printToEntityMethod();
            }
            printToEntitiesMethod();
            printToViewAbstractMethod();
            if (!isAbstract) {
                printToViewMethod();
            }
            printToViewsMethod();
        });

        println("}");
    }

    public void printRef() {
        String refClassName = type.getRefName();

        println(embeddableAnnotation.toString());
        println("public class ", refClassName, "{");

        intended(() -> {
            printRefIdField();
            printRefToEntityMethod();
            printRefToEntitiesMethod();
            printToRefMethod();
            printToRefsMethod();
        });

        println("}");
    }

    private void printAnnotations() {
        Arrays.
                stream(getAnnotations()).
                filter(Viewable::includeInView).
                map(AnnotationModel::toString).
                forEach(Printable::println);
    }

    private void printViewInterface() {
        print(" implements ");
        if (isAbstract) {
            print(AbstractView.class.getCanonicalName());
        } else {
            print(View.class.getCanonicalName());
        }
    }

    private void printSuperClass() {
        inView(getMappedView(), () -> {
            if (!superClass.isUsedView()) {
                System.err.println("Class " + type.getClassName() + " should extend view instead of real entity.");
            }

            print(" extends ", superClass.getUsage());
        });
    }

    private void printInterfaces() {
        if (implementModels.length > 0) {
            String str = Arrays.
                    stream(implementModels).
                    filter(Viewable::includeInView).
                    map(ImplementModel::getImplementedInterface).
                    map(TypeModel::getUsage).
                    collect(Collectors.joining(", "));

            if (str.length() > 0) {
                print(", ", str);
            }
        }

    }

    private void printFields() {
        for(FieldModel field : fields) {
            field.printDefinition();
        }

        if (fields.length > 0) {
            println();
        }
    }

    private void printMethods() {
        for(MethodModel method : methods) {
            method.printMethod();
        }
    }

    private void printToEntityAbstractMethod() {
        String parametrizedClassName = type.getUsageOriginal();
        String parametrizedViewClassName = type.getUsage();

        println(() -> {
            print("public static ");

            if (type.hasArguments()) {
                print("<", type.getArgumentsDefinition(), "> ");
            }

            print("void toEntity(", parametrizedClassName, " entity, ", parametrizedViewClassName, " view) {");
        });

        intended(() -> {
            println("if (view == null) {");
            intended(() -> {
                println("return;");
            });
            println("}");

            if (superClass != null) {
                inView(getMappedView(), () -> {
                    if (!superClass.isUsedView()) {
                        return;
                    }

                    println(superClass.getFullViewName(), ".toEntity(entity, view);");
                });
            }

            printToEntityFieldsAssignment();
        });

        println("}");
        println();
    }

    private void printToEntityMethod() {
        String parametrizedClassName = type.getUsageOriginal();
        String parametrizedViewClassName = type.getUsage();

        println(() -> {
            print("public static ");

            if (type.hasArguments()) {
                print("<", type.getArgumentsDefinition(), "> ");
            }

            print(parametrizedClassName, " toEntity(", parametrizedViewClassName, " view) {");
        });

        intended(() -> {
            println("if (view == null) {");
            intended(() -> {
                println("return null;");
            });
            println("}");

            println(parametrizedClassName, " entity = new ", parametrizedClassName, "();");
            println("toEntity(entity, view);");

            println("return entity;");
        });

        println("}");
        println();
    }

    private void printToEntitiesMethod() {
        if (isAbstract) {
            return;
        }

        String parametrizedClassName = type.getUsageOriginal();
        String parametrizedViewClassName = type.getUsage();

        println(() -> {
            print("public static <");

            if (type.hasArguments()) {
                print(type.getArgumentsDefinition(), ", ");
            }

            print("EVCollection extends java.util.Collection<", parametrizedClassName, ">>");
            print("EVCollection toEntities(java.util.Collection<", parametrizedViewClassName, "> views, java.util.function.Supplier<EVCollection> supplier) {");
        });

        intended(() -> {
            println("if (views == null) {");
            intended(() -> {
                println("return null;");
            });

            println("}");
            println();

            println("return views.stream().map(view -> toEntity(view)).collect(java.util.stream.Collectors.toCollection(supplier));");
        });

        println("}");
        println();
    }

    private void printRefIdField() {
        println("public String id;");
        println();
    }

    private void printRefToEntityMethod() {
        String refClassName = type.getRefName();
        String parametrizedClassName = type.getUsageOriginal();

        println(() -> {
            print("public static ");

            if (type.hasArguments()) {
                print("<", type.getArgumentsDefinition(), "> ");
            }

            print(parametrizedClassName, " toEntity(", refClassName, " ref", ") {");
        });

        intended(() -> {
            println("if (ref == null) {");
            intended(() -> {
                println("return null;");
            });
            println("}");
            println();
            println(parametrizedClassName, " entity = new ", parametrizedClassName, "();");
            println("entity.id = ref.id;");
            println("return entity;");
        });

        println("}");
        println();
    }

    private void printRefToEntitiesMethod() {
        if (isAbstract) {
            return;
        }

        String parametrizedClassName = type.getUsageOriginal();
        String refClassName = type.getRefName();

        println(() -> {
            print("public static <");

            if (type.hasArguments()) {
                print(type.getArgumentsDefinition(), ", ");
            }

            print("EVCollection extends java.util.Collection<", parametrizedClassName, ">>");
            print("EVCollection toEntities(java.util.Collection<", refClassName, "> refs, java.util.function.Supplier<EVCollection> supplier) {");
        });

        intended(() -> {
            println("if (refs == null) {");
            intended(() -> {
                println("return null;");
            });

            println("}");
            println();

            println(() -> {
                print("return refs.stream().map(", refClassName, "::");

                if (type.hasArguments()) {
                    print("<", type.getArgumentsUsage(), ">");
                }

                print("toEntity).collect(java.util.stream.Collectors.toCollection(supplier));");
            });
        });

        println("}");
        println();
    }

    private void printToRefMethod() {
        String refClassName = type.getRefName();
        String parametrizedClassName = type.getUsageOriginal();

        println(() -> {
            print("public static ");

            if (type.hasArguments()) {
                print("<", type.getArgumentsDefinition(), "> ");
            }

            print(refClassName, " toRef(", parametrizedClassName, " entity) {");
        });

        intended(() -> {
            println("if (entity == null) {");
            intended(() -> {
                println("return null;");
            });
            println("}");
            println();

            println(refClassName, " ref = new ", refClassName, "();");
            println("ref.id = entity.id;");
            println("return ref;");
        });

        println("}");
        println();
    }

    private void printToRefsMethod() {
        if (isAbstract) {
            return;
        }

        String parametrizedClassName = type.getUsageOriginal();
        String refClassName = type.getRefName();

        println(() -> {
            print("public static ");

            print("<");

            if (type.hasArguments()) {
                print(type.getArgumentsDefinition(), ", ");
            }

            print("EVCollection extends java.util.Collection<", refClassName, ">>");

            print("EVCollection toRefs(java.util.Collection<", parametrizedClassName, "> entities, java.util.function.Supplier<EVCollection> supplier) {");
        });

        intended(() -> {
            println("if (entities == null) {");
            intended(() -> {
                println("return null;");
            });

            println("}");
            println();

            println(() -> {
                print("return entities.stream().map(", refClassName, "::");

                if (type.hasArguments()) {
                    print("<", type.getArgumentsUsage(), ">");
                }

                print("toRef).collect(java.util.stream.Collectors.toCollection(supplier));");
            });

        });

        println("}");
        println();
    }

    private void printToViewAbstractMethod() {
        String parametrizedClassName = type.getUsageOriginal();
        String parametrizedViewClassName = type.getUsage();

        println(() -> {
            print("public static ");

            if (type.hasArguments()) {
                print("<", type.getArgumentsDefinition(), "> ");
            }

            print("void toView(", parametrizedViewClassName, " view, ", parametrizedClassName, " entity) {");
        });

        intended(() -> {
            println("if (entity == null) {");
            intended(() -> {
                println("return;");
            });

            println("}");

            if (superClass != null) {
                inView(getMappedView(), () -> {
                    if (!superClass.isUsedView()) {
                        return;
                    }

                    println(superClass.getFullViewName(), ".toView(view, entity);");
                });
            }

            printToViewFieldsAssignment();
        });

        println("}");
        println();
    }

    private void printToViewMethod() {
        String parametrizedClassName = type.getUsageOriginal();
        String parametrizedViewClassName = type.getUsage();

        println(() -> {
            print("public static ");

            if (type.hasArguments()) {
                print("<", type.getArgumentsDefinition(), "> ");
            }

            print(parametrizedViewClassName, " toView(", parametrizedClassName, " entity) {");
        });

        intended(() -> {
            println("if (entity == null) {");
            intended(() -> {
                println("return null;");
            });

            println("}");

            println(parametrizedViewClassName, " view = new ", parametrizedViewClassName, "();");
            println("toView(view, entity);");

            println("return view;");
        });

        println("}");
        println();
    }

    private void printToViewsMethod() {
        if (isAbstract) {
            return;
        }

        String parametrizedClassName = type.getUsageOriginal();
        String parametrizedViewClassName = type.getUsage();

        println(() -> {
            print("public static ");

            print("<");

            if (type.hasArguments()) {
                print(type.getArgumentsDefinition(), ", ");
            }

            print("EVCollection extends java.util.Collection<", parametrizedViewClassName, ">>");

            print("EVCollection toViews(java.util.Collection<", parametrizedClassName, "> entities, java.util.function.Supplier<EVCollection> supplier) {");
        });

        intended(() -> {
            println("if (entities == null) {");
            intended(() -> {
                println("return null;");
            });

            println("}");
            println();

            println("return entities.stream().map(entity -> toView(entity)).collect(java.util.stream.Collectors.toCollection(supplier));");
        });

        println("}");
        println();
    }

    private void printToEntityFieldsAssignment() {
        for(FieldModel field : fields) {
            field.printToEntityAssignment();
        }

        if (fields.length > 0) {
            println();
        }
    }

    private void printToViewFieldsAssignment() {
        for(FieldModel field : fields) {
            field.printToViewAssignment();
        }

        if (fields.length > 0) {
            println();
        }
    }
}
